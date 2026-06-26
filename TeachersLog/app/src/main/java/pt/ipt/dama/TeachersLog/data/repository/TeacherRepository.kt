package pt.ipt.dama.TeachersLog.data.repository

import kotlinx.coroutines.flow.Flow
import pt.ipt.dama.TeachersLog.data.local.TeacherDao
import pt.ipt.dama.TeachersLog.data.model.*

/**
 * TeacherRepository acts as the single source of truth for academic data.
 * It abstracts access to the [TeacherDao] and manages data flows.
 * 
 * @property teacherDao The DAO used to interact with the Room database.
 */
class TeacherRepository(private val teacherDao: TeacherDao) {
    
    /** Flow emitting all registered academic years. */
    val allAcademicYears: Flow<List<AcademicYear>> = teacherDao.getAllAcademicYears()
    
    /**
     * Fetches semesters for a specific academic year.
     * @param yearId Parent academic year ID.
     */
    fun getSemestersForYear(yearId: Int): Flow<List<Semester>> =
        teacherDao.getSemestersForYear(yearId)

    /**
     * Inserts a new semester.
     * @param semester Semester record.
     */
    suspend fun insertSemester(semester: Semester) = teacherDao.insertSemester(semester)
    
    /**
     * Deletes a semester.
     * @param semester Semester record.
     */
    suspend fun deleteSemester(semester: Semester) = teacherDao.deleteSemester(semester)

    /**
     * Fetches subjects with stats for a specific semester.
     * @param semesterId Target semester ID.
     */
    fun getSubjectsWithStatsForSemester(semesterId: Int): Flow<List<SubjectWithStats>> =
        teacherDao.getSubjectsWithStatsForSemester(semesterId)

    /**
     * Fetches global stats for a semester.
     * @param semesterId Target semester ID.
     */
    fun getGlobalStatsForSemester(semesterId: Int): Flow<GlobalStats> =
        teacherDao.getGlobalStatsForSemester(semesterId)

    /**
     * Retrieves the 3 most recently accessed classes for a specific academic period.
     * @param semesterId The ID of the semester/period.
     */
    fun getRecentClassesForPeriod(semesterId: Int): Flow<List<StudentsClassWithStats>> =
        teacherDao.getRecentClassesForPeriod(semesterId)

    /**
     * Fetches classes for a specific subject.
     * @param subjectId Parent subject ID.
     */
    fun getStudentsClassesForSubject(subjectId: Int): Flow<List<StudentsClassWithStats>> =
        teacherDao.getStudentsClassesForSubject(subjectId)

    /**
     * Fetches students with their weighted grades for a class.
     * @param studentsClassId Target class ID.
     */
    fun getStudentsWithGradesForStudentsClass(studentsClassId: Int): Flow<List<StudentWithGrade>> =
        teacherDao.getStudentsWithGradesForStudentsClass(studentsClassId)

    /**
     * Fetches the qualitative grade for a student.
     * @param studentId Student ID.
     */
    suspend fun getGradeForStudent(studentId: Int): Grade? =
        teacherDao.getGradeForStudent(studentId)

    /**
     * Fetches basic student info.
     * @param studentId Student ID.
     */
    suspend fun getStudentById(studentId: Int): Student? =
        teacherDao.getStudentById(studentId)

    /**
     * Fetches the scale of a student's class.
     * @param studentId Student ID.
     */
    suspend fun getGradingScaleByStudentId(studentId: Int): String? =
        teacherDao.getGradingScaleByStudentId(studentId)

    /**
     * Fetches the scale of a specific class.
     * @param classId Class ID.
     */
    suspend fun getGradingScaleByClassId(classId: Int): String? =
        teacherDao.getGradingScaleByClassId(classId)

    /**
     * Checks if a process number exists globally.
     * @param processNumber Process string.
     * @param excludeId ID to ignore.
     */
    suspend fun existsProcessNumber(processNumber: String, excludeId: Int = -1): Boolean =
        teacherDao.existsProcessNumber(processNumber, excludeId)

    /**
     * Inserts a student.
     * @param student Student record.
     */
    suspend fun insertStudent(student: Student) = teacherDao.insertStudent(student)
    
    /**
     * Updates student info.
     * @param student Updated record.
     */
    suspend fun updateStudent(student: Student) = teacherDao.updateStudent(student)
    
    /**
     * Deletes a student.
     * @param student Record to remove.
     */
    suspend fun deleteStudent(student: Student) = teacherDao.deleteStudent(student)

    /**
     * Inserts a new year.
     * @param year Record to insert.
     */
    suspend fun insertAcademicYear(year: AcademicYear) = teacherDao.insertAcademicYear(year)
    
    /**
     * Deletes a year.
     * @param year Record to remove.
     */
    suspend fun deleteAcademicYear(year: AcademicYear) = teacherDao.deleteAcademicYear(year)

    /**
     * Inserts a subject.
     * @param subject Record to insert.
     */
    suspend fun insertSubject(subject: Subject) = teacherDao.insertSubject(subject)
    
    /**
     * Updates a subject.
     * @param subject Updated record.
     */
    suspend fun updateSubject(subject: Subject) = teacherDao.updateSubject(subject)
    
    /**
     * Deletes a subject.
     * @param subject Record to remove.
     */
    suspend fun deleteSubject(subject: Subject) = teacherDao.deleteSubject(subject)
    
    /**
     * Fetches a subject by ID.
     * @param subjectId Subject identifier.
     */
    fun getSubjectById(subjectId: Int): Flow<Subject?> = teacherDao.getSubjectById(subjectId)

    /**
     * Inserts a component.
     * @param component Criterion to insert.
     */
    suspend fun insertGradingComponent(component: GradingComponent) = teacherDao.insertGradingComponent(component)

    /**
     * Deletes a component.
     * @param component Record to remove.
     */
    suspend fun deleteGradingComponent(component: GradingComponent) = teacherDao.deleteGradingComponent(component)
    
    /**
     * Fetches criteria for a class.
     * @param classId Class identifier.
     */
    fun getGradingComponentsForClass(classId: Int): Flow<List<GradingComponent>> = teacherDao.getGradingComponentsForClass(classId)

    /**
     * Inserts a class.
     * @param studentsClass Record to insert.
     */
    suspend fun insertStudentsClass(studentsClass: StudentsClass) = teacherDao.insertStudentsClass(studentsClass)
    
    /**
     * Updates a class.
     * @param studentsClass Updated record.
     */
    suspend fun updateStudentsClass(studentsClass: StudentsClass) = teacherDao.updateStudentsClass(studentsClass)
    
    /**
     * Updates the last accessed timestamp for a class.
     * @param classId Class identifier.
     */
    suspend fun updateClassLastAccessed(classId: Int) = 
        teacherDao.updateClassLastAccessed(classId, System.currentTimeMillis())

    /**
     * Deletes a class.
     * @param studentsClass Record to remove.
     */
    suspend fun deleteStudentsClass(studentsClass: StudentsClass) = teacherDao.deleteStudentsClass(studentsClass)
    
    /**
     * Fetches a class by ID.
     * @param id Class identifier.
     */
    fun getStudentsClassById(id: Int): Flow<StudentsClass?> = teacherDao.getStudentsClassById(id)

    /**
     * Fetches a class with stats by ID.
     * @param id Class identifier.
     */
    fun getStudentsClassWithStatsById(id: Int): Flow<StudentsClassWithStats?> = 
        teacherDao.getStudentsClassWithStatsById(id)

    /**
     * Inserts a component score for a student.
     * @param score Score record.
     */
    suspend fun insertStudentScore(score: StudentScore) = teacherDao.insertStudentScore(score)
    
    /**
     * Fetches component scores for a student.
     * @param studentId Student identifier.
     */
    fun getComponentScoresForStudent(studentId: Int): Flow<List<ComponentScore>> = teacherDao.getComponentScoresForStudent(studentId)

    /**
     * Fetches subjects for a specific semester without stats.
     */
    fun getSubjectsBySemesterId(semesterId: Int): Flow<List<Subject>> =
        teacherDao.getSubjectsBySemesterIdFlow(semesterId)

    /**
     * Fetches classes for a specific subject without stats.
     */
    fun getClassesBySubjectId(subjectId: Int): Flow<List<StudentsClass>> =
        teacherDao.getClassesBySubjectIdFlow(subjectId)

    /**
     * Inserts a qualitative grade entry.
     * @param grade Grade record.
     */
    suspend fun insertGrade(grade: Grade) = teacherDao.insertGrade(grade)
}
