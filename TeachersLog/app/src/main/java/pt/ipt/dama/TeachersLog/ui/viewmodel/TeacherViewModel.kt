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
     * Adds a new student record to the database.
     * 
     * @param name Full name.
     * @param number Roster number.
     * @param process Admin process number.
     * @param photoUrl Image URI or path.
     * @param classId Target class ID.
     */
    fun addStudent(name: String, number: Int, process: String?, photoUrl: String?, classId: Int) {
        viewModelScope.launch {
            repository.insertStudent(Student(
                studentsClassId = classId,
                numberInGroup = number,
                name = name,
                processNumber = process,
                profileImageUrl = photoUrl
            ))
            triggerSync()
        }
    }

    /**
     * Updates an existing student's record.
     * @param student The updated [Student] object.
     */
    fun updateStudent(student: Student) {
        viewModelScope.launch {
            repository.updateStudent(student)
            triggerSync()
        }
    }

    /**
     * Deletes a student and their associated grades/scores.
     * @param student The [Student] object to delete.
     */
    fun deleteStudent(student: Student) {
        viewModelScope.launch {
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
            // Use first() to check preference only once at the moment of mutation
            val enabled = sessionManager.isSyncEnabled.first()
            if (enabled) {
                syncRepository.uploadToCloud()
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
