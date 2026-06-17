package pt.ipt.dama.teacherslog.data.local

import kotlinx.coroutines.flow.first
import pt.ipt.dama.teacherslog.data.model.*

/**
 * DatabaseSeeder populates the Room database with initial mock data.
 * It sets up a complete academic hierarchy for testing and first-run experience.
 */
object DatabaseSeeder {

    /**
     * Seeds the database with initial academic data if no data exists.
     * 
     * @param teacherDao The Data Access Object used for database operations.
     */
    suspend fun seed(teacherDao: TeacherDao) {
        // Check if there are already academic years registered to maintain idempotency
        val existingYears = teacherDao.getAllAcademicYears().first()
        if (existingYears.isNotEmpty()) return

        // --- 1. Academic Years ---
        val year2425Id = teacherDao.insertAcademicYear(AcademicYear(year = "2024/2025", isCurrent = true)).toInt()
        if (year2425Id == -1) return

        // --- 2. Semesters ---
        val sem1Id = teacherDao.insertSemester(Semester(academicYearId = year2425Id, name = "1st Semester")).toInt()
        if (sem1Id == -1) return
        teacherDao.insertSemester(Semester(academicYearId = year2425Id, name = "2nd Semester"))

        // --- 3. Subjects ---
        val sub1Id = teacherDao.insertSubject(Subject(semesterId = sem1Id, name = "Mathematics", iconName = "functions", colorHex = "#004AC6")).toInt()
        teacherDao.insertSubject(Subject(semesterId = sem1Id, name = "Science", iconName = "biotech", colorHex = "#006686"))

        // --- 4. Classes with Hierarchical Grading ---
        
        // Math Class: Grouped Evaluation
        val mathClassId = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub1Id, name = "10.ºA", gradingScale = "20")).toInt()
        
        // Root Groups
        val groupTestsId = teacherDao.insertGradingComponent(GradingComponent(classId = mathClassId, name = "Tests", weightPercentage = 60)).toInt()
        val groupWorkId = teacherDao.insertGradingComponent(GradingComponent(classId = mathClassId, name = "Assignments", weightPercentage = 40)).toInt()
        
        // Sub-items for Tests
        val test1Id = teacherDao.insertGradingComponent(GradingComponent(classId = mathClassId, parentId = groupTestsId, name = "1st Test", weightPercentage = 50)).toInt()
        val test2Id = teacherDao.insertGradingComponent(GradingComponent(classId = mathClassId, parentId = groupTestsId, name = "2nd Test", weightPercentage = 50)).toInt()
        
        // Sub-items for Assignments
        val hw1Id = teacherDao.insertGradingComponent(GradingComponent(classId = mathClassId, parentId = groupWorkId, name = "Homework", weightPercentage = 40)).toInt()
        val projId = teacherDao.insertGradingComponent(GradingComponent(classId = mathClassId, parentId = groupWorkId, name = "Final Project", weightPercentage = 60)).toInt()

        // --- 5. Students and Scores ---
        seedDataForGroup(teacherDao, mathClassId, 10, 100, listOf(test1Id, test2Id, hw1Id, projId), 20.0)
    }

    /**
     * Generates a set of mock students and random grades for a specific class.
     * 
     * @param dao The DAO for database operations.
     * @param studentsClassId Target class identifier.
     * @param count Number of students to create.
     * @param processOffset Base offset for unique process numbers.
     * @param components List of grading component identifiers to populate.
     * @param maxScore Maximum score based on the class scale.
     */
    private suspend fun seedDataForGroup(
        dao: TeacherDao, 
        studentsClassId: Int, 
        count: Int, 
        processOffset: Int,
        components: List<Int>,
        maxScore: Double
    ) {
        for (i in 0 until count) {
            val studentId = dao.insertStudent(
                Student(
                    studentsClassId = studentsClassId, 
                    numberInGroup = i + 1,
                    name = "Student ${i + 1}", 
                    processNumber = "PROC-${2000 + processOffset + i}" 
                )
            ).toInt()
            
            dao.insertGrade(Grade(studentId = studentId, teacherNotes = "Example notes for Student ${i+1}"))

            components.forEach { compId ->
                val randomScore = ( (maxScore * 0.5 * 10).toInt()..(maxScore * 10).toInt() ).random() / 10.0
                dao.insertStudentScore(StudentScore(studentId = studentId, componentId = compId, score = randomScore))
            }
        }
    }
}
