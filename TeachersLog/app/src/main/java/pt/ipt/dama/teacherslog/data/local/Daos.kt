package pt.ipt.dama.teacherslog.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pt.ipt.dama.teacherslog.data.model.*

/**
 * Data Access Object (DAO) for the Teacher's Log application.
 */
@Dao
interface TeacherDao {
    
    @Query("SELECT * FROM academic_years ORDER BY year DESC")
    fun getAllAcademicYears(): Flow<List<AcademicYear>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAcademicYear(year: AcademicYear): Long

    @Delete
    suspend fun deleteAcademicYear(year: AcademicYear)

    @Query("SELECT s.*, " +
           "(SELECT COUNT(*) FROM students stu JOIN students_classes sc ON stu.studentsClassId = sc.id WHERE sc.subjectId = s.id) as studentCount, " +
           "(SELECT IFNULL(AVG(grd.homework * 0.2 + grd.participation * 0.1 + grd.quizzes * 0.4 + grd.projects * 0.3), 0.0) " +
           " FROM grades grd JOIN students stu ON grd.studentId = stu.studentId JOIN students_classes sc ON stu.studentsClassId = sc.id " +
           " WHERE sc.subjectId = s.id) as averageGrade " +
           "FROM subjects s " +
           "WHERE s.academicYearId = :yearId")
    fun getSubjectsWithStatsForYear(yearId: Int): Flow<List<SubjectWithStats>>

    @Query("SELECT " +
           "(SELECT COUNT(*) FROM students stu JOIN students_classes sc ON stu.studentsClassId = sc.id JOIN subjects s ON sc.subjectId = s.id WHERE s.academicYearId = :yearId) as totalStudents, " +
           "(SELECT COUNT(*) FROM subjects WHERE academicYearId = :yearId) as totalSubjects, " +
           "(SELECT IFNULL(AVG(grd.homework * 0.2 + grd.participation * 0.1 + grd.quizzes * 0.4 + grd.projects * 0.3), 0.0) " +
           " FROM grades grd JOIN students stu ON grd.studentId = stu.studentId JOIN students_classes sc ON stu.studentsClassId = sc.id JOIN subjects s ON sc.subjectId = s.id " +
           " WHERE s.academicYearId = :yearId) as globalAverage")
    fun getGlobalStatsForYear(yearId: Int): Flow<GlobalStats>

    @Query("SELECT sc.*, " +
           "(SELECT COUNT(*) FROM students stu WHERE studentsClassId = sc.id) as studentCount, " +
           "(SELECT IFNULL(AVG(grd.homework * 0.2 + grd.participation * 0.1 + grd.quizzes * 0.4 + grd.projects * 0.3), 0.0) " + 
           " FROM grades grd JOIN students stu ON grd.studentId = stu.studentId WHERE stu.studentsClassId = sc.id) as averageGrade " +
           "FROM students_classes sc WHERE sc.subjectId = :subjectId")
    fun getStudentsClassesForSubject(subjectId: Int): Flow<List<StudentsClassWithStats>>

    @Query("SELECT sc.*, " +
           "(SELECT COUNT(*) FROM students stu WHERE studentsClassId = sc.id) as studentCount, " +
           "(SELECT IFNULL(AVG(grd.homework * 0.2 + grd.participation * 0.1 + grd.quizzes * 0.4 + grd.projects * 0.3), 0.0) " + 
           " FROM grades grd JOIN students stu ON grd.studentId = stu.studentId WHERE stu.studentsClassId = sc.id) as averageGrade " +
           "FROM students_classes sc JOIN subjects s ON sc.subjectId = s.id WHERE s.academicYearId = :yearId")
    fun getAllStudentsClassesForYear(yearId: Int): Flow<List<StudentsClassWithStats>>

    @Query("SELECT s.studentId, s.studentsClassId, s.numberInGroup, s.name, s.processNumber, " +
           "IFNULL((g.homework * 0.2 + g.participation * 0.1 + g.quizzes * 0.4 + g.projects * 0.3), 0.0) as weightedGrade " +
           "FROM students s LEFT JOIN grades g ON s.studentId = g.studentId " +
           "WHERE s.studentsClassId = :studentsClassId ORDER BY s.numberInGroup ASC")
    fun getStudentsWithGradesForStudentsClass(studentsClassId: Int): Flow<List<StudentWithGrade>>

    @Query("SELECT * FROM grades WHERE studentId = :studentId")
    suspend fun getGradeForStudent(studentId: Int): Grade?

    @Query("SELECT * FROM students WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: Int): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentsClass(studentsClass: StudentsClass): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: Grade): Long
}
