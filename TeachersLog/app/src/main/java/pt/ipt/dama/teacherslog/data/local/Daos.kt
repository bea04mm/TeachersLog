package pt.ipt.dama.teacherslog.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pt.ipt.dama.teacherslog.data.model.*

/**
 * Data Access Object (DAO) for the Teacher's Log application.
 * 
 * Defines the database operations for academic years, subjects, classes, 
 * students, and grades. It utilizes complex SQL queries to aggregate 
 * statistics (averages and counts) directly from the database.
 */
@Dao
interface TeacherDao {
    
    /**
     * Retrieves all registered academic years ordered by year descending.
     * @return A [Flow] emitting the list of [AcademicYear].
     */
    @Query("SELECT * FROM academic_years ORDER BY year DESC")
    fun getAllAcademicYears(): Flow<List<AcademicYear>>

    /**
     * Inserts a new academic year. Ignores duplicates based on unique indices.
     * @param year The [AcademicYear] to insert.
     * @return The row ID of the newly inserted year.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAcademicYear(year: AcademicYear): Long

    /**
     * Deletes a specific academic year. Cascades to related subjects and data.
     * @param year The [AcademicYear] to remove.
     */
    @Delete
    suspend fun deleteAcademicYear(year: AcademicYear)

    /**
     * Retrieves subjects for a specific year, including aggregated stats:
     * 1. studentCount: Total students across all classes of the subject.
     * 2. averageGrade: Weighted average of all student grades in the subject.
     * 
     * @param yearId The ID of the target academic year.
     * @return A [Flow] of [SubjectWithStats] objects.
     */
    @Query("SELECT s.*, " +
           "(SELECT COUNT(*) FROM students stu JOIN students_classes sc ON stu.studentsClassId = sc.id WHERE sc.subjectId = s.id) as studentCount, " +
           "(SELECT IFNULL(AVG(grd.homework * 0.2 + grd.participation * 0.1 + grd.quizzes * 0.4 + grd.projects * 0.3), 0.0) " +
           " FROM grades grd JOIN students stu ON grd.studentId = stu.studentId JOIN students_classes sc ON stu.studentsClassId = sc.id " +
           " WHERE sc.subjectId = s.id) as averageGrade " +
           "FROM subjects s " +
           "WHERE s.academicYearId = :yearId")
    fun getSubjectsWithStatsForYear(yearId: Int): Flow<List<SubjectWithStats>>

    /**
     * Calculates global statistics for a specific academic year:
     * - totalStudents: Sum of all students across all subjects.
     * - totalSubjects: Count of subjects in the year.
     * - globalAverage: Weighted average of all grades recorded in that year.
     * 
     * @param yearId The ID of the target academic year.
     * @return A [Flow] emitting the [GlobalStats].
     */
    @Query("SELECT " +
           "(SELECT COUNT(*) FROM students stu JOIN students_classes sc ON stu.studentsClassId = sc.id JOIN subjects s ON sc.subjectId = s.id WHERE s.academicYearId = :yearId) as totalStudents, " +
           "(SELECT COUNT(*) FROM subjects WHERE academicYearId = :yearId) as totalSubjects, " +
           "(SELECT IFNULL(AVG(grd.homework * 0.2 + grd.participation * 0.1 + grd.quizzes * 0.4 + grd.projects * 0.3), 0.0) " +
           " FROM grades grd JOIN students stu ON grd.studentId = stu.studentId JOIN students_classes sc ON stu.studentsClassId = sc.id JOIN subjects s ON sc.subjectId = s.id " +
           " WHERE s.academicYearId = :yearId) as globalAverage")
    fun getGlobalStatsForYear(yearId: Int): Flow<GlobalStats>

    /**
     * Fetches classes associated with a specific subject, including class-level stats.
     * @param subjectId The ID of the subject.
     * @return A [Flow] of [StudentsClassWithStats].
     */
    @Query("SELECT sc.*, " +
           "(SELECT COUNT(*) FROM students stu WHERE studentsClassId = sc.id) as studentCount, " +
           "(SELECT IFNULL(AVG(grd.homework * 0.2 + grd.participation * 0.1 + grd.quizzes * 0.4 + grd.projects * 0.3), 0.0) " + 
           " FROM grades grd JOIN students stu ON grd.studentId = stu.studentId WHERE stu.studentsClassId = sc.id) as averageGrade " +
           "FROM students_classes sc WHERE sc.subjectId = :subjectId")
    fun getStudentsClassesForSubject(subjectId: Int): Flow<List<StudentsClassWithStats>>

    /**
     * Fetches all classes registered for a specific academic year.
     * @param yearId The ID of the academic year.
     * @return A [Flow] of [StudentsClassWithStats].
     */
    @Query("SELECT sc.*, " +
           "(SELECT COUNT(*) FROM students stu WHERE studentsClassId = sc.id) as studentCount, " +
           "(SELECT IFNULL(AVG(grd.homework * 0.2 + grd.participation * 0.1 + grd.quizzes * 0.4 + grd.projects * 0.3), 0.0) " + 
           " FROM grades grd JOIN students stu ON grd.studentId = stu.studentId WHERE stu.studentsClassId = sc.id) as averageGrade " +
           "FROM students_classes sc JOIN subjects s ON sc.subjectId = s.id WHERE s.academicYearId = :yearId")
    fun getAllStudentsClassesForYear(yearId: Int): Flow<List<StudentsClassWithStats>>

    /**
     * Retrieves students in a class with their individual weighted grades.
     * Performs a LEFT JOIN with the grades table to include students without grades yet.
     * 
     * @param studentsClassId The ID of the class.
     * @return A [Flow] of [StudentWithGrade].
     */
    @Query("SELECT s.studentId, s.studentsClassId, s.numberInGroup, s.name, s.processNumber, " +
           "IFNULL((g.homework * 0.2 + g.participation * 0.1 + g.quizzes * 0.4 + g.projects * 0.3), 0.0) as weightedGrade " +
           "FROM students s LEFT JOIN grades g ON s.studentId = g.studentId " +
           "WHERE s.studentsClassId = :studentsClassId ORDER BY s.numberInGroup ASC")
    fun getStudentsWithGradesForStudentsClass(studentsClassId: Int): Flow<List<StudentWithGrade>>

    /**
     * Fetches the raw grade record for a specific student.
     */
    @Query("SELECT * FROM grades WHERE studentId = :studentId")
    suspend fun getGradeForStudent(studentId: Int): Grade?

    /**
     * Retrieves a student record by ID.
     */
    @Query("SELECT * FROM students WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: Int): Student?

    /**
     * Inserts or replaces a subject record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    /**
     * Updates an existing subject record.
     */
    @Update
    suspend fun updateSubject(subject: Subject)

    /**
     * Deletes a subject record.
     */
    @Delete
    suspend fun deleteSubject(subject: Subject)

    /**
     * Retrieves a subject by its unique ID.
     */
    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    fun getSubjectById(subjectId: Int): Flow<Subject?>

    /**
     * Inserts or replaces a class record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentsClass(studentsClass: StudentsClass): Long

    /**
     * Inserts or replaces a student record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    /**
     * Inserts or replaces a grade record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: Grade): Long
}
