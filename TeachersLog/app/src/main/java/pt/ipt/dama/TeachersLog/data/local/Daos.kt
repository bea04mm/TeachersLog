package pt.ipt.dama.TeachersLog.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pt.ipt.dama.TeachersLog.data.model.*

/**
 * Data Access Object (DAO) for the Teacher's Log application.
 * 
 * This interface defines all the database operations using Room annotations.
 * It includes complex SQL queries to aggregate statistics (averages, counts)
 * across multiple related tables, including hierarchical grading support.
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
     * Inserts a new academic year.
     * @param year The [AcademicYear] to insert.
     * @return The row ID of the inserted year or -1 if a conflict occurs.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAcademicYear(year: AcademicYear): Long

    /**
     * Deletes a specific academic year and all its cascading data.
     * @param year The [AcademicYear] to delete.
     */
    @Delete
    suspend fun deleteAcademicYear(year: AcademicYear)

    /**
     * Retrieves all academic periods (semesters/periods) for a specific year.
     * Sorted by fixed educational logic: 1st Sem, 2nd Sem, 1st Per, 2nd Per, 3rd Per, Annual.
     * @param yearId The ID of the parent academic year.
     * @return A [Flow] of [Semester] objects.
     */
    @Query("""
        SELECT * FROM semesters 
        WHERE academicYearId = :yearId 
        ORDER BY CASE name
            WHEN '1st Semester' THEN 1
            WHEN '1.º Semestre' THEN 1
            WHEN '2nd Semester' THEN 2
            WHEN '2.º Semestre' THEN 2
            WHEN '1st Period' THEN 3
            WHEN '1.º Período' THEN 3
            WHEN '2nd Period' THEN 4
            WHEN '2.º Período' THEN 4
            WHEN '3rd Period' THEN 5
            WHEN '3.º Período' THEN 5
            WHEN 'Annual' THEN 6
            WHEN 'Anual' THEN 6
            ELSE 7
        END ASC
    """)
    fun getSemestersForYear(yearId: Int): Flow<List<Semester>>

    /**
     * Inserts or replaces an academic period record.
     * @param semester The [Semester] record to save.
     * @return The row ID of the saved record or -1 if a conflict occurs.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSemester(semester: Semester): Long

    /**
     * Deletes an academic period and its cascading subjects.
     * @param semester The [Semester] to remove.
     */
    @Delete
    suspend fun deleteSemester(semester: Semester)

    /**
     * Retrieves subjects for a specific semester with aggregated statistics.
     * Normalizes student grades to a 100% scale before averaging.
     * 
     * Handles hierarchical grading by calculating total weighted scores from leaf components.
     * 
     * @param semesterId The ID of the target semester.
     * @return A [Flow] of [SubjectWithStats] with percentage-based averages.
     */
    @Query("""
        SELECT s.id, s.name, s.iconName, s.colorHex, 
        (SELECT COUNT(*) FROM students stu JOIN students_classes sc ON stu.studentsClassId = sc.id WHERE sc.subjectId = s.id) as studentCount, 
        (SELECT IFNULL(AVG(sub_scores.normalized_total), 0.0) FROM (
          SELECT stu_in.studentId, (SUM(ss.score * (CAST(gc.weightPercentage AS REAL) / 100.0) * (CAST(IFNULL(parent.weightPercentage, 100) AS REAL) / 100.0)) / 
            (CASE WHEN sc_in.gradingScale = '5' THEN 5.0 WHEN sc_in.gradingScale = '20' THEN 20.0 ELSE 100.0 END) * 100.0) as normalized_total 
          FROM students stu_in 
          JOIN students_classes sc_in ON stu_in.studentsClassId = sc_in.id 
          JOIN student_scores ss ON stu_in.studentId = ss.studentId 
          JOIN grading_components gc ON ss.componentId = gc.id 
          LEFT JOIN grading_components parent ON gc.parentId = parent.id
          WHERE sc_in.subjectId = s.id AND gc.classId = sc_in.id
          GROUP BY stu_in.studentId
        ) sub_scores) as averageGrade 
        FROM subjects s 
        WHERE s.semesterId = :semesterId
    """)
    fun getSubjectsWithStatsForSemester(semesterId: Int): Flow<List<SubjectWithStats>>

    /**
     * Calculates global statistics for a specific semester.
     * @param semesterId The ID of the semester.
     * @return A [Flow] emitting [GlobalStats].
     */
    @Query("""
        SELECT 
        (SELECT COUNT(*) FROM students stu JOIN students_classes sc ON stu.studentsClassId = sc.id JOIN subjects s ON sc.subjectId = s.id WHERE s.semesterId = :semesterId) as totalStudents, 
        (SELECT COUNT(*) FROM subjects WHERE semesterId = :semesterId) as totalSubjects, 
        (SELECT IFNULL(AVG(global_scores.normalized_avg), 0.0) FROM (
          SELECT stu_in.studentId, (SUM(ss.score * (CAST(gc.weightPercentage AS REAL) / 100.0) * (CAST(IFNULL(parent.weightPercentage, 100) AS REAL) / 100.0)) / 
            (CASE WHEN sc_in.gradingScale = '5' THEN 5.0 WHEN sc_in.gradingScale = '20' THEN 20.0 ELSE 100.0 END) * 100.0) as normalized_avg 
          FROM students stu_in 
          JOIN students_classes sc_in ON stu_in.studentsClassId = sc_in.id 
          JOIN subjects s_in ON sc_in.subjectId = s_in.id 
          JOIN student_scores ss ON stu_in.studentId = ss.studentId 
          JOIN grading_components gc ON ss.componentId = gc.id 
          LEFT JOIN grading_components parent ON gc.parentId = parent.id
          WHERE s_in.semesterId = :semesterId AND gc.classId = sc_in.id
          GROUP BY stu_in.studentId
        ) global_scores) as globalAverage
    """)
    fun getGlobalStatsForSemester(semesterId: Int): Flow<GlobalStats>

    /**
     * Fetches classes for a specific subject, including class-level averages and subject context.
     * @param subjectId The ID of the parent subject.
     * @return A [Flow] of [StudentsClassWithStats].
     */
    @Query("""
        SELECT sc.id, sc.subjectId, sc.name, sc.gradingScale, s.colorHex, s.iconName,
        (SELECT COUNT(*) FROM students stu WHERE studentsClassId = sc.id) as studentCount, 
        (SELECT IFNULL(AVG(class_scores.weighted_total), 0.0) FROM (
          SELECT stu_in.studentId, SUM(ss.score * (CAST(gc.weightPercentage AS REAL) / 100.0) * (CAST(IFNULL(parent.weightPercentage, 100) AS REAL) / 100.0)) as weighted_total 
          FROM students stu_in 
          JOIN student_scores ss ON stu_in.studentId = ss.studentId 
          JOIN grading_components gc ON ss.componentId = gc.id 
          LEFT JOIN grading_components parent ON gc.parentId = parent.id
          WHERE stu_in.studentsClassId = sc.id AND gc.classId = sc.id
          GROUP BY stu_in.studentId
        ) class_scores) as averageGrade 
        FROM students_classes sc 
        JOIN subjects s ON sc.subjectId = s.id 
        WHERE sc.subjectId = :subjectId
    """)
    fun getStudentsClassesForSubject(subjectId: Int): Flow<List<StudentsClassWithStats>>

    /**
     * Retrieves the 3 most recently accessed classes for a specific academic period.
     * @param semesterId The ID of the semester/period.
     * @return A [Flow] emitting the top 3 classes.
     */
    @Query("""
        SELECT sc.id, sc.subjectId, sc.name, sc.gradingScale, s.colorHex, s.iconName,
        (SELECT COUNT(*) FROM students stu WHERE studentsClassId = sc.id) as studentCount, 
        (SELECT IFNULL(AVG(recent_scores.weighted_total), 0.0) FROM (
          SELECT stu_in.studentId, SUM(ss.score * (CAST(gc.weightPercentage AS REAL) / 100.0) * (CAST(IFNULL(parent.weightPercentage, 100) AS REAL) / 100.0)) as weighted_total 
          FROM students stu_in 
          JOIN student_scores ss ON stu_in.studentId = ss.studentId 
          JOIN grading_components gc ON ss.componentId = gc.id 
          LEFT JOIN grading_components parent ON gc.parentId = parent.id
          WHERE stu_in.studentsClassId = sc.id AND gc.classId = sc.id
          GROUP BY stu_in.studentId
        ) recent_scores) as averageGrade 
        FROM students_classes sc 
        JOIN subjects s ON sc.subjectId = s.id 
        WHERE s.semesterId = :semesterId 
        ORDER BY sc.lastAccessed DESC LIMIT 3
    """)
    fun getRecentClassesForPeriod(semesterId: Int): Flow<List<StudentsClassWithStats>>

    /**
     * Retrieves students in a class with their individual weighted grades.
     * @param studentsClassId The ID of the student class.
     * @return A [Flow] of [StudentWithGrade] objects.
     */
    @Query("""
        SELECT s.studentId, s.studentsClassId, s.numberInGroup, s.name, s.processNumber, s.profileImageUrl, 
        IFNULL((SELECT SUM(ss.score * (CAST(gc.weightPercentage AS REAL) / 100.0) * (CAST(IFNULL(parent.weightPercentage, 100) AS REAL) / 100.0)) 
                FROM student_scores ss 
                JOIN grading_components gc ON ss.componentId = gc.id 
                LEFT JOIN grading_components parent ON gc.parentId = parent.id
                WHERE ss.studentId = s.studentId AND gc.classId = s.studentsClassId), 0.0) as weightedGrade
        FROM students s 
        WHERE s.studentsClassId = :studentsClassId ORDER BY s.numberInGroup ASC
    """)
    fun getStudentsWithGradesForStudentsClass(studentsClassId: Int): Flow<List<StudentWithGrade>>

    /**
     * Retrieves dynamic component scores for a specific student.
     * Supports hierarchical structures with absolute weights.
     * @param studentId The ID of the student.
     * @return A [Flow] of [ComponentScore] objects.
     */
    @Query("""
        SELECT gc.id as componentId, gc.parentId, gc.name as componentName, gc.weightPercentage as weight, 
        (gc.weightPercentage * (IFNULL(parent.weightPercentage, 100) / 100.0)) as absoluteWeight,
        IFNULL(ss.score, 0.0) as score,
        EXISTS(SELECT 1 FROM grading_components sub WHERE sub.parentId = gc.id) as isGroup
        FROM grading_components gc 
        LEFT JOIN student_scores ss ON gc.id = ss.componentId AND ss.studentId = :studentId 
        LEFT JOIN grading_components parent ON gc.parentId = parent.id
        WHERE gc.classId = (SELECT studentsClassId FROM students WHERE studentId = :studentId)
        ORDER BY CASE WHEN gc.parentId IS NULL THEN 0 ELSE 1 END, gc.parentId, gc.id
    """)
    fun getComponentScoresForStudent(studentId: Int): Flow<List<ComponentScore>>

    /**
     * Fetches qualitative grade record (notes) for a student.
     * @param studentId The student ID.
     * @return The [Grade] object or null.
     */
    @Query("SELECT * FROM grades WHERE studentId = :studentId")
    suspend fun getGradeForStudent(studentId: Int): Grade?

    /**
     * Retrieves a single student record by ID.
     * @param studentId The student ID.
     * @return The [Student] object or null.
     */
    @Query("SELECT * FROM students WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: Int): Student?

    /**
     * Fetches the grading scale used by a student's class.
     * @param studentId The student ID.
     * @return The scale identifier string.
     */
    @Query("SELECT gradingScale FROM students_classes WHERE id = (SELECT studentsClassId FROM students WHERE studentId = :studentId)")
    suspend fun getGradingScaleByStudentId(studentId: Int): String?

    /**
     * Fetches the grading scale used by a specific class.
     * @param classId The class ID.
     * @return The scale identifier string.
     */
    @Query("SELECT gradingScale FROM students_classes WHERE id = :classId")
    suspend fun getGradingScaleByClassId(classId: Int): String?

    /**
     * Checks if a process number is already taken globally.
     * @param processNumber Process string.
     * @param excludeId ID to ignore (self-check).
     * @return True if exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM students WHERE processNumber = :processNumber AND studentId != :excludeId)")
    suspend fun existsProcessNumber(processNumber: String, excludeId: Int = -1): Boolean

    /**
     * Inserts a new subject.
     * @param subject The [Subject] record.
     * @return Row ID or -1 if conflicted.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubject(subject: Subject): Long

    /**
     * Updates subject details.
     * @param subject Updated [Subject].
     */
    @Update
    suspend fun updateSubject(subject: Subject)

    /**
     * Deletes a subject and cascading classes.
     * @param subject Subject to remove.
     */
    @Delete
    suspend fun deleteSubject(subject: Subject)

    /**
     * Fetches a subject by ID.
     * @param subjectId Subject identifier.
     * @return [Flow] with [Subject] data.
     */
    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    fun getSubjectById(subjectId: Int): Flow<Subject?>

    /**
     * Inserts a grading component.
     * @param component The [GradingComponent].
     * @return Row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradingComponent(component: GradingComponent): Long

    /**
     * Deletes a grading component.
     * @param component Component to remove.
     */
    @Delete
    suspend fun deleteGradingComponent(component: GradingComponent)

    /**
     * Fetches criteria for a class.
     * @param classId Class identifier.
     * @return [Flow] of [GradingComponent] list.
     */
    @Query("SELECT * FROM grading_components WHERE classId = :classId")
    fun getGradingComponentsForClass(classId: Int): Flow<List<GradingComponent>>

    /**
     * Inserts a student class.
     * @param studentsClass The [StudentsClass].
     * @return Row ID or -1 if conflicted.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudentsClass(studentsClass: StudentsClass): Long

    /**
     * Updates a class.
     * @param studentsClass Updated [StudentsClass].
     */
    @Update
    suspend fun updateStudentsClass(studentsClass: StudentsClass)

    /**
     * Updates the last accessed timestamp for a class.
     * @param classId Class identifier.
     * @param timestamp Current time in ms.
     */
    @Query("UPDATE students_classes SET lastAccessed = :timestamp WHERE id = :classId")
    suspend fun updateClassLastAccessed(classId: Int, timestamp: Long)

    /**
     * Deletes a class.
     * @param studentsClass Class to remove.
     */
    @Delete
    suspend fun deleteStudentsClass(studentsClass: StudentsClass)

    /**
     * Fetches a class by ID.
     * @param id Class identifier.
     * @return [Flow] with [StudentsClass] data.
     */
    @Query("SELECT * FROM students_classes WHERE id = :id")
    fun getStudentsClassById(id: Int): Flow<StudentsClass?>

    /**
     * Fetches a single class with its statistics and parent subject icon.
     * @param id The class identifier.
     * @return A [Flow] with the [StudentsClassWithStats] object.
     */
    @Query("""
        SELECT sc.id, sc.subjectId, sc.name, sc.gradingScale, s.colorHex, s.iconName,
        (SELECT COUNT(*) FROM students stu WHERE studentsClassId = sc.id) as studentCount, 
        (SELECT IFNULL(AVG(class_scores.weighted_total), 0.0) FROM (
          SELECT stu_in.studentId, SUM(ss.score * (gc.weightPercentage / 100.0) * (IFNULL(parent.weightPercentage, 100) / 100.0)) as weighted_total 
          FROM students stu_in 
          JOIN student_scores ss ON stu_in.studentId = ss.studentId 
          JOIN grading_components gc ON ss.componentId = gc.id 
          LEFT JOIN grading_components parent ON gc.parentId = parent.id
          WHERE stu_in.studentsClassId = sc.id 
          GROUP BY stu_in.studentId
        ) class_scores) as averageGrade 
        FROM students_classes sc 
        JOIN subjects s ON sc.subjectId = s.id 
        WHERE sc.id = :id
    """)
    fun getStudentsClassWithStatsById(id: Int): Flow<StudentsClassWithStats?>

    /**
     * Inserts a student.
     * @param student The [Student] record.
     * @return Row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    /**
     * Updates student info.
     * @param student Updated [Student].
     */
    @Update
    suspend fun updateStudent(student: Student)

    /**
     * Deletes a student.
     * @param student Student to remove.
     */
    @Delete
    suspend fun deleteStudent(student: Student)

    /**
     * Inserts a component score.
     * @param score The [StudentScore].
     * @return Row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentScore(score: StudentScore): Long

    /**
     * Inserts a qualitative grade.
     * @param grade The [Grade].
     * @return Row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: Grade): Long

    /**
     * Fetches all subjects for a specific semester without stats.
     * @param semesterId The ID of the parent semester.
     * @return A list of [Subject] objects.
     */
    @Query("SELECT * FROM subjects WHERE semesterId = :semesterId")
    suspend fun getSubjectsBySemesterId(semesterId: Int): List<Subject>

    /**
     * Fetches all subjects for a specific semester as an observable flow.
     * @param semesterId The ID of the parent semester.
     * @return A [Flow] emitting lists of [Subject].
     */
    @Query("SELECT * FROM subjects WHERE semesterId = :semesterId")
    fun getSubjectsBySemesterIdFlow(semesterId: Int): Flow<List<Subject>>

    /**
     * Fetches all classes for a specific subject without stats.
     * @param subjectId The ID of the parent subject.
     * @return A list of [StudentsClass] objects.
     */
    @Query("SELECT * FROM students_classes WHERE subjectId = :subjectId")
    suspend fun getClassesBySubjectId(subjectId: Int): List<StudentsClass>

    /**
     * Fetches all classes for a specific subject as an observable flow.
     * @param subjectId The ID of the parent subject.
     * @return A [Flow] emitting lists of [StudentsClass].
     */
    @Query("SELECT * FROM students_classes WHERE subjectId = :subjectId")
    fun getClassesBySubjectIdFlow(subjectId: Int): Flow<List<StudentsClass>>

    /**
     * Fetches all students for a specific class.
     * @param classId The ID of the parent class.
     * @return A list of [Student] objects.
     */
    @Query("SELECT * FROM students WHERE studentsClassId = :classId")
    suspend fun getStudentsByClassId(classId: Int): List<Student>

    /**
     * Fetches all grading components for a specific class.
     * @param classId The ID of the parent class.
     * @return A list of [GradingComponent] objects.
     */
    @Query("SELECT * FROM grading_components WHERE classId = :classId")
    suspend fun getGradingComponentsByClassId(classId: Int): List<GradingComponent>

    /**
     * Fetches a single class record by ID synchronously.
     * @param classId The class identifier.
     * @return The [StudentsClass] or null if not found.
     */
    @Query("SELECT * FROM students_classes WHERE id = :classId")
    suspend fun getStudentsClassByIdSync(classId: Int): StudentsClass?

    /**
     * Fetches a single subject record by ID synchronously.
     * @param subjectId The subject identifier.
     * @return The [Subject] or null if not found.
     */
    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun getSubjectByIdSync(subjectId: Int): Subject?

    /**
     * Fetches a single academic period (semester) by ID synchronously.
     * @param semesterId The semester identifier.
     * @return The [Semester] or null if not found.
     */
    @Query("SELECT * FROM semesters WHERE id = :semesterId")
    suspend fun getSemesterByIdSync(semesterId: Int): Semester?

    /**
     * Fetches all semesters for a specific year synchronously.
     * @param yearId The ID of the parent academic year.
     * @return A list of [Semester] objects.
     */
    @Query("SELECT * FROM semesters WHERE academicYearId = :yearId")
    suspend fun getSemestersForYearList(yearId: Int): List<Semester>

    /**
     * Retrieves all academic years synchronously for backup purposes.
     * @return A list of [AcademicYear].
     */
    @Query("SELECT * FROM academic_years")
    suspend fun getAllAcademicYearsSync(): List<AcademicYear>

    /**
     * Retrieves all semesters synchronously for backup purposes.
     * @return A list of [Semester].
     */
    @Query("SELECT * FROM semesters")
    suspend fun getAllSemestersSync(): List<Semester>

    /**
     * Retrieves all subjects synchronously for backup purposes.
     * @return A list of [Subject].
     */
    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsSync(): List<Subject>

    /**
     * Retrieves all classes synchronously for backup purposes.
     * @return A list of [StudentsClass].
     */
    @Query("SELECT * FROM students_classes")
    suspend fun getAllClassesSync(): List<StudentsClass>

    /**
     * Retrieves all grading components synchronously for backup purposes.
     * @return A list of [GradingComponent].
     */
    @Query("SELECT * FROM grading_components")
    suspend fun getAllGradingComponentsSync(): List<GradingComponent>

    /**
     * Retrieves all students synchronously for backup purposes.
     * @return A list of [Student].
     */
    @Query("SELECT * FROM students")
    suspend fun getAllStudentsSync(): List<Student>

    /**
     * Retrieves all student scores synchronously for backup purposes.
     * @return A list of [StudentScore].
     */
    @Query("SELECT * FROM student_scores")
    suspend fun getAllStudentScoresSync(): List<StudentScore>

    /**
     * Retrieves all grades (notes) synchronously for backup purposes.
     * @return A list of [Grade].
     */
    @Query("SELECT * FROM grades")
    suspend fun getAllGradesSync(): List<Grade>

    // --- Bulk Insert Methods for Restoration ---

    /**
     * Inserts multiple academic years, replacing on conflict.
     * @param years The list of [AcademicYear] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicYears(years: List<AcademicYear>)

    /**
     * Inserts multiple semesters, replacing on conflict.
     * @param semesters The list of [Semester] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemesters(semesters: List<Semester>)

    /**
     * Inserts multiple subjects, replacing on conflict.
     * @param subjects The list of [Subject] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    /**
     * Inserts multiple student classes, replacing on conflict.
     * @param classes The list of [StudentsClass] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentsClasses(classes: List<StudentsClass>)

    /**
     * Inserts multiple grading components, replacing on conflict.
     * @param components The list of [GradingComponent] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradingComponents(components: List<GradingComponent>)

    /**
     * Inserts multiple students, replacing on conflict.
     * @param students The list of [Student] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    /**
     * Inserts multiple student scores, replacing on conflict.
     * @param scores The list of [StudentScore] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentScores(scores: List<StudentScore>)

    /**
     * Inserts multiple grades, replacing on conflict.
     * @param grades The list of [Grade] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<Grade>)

    /**
     * Wipes all academic year records (and cascading data) from the local database.
     */
    @Query("DELETE FROM academic_years")
    suspend fun clearAllData()
}
