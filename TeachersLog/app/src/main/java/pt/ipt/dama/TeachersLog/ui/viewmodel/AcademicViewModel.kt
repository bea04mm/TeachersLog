package pt.ipt.dama.TeachersLog.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.data.local.AppDatabase
import pt.ipt.dama.TeachersLog.data.model.*
import pt.ipt.dama.TeachersLog.data.remote.NetworkClient
import pt.ipt.dama.TeachersLog.data.repository.CloningRepository
import pt.ipt.dama.TeachersLog.data.repository.SyncRepository
import pt.ipt.dama.TeachersLog.data.repository.TeacherRepository
import pt.ipt.dama.TeachersLog.utils.SessionManager
import androidx.core.content.edit
import androidx.work.*
import pt.ipt.dama.TeachersLog.data.workers.DataSyncWorker
import pt.ipt.dama.TeachersLog.data.workers.ImageDeleteWorker

/**
 * AcademicViewModel manages the high-level academic structure, including years, 
 * semesters/periods, and subjects, along with global statistics.
 * 
 * It persists the last selected year and period in SharedPreferences to maintain 
 * state across app restarts.
 *
 * @param application The application context, used for locale and persistence initialization.
 */
class AcademicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TeacherRepository
    private val cloningRepository: CloningRepository
    private val syncRepository: SyncRepository
    private val sessionManager: SessionManager
    private val prefs = application.getSharedPreferences("academic_prefs", Context.MODE_PRIVATE)

    /**
     * Observable list of semesters for a specific year.
     * Used for cloning destination selection.
     */
    fun getSemestersForYear(yearId: Int): LiveData<List<Semester>> {
        return repository.getSemestersForYear(yearId).asLiveData()
    }

    /**
     * Observable list of subjects for a specific semester.
     * Used for cloning destination selection.
     */
    fun getSubjectsForSemester(semesterId: Int): LiveData<List<Subject>> {
        return repository.getSubjectsBySemesterId(semesterId).asLiveData()
    }

    /** List of all registered academic years. */
    val allAcademicYears: LiveData<List<AcademicYear>>
    
    private val _selectedYearId = MutableLiveData<Int>()
    /** The ID of the currently selected academic year. */
    val selectedYearId: LiveData<Int> = _selectedYearId

    /** 
     * Observable list of semesters for the selected academic year. 
     * Automatically updates when [selectedYearId] changes.
     */
    val semesters: LiveData<List<Semester>> = _selectedYearId.switchMap { id ->
        repository.getSemestersForYear(id).asLiveData()
    }

    private val _selectedSemesterId = MutableLiveData<Int?>()
    /** The ID of the currently selected semester/period. */
    val selectedSemesterId: LiveData<Int?> = _selectedSemesterId

    /**
     * Observable list of subjects for the selected semester, including calculated statistics.
     * Automatically updates whenever [selectedSemesterId] changes.
     */
    val subjectsWithStats: LiveData<List<SubjectWithStats>> = _selectedSemesterId.switchMap { id ->
        if (id == null) MutableLiveData(emptyList())
        else repository.getSubjectsWithStatsForSemester(id).asLiveData()
    }

    /**
     * Global academic statistics for the selected semester.
     * Automatically updates whenever [selectedSemesterId] changes.
     */
    val globalStats: LiveData<GlobalStats> = _selectedSemesterId.switchMap { id ->
        if (id == null) MutableLiveData(GlobalStats(0, 0, 0.0))
        else repository.getGlobalStatsForSemester(id).asLiveData()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        val teacherDao = database.teacherDao()
        repository = TeacherRepository(teacherDao)
        cloningRepository = CloningRepository(database, teacherDao)
        sessionManager = SessionManager(application)
        syncRepository = SyncRepository(database, teacherDao, NetworkClient.apiService, sessionManager)
        
        allAcademicYears = repository.allAcademicYears.asLiveData()
        
        // Handle initialization and selection persistence
        viewModelScope.launch {
            repository.allAcademicYears.collectLatest { years ->
                if (years.isNotEmpty() && (_selectedYearId.value == null)) {
                    val savedYearId = prefs.getInt("last_year_id", -1)
                    val yearToSelect = if (savedYearId != -1 && years.any { it.id == savedYearId }) {
                        savedYearId
                    } else {
                        years.first().id
                    }
                    selectYear(yearToSelect)
                }
            }
        }

        // Auto-select last or first available semester when the year changes
        semesters.observeForever { list ->
            if (list.isNotEmpty() && _selectedSemesterId.value == null) {
                val savedPeriodId = prefs.getInt("last_period_id", -1)
                val periodToSelect = if (savedPeriodId != -1 && list.any { it.id == savedPeriodId }) {
                    savedPeriodId
                } else {
                    list.first().id
                }
                _selectedSemesterId.value = periodToSelect
            }
        }
    }

    /**
     * Changes the currently active academic year and persists the selection.
     * @param yearId The ID of the year to select.
     */
    fun selectYear(yearId: Int) {
        if (_selectedYearId.value != yearId) {
            _selectedYearId.value = yearId
            _selectedSemesterId.value = null // Reset selection to trigger re-evaluation in observer
            prefs.edit { putInt("last_year_id", yearId) }
        }
    }

    /**
     * Changes the currently active semester and persists the selection.
     * @param semesterId The ID of the semester to select.
     */
    fun selectSemester(semesterId: Int) {
        if (_selectedSemesterId.value != semesterId) {
            _selectedSemesterId.value = semesterId
            prefs.edit { putInt("last_period_id", semesterId) }
        }
    }

    // --- Academic Year CRUD ---

    /**
     * Atomically adds a new academic year along with its first academic period.
     * This ensures no year exists without at least one period.
     * 
     * @param yearName The string representation of the year (e.g., "2024/2025").
     * @param periodName The name of the first period (e.g., "1st Semester").
     */
    fun addYearWithPeriod(yearName: String, periodName: String) {
        viewModelScope.launch {
            val yId = repository.insertAcademicYear(AcademicYear(year = yearName))
            if (yId != -1L) {
                repository.insertSemester(Semester(academicYearId = yId.toInt(), name = periodName))
                selectYear(yId.toInt())
                triggerSync()
            }
        }
    }

    /**
     * Deletes an academic year and all its associated data, including images.
     * @param year The [AcademicYear] object to delete.
     */
    fun deleteAcademicYear(year: AcademicYear) {
        viewModelScope.launch {
            // 1. Clean up images first
            repository.getSemestersForYearList(year.id).forEach { semester ->
                cleanupSemesterImages(semester.id)
            }
            
            // 2. Delete data
            if (_selectedYearId.value == year.id) {
                prefs.edit { remove("last_year_id") }
            }
            repository.deleteAcademicYear(year)
            triggerSync()
        }
    }

    // --- Semester CRUD ---

    /**
     * Adds a new semester to the currently selected academic year.
     * @param name The name of the semester (e.g., "1st Semester").
     */
    fun addSemester(name: String) {
        val yearId = _selectedYearId.value ?: return
        viewModelScope.launch {
            repository.insertSemester(Semester(academicYearId = yearId, name = name))
            triggerSync()
        }
    }

    /**
     * Deletes a semester and all its associated subjects and images.
     * @param semester The [Semester] object to delete.
     */
    fun deleteSemester(semester: Semester) {
        viewModelScope.launch {
            // 1. Clean up images
            cleanupSemesterImages(semester.id)
            
            // 2. Delete data
            if (_selectedSemesterId.value == semester.id) {
                prefs.edit { remove("last_period_id") }
            }
            repository.deleteSemester(semester)
            triggerSync()
        }
    }

    // --- Subject CRUD ---

    /**
     * Adds a new subject to the currently selected semester.
     * @param name The name of the subject.
     * @param iconName The identifier for the UI icon.
     * @param colorHex Theme color hex code.
     */
    fun addSubject(name: String, iconName: String, colorHex: String) {
        val semId = _selectedSemesterId.value ?: return
        viewModelScope.launch {
            repository.insertSubject(Subject(
                semesterId = semId, 
                name = name, 
                iconName = iconName, 
                colorHex = colorHex
            ))
            triggerSync()
        }
    }

    /**
     * Updates an existing subject's information.
     * @param subject The updated [Subject] object.
     */
    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            repository.updateSubject(subject)
            triggerSync()
        }
    }

    /**
     * Deletes a subject and its associated classes/students and images.
     * @param subject The [Subject] object to delete.
     */
    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            // 1. Clean up student images for this subject
            cleanupSubjectImages(subject.id)
            
            // 2. Delete data
            repository.deleteSubject(subject)
            triggerSync()
        }
    }

    /**
     * Helper to delete all student images within a subject.
     */
    private suspend fun cleanupSubjectImages(subjectId: Int) {
        repository.getClassesBySubjectIdSync(subjectId).forEach { clazz ->
            repository.getStudentsByClassIdSync(clazz.id).forEach { student ->
                student.profileImageUrl?.let { url ->
                    if (url.startsWith("http")) {
                        try { 
                            val response = NetworkClient.apiService.deleteImage(url)
                            if (!response.isSuccessful) {
                                scheduleImageDeletion(url)
                            }
                        } catch (_: Exception) {
                            scheduleImageDeletion(url)
                        }
                    }
                }
            }
        }
    }

    /**
     * Schedules a background task to delete an image when internet is available.
     */
    private fun scheduleImageDeletion(url: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("image_url", url)
            .build()

        val deleteRequest = OneTimeWorkRequestBuilder<ImageDeleteWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "delete_${url.hashCode()}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            deleteRequest
        )
    }

    /**
     * Helper to delete all student images within a semester.
     */
    private suspend fun cleanupSemesterImages(semesterId: Int) {
        repository.getSubjectsBySemesterIdSync(semesterId).forEach { subject ->
            cleanupSubjectImages(subject.id)
        }
    }

    /**
     * Retrieves a specific subject by its ID.
     * @param subjectId The unique identifier of the subject.
     * @return A [LiveData] stream containing the subject or null if not found.
     */
    fun getSubjectById(subjectId: Int): LiveData<Subject?> {
        return repository.getSubjectById(subjectId).asLiveData()
    }

    // --- Cloning Operations ---

    /**
     * Deep clones a subject to another semester.
     */
    fun cloneSubject(sourceId: Int, targetSemesterId: Int) {
        viewModelScope.launch {
            cloningRepository.cloneSubject(sourceId, targetSemesterId)
            triggerSync()
        }
    }

    /**
     * Deep clones an entire semester to another academic year.
     */
    fun cloneSemester(sourceId: Int, targetYearId: Int, newName: String? = null) {
        viewModelScope.launch {
            cloningRepository.cloneSemester(sourceId, targetYearId, newName)
            triggerSync()
        }
    }

    /**
     * Deep clones an entire academic year to a new one.
     */
    fun cloneAcademicYear(sourceId: Int, newYearName: String) {
        viewModelScope.launch {
            cloningRepository.cloneAcademicYear(sourceId, newYearName)
            triggerSync()
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    /** Observable state for the pull-to-refresh indicator. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Refreshes data by downloading the latest backup from the cloud for authenticated users.
     * Triggered by Pull-to-Refresh actions in the UI.
     */
    fun refreshData() {
        viewModelScope.launch {
            val authenticated = sessionManager.isAuthenticated.first()
            if (authenticated) {
                _isRefreshing.value = true
                // Before downloading, try to push current local state if possible
                syncRepository.uploadToCloud() 
                syncRepository.downloadFromCloud()
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Schedules a background database sync when internet is available.
     * Called automatically after local database mutations.
     */
    private fun triggerSync() {
        viewModelScope.launch {
            val enabled = sessionManager.isSyncEnabled.first()
            if (enabled) {
                // Try immediate sync first
                syncRepository.uploadToCloud()

                // Schedule background retry/sync to ensure reliability
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "full_data_sync",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            }
        }
    }
}

/**
 * Factory class to satisfy Android's ViewModel initialization requirements.
 * Ensures the [AcademicViewModel] is created with the [Application] context.
 */
class AcademicViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AcademicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AcademicViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
