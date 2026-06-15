package pt.ipt.dama.teacherslog.data.repository

import kotlinx.coroutines.flow.Flow
import pt.ipt.dama.teacherslog.data.local.TeacherDao
import pt.ipt.dama.teacherslog.data.model.*

/**
 * TeacherRepository acts as the single source of truth for the application's academic data.
 * 
 * It abstracts access to the [TeacherDao], coordinating data operations between the 
 * local Room database and the rest of the application (typically the ViewModel).
 *
 * @property teacherDao The Data Access Object used to interact with the Room database.
 */
class TeacherRepository(private val teacherDao: TeacherDao) {
    
    /**
     * A [Flow] emitting the list of all registered academic years, ordered by year descending.
     */
    val allAcademicYears: Flow<List<AcademicYear>> = teacherDao.getAllAcademicYears()
    
    /**
     * Retrieves all subjects and their calculated statistics for a specific academic year.
     * 
     * @param yearId The unique identifier of the academic year.
     * @return A [Flow] emitting a list of [SubjectWithStats].
     */
    fun getSubjectsWithStatsForYear(yearId: Int): Flow<List<SubjectWithStats>> =
        teacherDao.getSubjectsWithStatsForYear(yearId)

    /**
     * Retrieves the global academic statistics (totals and averages) for a specific year.
     * 
     * @param yearId The unique identifier of the academic year.
     * @return A [Flow] emitting the [GlobalStats].
     */
    fun getGlobalStatsForYear(yearId: Int): Flow<GlobalStats> =
        teacherDao.getGlobalStatsForYear(yearId)

    /**
     * Fetches all student classes across all subjects for a specific academic year.
     * 
     * @param yearId The unique identifier of the academic year.
     * @return A [Flow] emitting a list of [StudentsClassWithStats].
     */
    fun getAllStudentsClassesForYear(yearId: Int): Flow<List<StudentsClassWithStats>> =
        teacherDao.getAllStudentsClassesForYear(yearId)

    /**
     * Fetches the classes specifically associated with a single subject.
     * 
     * @param subjectId The unique identifier of the subject.
     * @return A [Flow] emitting a list of [StudentsClassWithStats].
     */
    fun getStudentsClassesForSubject(subjectId: Int): Flow<List<StudentsClassWithStats>> =
        teacherDao.getStudentsClassesForSubject(subjectId)

    /**
     * Retrieves the list of students within a class along with their weighted grades.
     * 
     * @param studentsClassId The unique identifier of the student class.
     * @return A [Flow] emitting a list of [StudentWithGrade].
     */
    fun getStudentsWithGradesForStudentsClass(studentsClassId: Int): Flow<List<StudentWithGrade>> =
        teacherDao.getStudentsWithGradesForStudentsClass(studentsClassId)

    /**
     * Fetches the raw grade data for a specific student.
     * 
     * @param studentId The unique identifier of the student.
     * @return The [Grade] object if found, or null otherwise.
     */
    suspend fun getGradeForStudent(studentId: Int): Grade? =
        teacherDao.getGradeForStudent(studentId)

    /**
     * Retrieves basic student information by their ID.
     * 
     * @param studentId The unique identifier of the student.
     * @return The [Student] object if found, or null otherwise.
     */
    suspend fun getStudentById(studentId: Int): Student? =
        teacherDao.getStudentById(studentId)

    /**
     * Checks if a student number is already used in a specific class.
     */
    suspend fun existsNumberInClass(classId: Int, number: Int, excludeId: Int = -1): Boolean =
        teacherDao.existsNumberInClass(classId, number, excludeId)

    /**
     * Checks if a process number is already used globally.
     */
    suspend fun existsProcessNumber(processNumber: String, excludeId: Int = -1): Boolean =
        teacherDao.existsProcessNumber(processNumber, excludeId)

    /**
     * Inserts a new student into the database.
     * 
     * @param student The [Student] entity to insert.
     * @return The row ID of the newly inserted student.
     */
    suspend fun insertStudent(student: Student) = teacherDao.insertStudent(student)

    /**
     * Updates an existing student's information.
     * 
     * @param student The [Student] entity with updated values.
     */
    suspend fun updateStudent(student: Student) = teacherDao.updateStudent(student)

    /**
     * Deletes a student and their cascading data.
     * 
     * @param student The [Student] entity to delete.
     */
    suspend fun deleteStudent(student: Student) = teacherDao.deleteStudent(student)

    // --- Academic Year Operations ---

    /**
     * Inserts a new academic year into the database.
     * 
     * @param year The [AcademicYear] entity to insert.
     * @return The row ID of the newly inserted year.
     */
    suspend fun insertAcademicYear(year: AcademicYear) = teacherDao.insertAcademicYear(year)

    /**
     * Deletes an academic year and all cascading data (subjects, classes, etc.).
     * 
     * @param year The [AcademicYear] entity to delete.
     */
    suspend fun deleteAcademicYear(year: AcademicYear) = teacherDao.deleteAcademicYear(year)
    
    // --- Subject Operations ---

    /**
     * Inserts a new subject into the database.
     * 
     * @param subject The [Subject] entity to insert.
     * @return The row ID of the newly inserted subject.
     */
    suspend fun insertSubject(subject: Subject) = teacherDao.insertSubject(subject)

    /**
     * Updates an existing subject's information (name, icon, or color).
     * 
     * @param subject The [Subject] entity with updated values.
     */
    suspend fun updateSubject(subject: Subject) = teacherDao.updateSubject(subject)

    /**
     * Deletes a subject and its cascading classes and student records.
     * 
     * @param subject The [Subject] entity to delete.
     */
    suspend fun deleteSubject(subject: Subject) = teacherDao.deleteSubject(subject)

    /**
     * Retrieves a subject by its unique ID.
     * 
     * @param subjectId The unique identifier of the subject.
     * @return A [Flow] emitting the [Subject] or null if it doesn't exist.
     */
    fun getSubjectById(subjectId: Int): Flow<Subject?> = teacherDao.getSubjectById(subjectId)

    /**
     * Inserts a new class into the database.
     * 
     * @param studentsClass The [StudentsClass] entity to insert.
     * @return The row ID of the newly inserted class.
     */
    suspend fun insertStudentsClass(studentsClass: StudentsClass) = teacherDao.insertStudentsClass(studentsClass)

    /**
     * Updates an existing class information.
     * 
     * @param studentsClass The [StudentsClass] entity with updated values.
     */
    suspend fun updateStudentsClass(studentsClass: StudentsClass) = teacherDao.updateStudentsClass(studentsClass)

    /**
     * Deletes a class and its cascading student records.
     * 
     * @param studentsClass The [StudentsClass] entity to delete.
     */
    suspend fun deleteStudentsClass(studentsClass: StudentsClass) = teacherDao.deleteStudentsClass(studentsClass)

    // --- Grade Operations ---

    /**
     * Inserts or replaces a student's grade data.
     * 
     * @param grade The [Grade] entity to save.
     * @return The row ID of the saved grade record.
     */
    suspend fun insertGrade(grade: Grade) = teacherDao.insertGrade(grade)
}
