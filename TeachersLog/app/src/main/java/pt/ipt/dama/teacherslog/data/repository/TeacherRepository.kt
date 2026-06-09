package pt.ipt.dama.teacherslog.data.repository

import kotlinx.coroutines.flow.Flow
import pt.ipt.dama.teacherslog.data.local.TeacherDao
import pt.ipt.dama.teacherslog.data.model.*

/**
 * Single source of truth for the application's academic data.
 */
class TeacherRepository(private val teacherDao: TeacherDao) {
    
    val allAcademicYears: Flow<List<AcademicYear>> = teacherDao.getAllAcademicYears()
    
    fun getSubjectsWithStatsForYear(yearId: Int): Flow<List<SubjectWithStats>> =
        teacherDao.getSubjectsWithStatsForYear(yearId)

    fun getGlobalStatsForYear(yearId: Int): Flow<GlobalStats> =
        teacherDao.getGlobalStatsForYear(yearId)

    fun getAllStudentsClassesForYear(yearId: Int): Flow<List<StudentsClassWithStats>> =
        teacherDao.getAllStudentsClassesForYear(yearId)

    fun getStudentsClassesForSubject(subjectId: Int): Flow<List<StudentsClassWithStats>> =
        teacherDao.getStudentsClassesForSubject(subjectId)

    fun getStudentsWithGradesForStudentsClass(studentsClassId: Int): Flow<List<StudentWithGrade>> =
        teacherDao.getStudentsWithGradesForStudentsClass(studentsClassId)

    suspend fun getGradeForStudent(studentId: Int): Grade? =
        teacherDao.getGradeForStudent(studentId)

    suspend fun getStudentById(studentId: Int): Student? =
        teacherDao.getStudentById(studentId)

    suspend fun insertAcademicYear(year: AcademicYear) = teacherDao.insertAcademicYear(year)
    suspend fun deleteAcademicYear(year: AcademicYear) = teacherDao.deleteAcademicYear(year)
    suspend fun insertGrade(grade: Grade) = teacherDao.insertGrade(grade)
}
