package pt.ipt.dama.TeachersLog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.data.local.AppDatabase
import pt.ipt.dama.TeachersLog.data.model.*
import pt.ipt.dama.TeachersLog.data.remote.NetworkClient
import pt.ipt.dama.TeachersLog.data.repository.CloningRepository
import pt.ipt.dama.TeachersLog.data.repository.SyncRepository
import pt.ipt.dama.TeachersLog.data.repository.TeacherRepository
import pt.ipt.dama.TeachersLog.utils.SessionManager
import androidx.core.net.toUri
import androidx.work.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ipt.dama.TeachersLog.data.workers.ImageUploadWorker
import pt.ipt.dama.TeachersLog.data.workers.DataSyncWorker
import pt.ipt.dama.TeachersLog.data.workers.ImageDeleteWorker

/**
 * TeacherViewModel manages student-specific data including classes, students, and grading.
 *
 * It provides methods for student CRUD operations and manages the state of qualitative
 * and quantitative evaluation components.
 *
 * @param application The application context, required for database initialization.
 */
class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repository as the single source of truth for student/class data
    private val repository: TeacherRepository
    private val cloningRepository: CloningRepository
    private val syncRepository: SyncRepository
    private val sessionManager: SessionManager
    
    init {
        val database = AppDatabase.getDatabase(application)
        val teacherDao = database.teacherDao()
        repository = TeacherRepository(teacherDao)
        cloningRepository = CloningRepository(database, teacherDao)
        sessionManager = SessionManager(application)
        syncRepository = SyncRepository(database, teacherDao, NetworkClient.apiService, sessionManager)
    }

    // --- Student Class CRUD Methods ---

    /**
     * Retrieves all grading components (criteria) for a specific class.
     * @param classId The ID of the student class.
     * @return A [LiveData] stream containing the list of components.
     */
    fun getGradingComponentsForClass(classId: Int): LiveData<List<GradingComponent>> {
        return repository.getGradingComponentsForClass(classId).asLiveData()
    }

    /**
     * Atomic operation to save or update a class along with its hierarchical grading components.
     * 
     * @param classId The ID of the class (null for creation).
     * @param subjectId The parent subject ID.
     * @param name The class name (e.g., "10-A").
     * @param gradingScale The scale identifier (e.g., "5", "20").
     * @param components Flattened list of components (groups and sub-items).
     * @param parentMap Mapping from component index to its parent's index in the list.
     */
    fun upsertClassWithHierarchy(
        classId: Int?,
        subjectId: Int,
        name: String,
        gradingScale: String,
        components: List<GradingComponent>,
        parentMap: Map<Int, Int>
    ) {
        viewModelScope.launch {
            // Step 1: Persist the class metadata
            val cid = if (classId == null) {
                repository.insertStudentsClass(StudentsClass(
                    subjectId = subjectId,
                    name = name,
                    gradingScale = gradingScale
                )).toInt()
            } else {
                repository.updateStudentsClass(StudentsClass(
                    id = classId,
                    subjectId = subjectId,
                    name = name,
                    gradingScale = gradingScale
                ))
                // Step 2: Clear existing components to prevent orphans in hierarchy changes
                repository.getGradingComponentsForClass(classId).first().forEach {
                    repository.deleteGradingComponent(it)
                }
                classId
            }

            // Step 3: Sequential insertion to obtain IDs for children
            // We use a mapping from UI Index -> New Database ID
            val insertedIds = mutableMapOf<Int, Int>() 
            components.forEachIndexed { index, comp ->
                val parentDbId = parentMap[index]?.let { insertedIds[it] }
                val newId = repository.insertGradingComponent(comp.copy(
                    id = 0, // Force new ID generation
                    classId = cid,
                    parentId = parentDbId
                )).toInt()
                insertedIds[index] = newId
            }
            triggerSync()
        }
    }

    /**
     * Deletes a class and its associated students/grades.
     * @param studentsClass The [StudentsClass] object to delete.
     */
    fun deleteStudentsClass(studentsClass: StudentsClass) {
        viewModelScope.launch {
            // 1. Clean up student images first
            repository.getStudentsByClassIdSync(studentsClass.id).forEach { student ->
                student.profileImageUrl?.let { url ->
                    if (url.startsWith("http")) {
                        try { 
                            val response = NetworkClient.apiService.deleteImage(url)
                            if (!response.isSuccessful) scheduleImageDeletion(url)
                        } catch (_: Exception) {
                            scheduleImageDeletion(url)
                        }
                    }
                }
            }

            // 2. Delete data
            repository.deleteStudentsClass(studentsClass)
            triggerSync()
        }
    }

    // --- Data Retrieval Methods ---

    /**
     * Fetches all classes associated with a specific subject.
     * @param subjectId The ID of the parent subject.
     * @return A [LiveData] list of [StudentsClassWithStats].
     */
    fun getStudentsClassesForSubject(subjectId: Int): LiveData<List<StudentsClassWithStats>> {
        return repository.getStudentsClassesForSubject(subjectId).asLiveData()
    }

    /**
     * Retrieves a specific class by its ID.
     * @param classId The unique identifier of the class.
     * @return A [LiveData] stream containing the class or null.
     */
    fun getStudentsClassById(classId: Int): LiveData<StudentsClass?> {
        return repository.getStudentsClassById(classId).asLiveData()
    }

    /**
     * Retrieves a specific class with its statistics and parent subject icon.
     * @param classId The unique identifier of the class.
     * @return A [LiveData] stream containing the class with stats or null.
     */
    fun getStudentsClassWithStatsById(classId: Int): LiveData<StudentsClassWithStats?> {
        return repository.getStudentsClassWithStatsById(classId).asLiveData()
    }

    /**
     * Fetches all students and their calculated grades for a specific class.
     * @param studentsClassId The ID of the class.
     * @return A [LiveData] list of [StudentWithGrade].
     */
    fun getStudentsWithGradesForStudentsClass(studentsClassId: Int): LiveData<List<StudentWithGrade>> {
        return repository.getStudentsWithGradesForStudentsClass(studentsClassId).asLiveData()
    }

    /**
     * Retrieves a single student's basic information.
     * @param studentId The ID of the student.
     * @return The [Student] object or null if not found.
     */
    suspend fun getStudentById(studentId: Int): Student? {
        return repository.getStudentById(studentId)
    }

    /**
     * Retrieves the grading scale identifier for a specific class.
     * @param classId The ID of the class.
     * @return The scale identifier string (e.g., "5", "20").
     */
    suspend fun getGradingScaleForClass(classId: Int): String {
        return repository.getGradingScaleByClassId(classId) ?: "5"
    }

    /**
     * Retrieves the grading scale identifier for a specific student's class.
     * @param studentId The student ID.
     * @return The scale identifier string.
     */
    suspend fun getGradingScaleForStudent(studentId: Int): String {
        return repository.getGradingScaleByStudentId(studentId) ?: "5"
    }

    /**
     * Checks if a process number is already taken globally.
     * @param processNumber The process string to verify.
     * @param excludeId ID of a student to ignore (useful when editing).
     * @return True if taken, false otherwise.
     */
    suspend fun isProcessNumberTaken(processNumber: String, excludeId: Int = -1): Boolean {
        return repository.existsProcessNumber(processNumber, excludeId)
    }

    // --- Student CRUD Methods ---

    /**
     * Adds a new student and triggers synchronization.
     * If a local photo URI is provided, it attempts to upload it to the cloud first.
     * @param name Student's full name.
     * @param number Roster number.
     * @param process Administrative process string.
     * @param photoUrl Local URI or cloud photo path.
     * @param classId Target class ID.
     */
    fun addStudent(name: String, number: Int, process: String?, photoUrl: String?, classId: Int) {
        viewModelScope.launch {
            var finalPhotoUrl = photoUrl
            
            // If it's a local URI (starts with content:// or file://), upload it
            if (photoUrl != null && (photoUrl.startsWith("content://") || photoUrl.startsWith("file://"))) {
                val cloudUrl = uploadImageFromUri(photoUrl)
                if (cloudUrl != null) {
                    finalPhotoUrl = cloudUrl
                }
            }

            val newStudentId = repository.insertStudent(Student(
                studentsClassId = classId,
                numberInGroup = number,
                name = name,
                processNumber = process,
                profileImageUrl = finalPhotoUrl
            )).toInt()

            // If upload failed, and it was a local URI, schedule background sync with the new ID
            if (finalPhotoUrl != null && (finalPhotoUrl.startsWith("content://") || finalPhotoUrl.startsWith("file://"))) {
                scheduleImageUpload(finalPhotoUrl, newStudentId)
            }

            triggerSync()
        }
    }

    /**
     * Updates an existing student's record and triggers synchronization.
     * Handles cloud upload if a new local photo is provided.
     * @param student The updated [Student] object.
     */
    fun updateStudent(student: Student) {
        viewModelScope.launch {
            var finalStudent = student
            
            // If it's a local URI, upload it before saving
            if (student.profileImageUrl != null && 
                (student.profileImageUrl.startsWith("content://") || student.profileImageUrl.startsWith("file://"))) {
                
                // Delete old image if it exists and is a cloud URL
                val oldStudent = repository.getStudentById(student.studentId)
                val oldUrl = oldStudent?.profileImageUrl
                android.util.Log.d("TeacherViewModel", "Old student photo URL: $oldUrl")
                
                if (oldUrl != null && oldUrl.startsWith("http")) {
                    android.util.Log.d("TeacherViewModel", "Deleting old student cloud photo: $oldUrl")
                    try { 
                        val response = NetworkClient.apiService.deleteImage(oldUrl)
                        if (!response.isSuccessful) {
                            scheduleImageDeletion(oldUrl)
                        }
                    } catch (_: Exception) {
                        scheduleImageDeletion(oldUrl)
                    }
                }

                val cloudUrl = uploadImageFromUri(student.profileImageUrl)
                if (cloudUrl != null) {
                    finalStudent = student.copy(profileImageUrl = cloudUrl)
                } else {
                    // Upload failed, schedule for later
                    scheduleImageUpload(student.profileImageUrl, student.studentId)
                }
            }

            repository.updateStudent(finalStudent)
            triggerSync()
        }
    }

    /**
     * Schedules a background task to upload an image when internet is available.
     */
    private fun scheduleImageUpload(uri: String, studentId: Int, isProfile: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("image_uri", uri)
            .putInt("student_id", studentId)
            .putBoolean("is_profile", isProfile)
            .build()

        val uploadRequest = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "upload_${if (isProfile) "profile" else studentId}",
            ExistingWorkPolicy.REPLACE,
            uploadRequest
        )
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
     * Helper to handle the image upload process from a URI.
     */
    private suspend fun uploadImageFromUri(uriString: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val context = getApplication<Application>()
            val uri = uriString.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() }

            if (bytes != null) {
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(mediaType)

                val response = NetworkClient.apiService.uploadImage(requestBody)
                if (response.isSuccessful) response.body()?.imageUrl else null
            } else null
        } catch (e: Exception) {
            android.util.Log.e("TeacherViewModel", "Error uploading student image", e)
            null
        }
    }

    /**
     * Deletes a student and their associated grades/scores.
     * @param student The [Student] object to delete.
     */
    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            // 1. Clean up image first
            student.profileImageUrl?.let { url ->
                if (url.startsWith("http")) {
                    try { 
                        val response = NetworkClient.apiService.deleteImage(url)
                        if (!response.isSuccessful) scheduleImageDeletion(url)
                    } catch (_: Exception) {
                        scheduleImageDeletion(url)
                    }
                }
            }
            
            // 2. Delete data
            repository.deleteStudent(student)
            triggerSync()
        }
    }

    // --- Dashboard Specific Logic ---

    /**
     * Observable list of the 3 most recently accessed classes for a specific academic period.
     * @param semesterId The ID of the semester/period.
     * @return A [LiveData] stream of [StudentsClassWithStats].
     */
    fun getRecentClassesForPeriod(semesterId: Int): LiveData<List<StudentsClassWithStats>> {
        return repository.getRecentClassesForPeriod(semesterId).asLiveData()
    }

    /**
     * Updates the last accessed timestamp for a class.
     * @param classId The ID of the class.
     */
    fun updateClassLastAccessed(classId: Int) {
        viewModelScope.launch {
            repository.updateClassLastAccessed(classId)
        }
    }

    // --- Grading Management ---

    private val _currentGrade = MutableLiveData<Grade?>()
    /** The qualitative grade data (notes) for the student currently being graded. */
    val currentGrade: LiveData<Grade?> = _currentGrade

    /**
     * Loads the grade data for a specific student into [currentGrade].
     * @param studentId The ID of the student.
     */
    fun loadGradeForStudent(studentId: Int) {
        viewModelScope.launch {
            _currentGrade.value = repository.getGradeForStudent(studentId)
        }
    }

    /**
     * Retrieves the list of dynamic component scores for a student.
     * @param studentId The ID of the student.
     * @return A [LiveData] stream of [ComponentScore] objects.
     */
    fun getComponentScoresForStudent(studentId: Int): LiveData<List<ComponentScore>> {
        return repository.getComponentScoresForStudent(studentId).asLiveData()
    }

    /**
     * Saves or updates a student's grades and scores across all components.
     * 
     * @param studentId The student receiving the grades.
     * @param scores The list of [StudentScore] for each grading component.
     * @param teacherNotes Qualitative notes for the student.
     */
    fun saveGrade(studentId: Int, scores: List<StudentScore>, teacherNotes: String) {
        viewModelScope.launch {
            // Persist qualitative feedback
            repository.insertGrade(Grade(studentId = studentId, teacherNotes = teacherNotes))
            // Persist individual component scores
            scores.forEach { score ->
                repository.insertStudentScore(score)
            }
            triggerSync()
        }
    }

    // --- Cloning Operations ---

    /**
     * Deep clones a class to another subject.
     */
    fun cloneClass(sourceId: Int, targetSubjectId: Int) {
        viewModelScope.launch {
            cloningRepository.cloneClass(sourceId, targetSubjectId)
            triggerSync()
        }
    }

    /**
     * Deep clones a student to another class.
     */
    fun cloneStudent(sourceId: Int, targetClassId: Int) {
        viewModelScope.launch {
            cloningRepository.cloneStudent(sourceId, targetClassId)
            triggerSync()
        }
    }

    /**
     * Fetches all classes for a specific subject.
     * Used for cloning destination selection.
     */
    fun getClassesForSubject(subjectId: Int): LiveData<List<StudentsClass>> {
        return repository.getClassesBySubjectId(subjectId).asLiveData()
    }

    /**
     * Triggers a cloud backup if sync is enabled.
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
 * Factory class for [TeacherViewModel].
 */
class TeacherViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
