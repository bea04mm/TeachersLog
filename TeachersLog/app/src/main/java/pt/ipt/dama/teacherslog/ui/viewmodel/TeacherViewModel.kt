package pt.ipt.dama.teacherslog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pt.ipt.dama.teacherslog.data.local.AppDatabase
import pt.ipt.dama.teacherslog.data.model.*
import pt.ipt.dama.teacherslog.data.repository.TeacherRepository
import pt.ipt.dama.teacherslog.utils.LocaleManager

/**
 * TeacherViewModel is the primary ViewModel for the application, responsible for managing
 * academic data (years, subjects, classes, students) and global UI state (theme, language).
 *
 * It acts as a bridge between the [TeacherRepository] and the Compose UI, providing
 * lifecycle-aware data streams using [LiveData].
 *
 * @param application The application context, required for database and locale initialization.
 */
class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repository providing access to the underlying data source
    private val repository: TeacherRepository
    
    // --- Global UI State ---

    // Internal state for dark mode preference
    private val _isDarkMode = MutableLiveData(true)
    /** Observable state for the dark mode theme. */
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    // Internal state for the current application language
    private val _currentLanguage = MutableLiveData(LocaleManager.getLanguage(application))
    /** Observable state for the current language code (e.g., "en", "pt"). */
    val currentLanguage: LiveData<String> = _currentLanguage

    // --- Academic Data State ---

    /** List of all registered academic years. */
    val allAcademicYears: LiveData<List<AcademicYear>>
    
    // Internal state for the currently selected academic year ID
    private val _selectedYearId = MutableLiveData<Int>()
    /** The ID of the academic year currently being viewed/filtered. */
    val selectedYearId: LiveData<Int> = _selectedYearId

    /**
     * Observable list of subjects for the selected year, including calculated statistics.
     * Automatically updates whenever [selectedYearId] changes.
     */
    val subjectsWithStats: LiveData<List<SubjectWithStats>> = _selectedYearId.switchMap { id ->
        repository.getSubjectsWithStatsForYear(id).asLiveData()
    }

    /**
     * Global academic statistics for the selected year.
     * Automatically updates whenever [selectedYearId] changes.
     */
    val globalStats: LiveData<GlobalStats> = _selectedYearId.switchMap { id ->
        repository.getGlobalStatsForYear(id).asLiveData()
    }

    /**
     * All student classes associated with the selected year.
     * Automatically updates whenever [selectedYearId] changes.
     */
    val studentsClassesForYear: LiveData<List<StudentsClassWithStats>> = _selectedYearId.switchMap { id ->
        repository.getAllStudentsClassesForYear(id).asLiveData()
    }

    init {
        // Initialize the database and repository
        val database = AppDatabase.getDatabase(application, viewModelScope)
        val teacherDao = database.teacherDao()
        repository = TeacherRepository(teacherDao)
        
        // Convert the repository's Flow to LiveData for UI consumption
        allAcademicYears = repository.allAcademicYears.asLiveData()
        
        // Automatically select the first available year if none is selected
        viewModelScope.launch {
            repository.allAcademicYears.collectLatest { years ->
                if (years.isNotEmpty() && (_selectedYearId.value == null)) {
                    _selectedYearId.value = years.first().id
                }
            }
        }
    }

    // --- State Management Methods ---

    /**
     * Toggles the application's dark mode setting.
     * @param enabled True to enable dark mode, false for light mode.
     */
    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    /**
     * Updates the application's current language.
     * @param languageCode The new language code (e.g., "en" or "pt").
     */
    fun setLanguage(languageCode: String) {
        if (_currentLanguage.value != languageCode) {
            _currentLanguage.value = languageCode
        }
    }

    /**
     * Changes the currently active academic year.
     * @param yearId The ID of the year to select.
     */
    fun selectYear(yearId: Int) {
        _selectedYearId.value = yearId
    }

    // --- Academic Year CRUD Methods ---

    /**
     * Inserts a new academic year into the database.
     * @param yearName The name of the year (e.g., "2024/2025").
     */
    fun addAcademicYear(yearName: String) {
        viewModelScope.launch {
            repository.insertAcademicYear(AcademicYear(year = yearName))
        }
    }

    /**
     * Deletes an academic year and all its associated data.
     * @param year The [AcademicYear] object to delete.
     */
    fun deleteAcademicYear(year: AcademicYear) {
        viewModelScope.launch {
            repository.deleteAcademicYear(year)
        }
    }

    // --- Subject CRUD Methods ---

    /**
     * Adds a new subject to the currently selected academic year.
     * @param name The name of the subject.
     * @param iconName The name of the icon identifier.
     * @param colorHex The hex color code for the subject's theme.
     * @param gradingScale The grading scale identifier (e.g., "5", "20", "100").
     */
    fun addSubject(name: String, iconName: String, colorHex: String, gradingScale: String = "5") {
        val yearId = _selectedYearId.value ?: return
        viewModelScope.launch {
            repository.insertSubject(Subject(
                academicYearId = yearId, 
                name = name, 
                iconName = iconName, 
                colorHex = colorHex,
                gradingScale = gradingScale
            ))
        }
    }

    /**
     * Updates an existing subject's information.
     * @param subject The updated [Subject] object.
     */
    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    /**
     * Deletes a subject and its associated classes/students.
     * @param subject The [Subject] object to delete.
     */
    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    /**
     * Retrieves a specific subject by its ID.
     * @param subjectId The ID of the subject.
     * @return A [LiveData] stream containing the subject or null if not found.
     */
    fun getSubjectById(subjectId: Int): LiveData<Subject?> {
        return repository.getSubjectById(subjectId).asLiveData()
    }

    // --- Student Class CRUD Methods ---

    /**
     * Adds a new class to a specific subject.
     * @param name The name of the class (e.g., "10-A").
     * @param subjectId The ID of the parent subject.
     */
    fun addStudentsClass(name: String, subjectId: Int) {
        viewModelScope.launch {
            repository.insertStudentsClass(StudentsClass(subjectId = subjectId, name = name))
        }
    }

    /**
     * Updates an existing class's information.
     * @param studentsClass The updated [StudentsClass] object.
     */
    fun updateStudentsClass(studentsClass: StudentsClass) {
        viewModelScope.launch {
            repository.updateStudentsClass(studentsClass)
        }
    }

    /**
     * Deletes a class and its associated students/grades.
     * @param studentsClass The [StudentsClass] object to delete.
     */
    fun deleteStudentsClass(studentsClass: StudentsClass) {
        viewModelScope.launch {
            repository.deleteStudentsClass(studentsClass)
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
     * Retrieves all students for all classes in the currently selected academic year.
     * Useful for picking a random student across the entire year.
     * 
     * @return A flat list of [StudentWithGrade] objects for the active year.
     */
    suspend fun getAllStudentsForCurrentYear(): List<StudentWithGrade> {
        val yearId = _selectedYearId.value ?: return emptyList()
        val classes = repository.getAllStudentsClassesForYear(yearId).first()
        val allStudents = mutableListOf<StudentWithGrade>()
        classes.forEach { clazz ->
            val students = repository.getStudentsWithGradesForStudentsClass(clazz.id).first()
            allStudents.addAll(students)
        }
        return allStudents
    }

    // --- Grading Management ---

    private val _currentGrade = MutableLiveData<Grade?>()
    /** The grade details for the student currently being edited in the UI. */
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
     * Saves or updates a student's grade in the database.
     * @param grade The [Grade] object to save.
     */
    fun saveGrade(grade: Grade) {
        viewModelScope.launch {
            repository.insertGrade(grade)
        }
    }
}

/**
 * Factory class to satisfy Android's ViewModel initialization requirements.
 * Ensures the [TeacherViewModel] is created with the [Application] context.
 * 
 * @param application The application context.
 */
class TeacherViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    /**
     * Creates a new instance of the requested ViewModel class.
     * 
     * @param modelClass The class of the ViewModel to create.
     * @return A newly created ViewModel instance.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
