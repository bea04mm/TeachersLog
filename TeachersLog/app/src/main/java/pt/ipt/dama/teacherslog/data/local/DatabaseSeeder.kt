package pt.ipt.dama.teacherslog.data.local

import kotlinx.coroutines.flow.first
import pt.ipt.dama.teacherslog.data.model.*

/**
 * DatabaseSeeder is a utility object responsible for populating the Room database
 * with initial mock data when the application is first launched or when the
 * database is empty.
 * 
 * It creates a hierarchy of data:
 * Academic Years -> Subjects -> Classes -> Students -> Grades.
 */
object DatabaseSeeder {

    /**
     * Seeds the database with initial academic data if no data exists.
     * 
     * @param teacherDao The Data Access Object used to perform insertions.
     */
    suspend fun seed(teacherDao: TeacherDao) {
        // Check if there are already academic years registered
        val existingYears = teacherDao.getAllAcademicYears().first()
        // If the database is not empty, do not seed again (idempotency)
        if (existingYears.isNotEmpty()) return

        // --- 1. Academic Years ---
        val year2324 = AcademicYear(year = "2023/2024", isCurrent = false)
        val year2425 = AcademicYear(year = "2024/2025", isCurrent = true)
        val year1Id = teacherDao.insertAcademicYear(year2324).toInt()
        val year2Id = teacherDao.insertAcademicYear(year2425).toInt()

        // --- 2. Subjects ---
        val sub1Id = teacherDao.insertSubject(Subject(academicYearId = year2Id, name = "Mathematics", iconName = "functions", colorHex = "#004AC6")).toInt()
        val sub2Id = teacherDao.insertSubject(Subject(academicYearId = year2Id, name = "Science", iconName = "biotech", colorHex = "#006686")).toInt()
        val sub3Id = teacherDao.insertSubject(Subject(academicYearId = year2Id, name = "History", iconName = "history_edu", colorHex = "#4E565B")).toInt()
        val sub4Id = teacherDao.insertSubject(Subject(academicYearId = year1Id, name = "Mathematics", iconName = "functions", colorHex = "#004AC6")).toInt()

        // --- 3. Students Classes (Groups) ---
        val grp1Id = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub1Id, name = "4.ºA")).toInt()
        val grp2Id = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub1Id, name = "5.ºB")).toInt()
        val grp3Id = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub2Id, name = "3.ºA")).toInt()
        val grp4Id = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub4Id, name = "6.ºC")).toInt()
        val grp5Id = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub3Id, name = "6.ºC")).toInt()
        
        // --- 4. Students and Grades (via helper) ---
        seedDataForGroup(teacherDao, grp1Id, 12)
        seedDataForGroup(teacherDao, grp2Id, 10)
        seedDataForGroup(teacherDao, grp3Id, 8)
        seedDataForGroup(teacherDao, grp4Id, 15)
        seedDataForGroup(teacherDao, grp5Id, 5)
    }

    /**
     * Generates a specified number of students and random grades for a given class.
     * 
     * @param dao The [TeacherDao] for database operations.
     * @param studentsClassId The ID of the class to populate.
     * @param count The number of student records to create.
     */
    private suspend fun seedDataForGroup(dao: TeacherDao, studentsClassId: Int, count: Int) {
        val names = listOf(
            "Leo Bennett", "Emma Wilson", "Marcus Chen", "Sophia Rodriguez",
            "Lucas Silva", "Ana Martins", "João Pereira", "Bia Santos",
            "David Oliveira", "Clara Costa", "Hugo Moreira", "Inês Rocha"
        )
        
        for (i in 0 until count) {
            val name = if (i < names.size) names[i] else "Student ${i + 1}"
            // Insert student basic info
            val studentId = dao.insertStudent(
                Student(
                    studentsClassId = studentsClassId, 
                    numberInGroup = i + 1,
                    name = name, 
                    processNumber = if (i % 2 == 0) "PROC-${2000 + i}" else null 
                )
            ).toInt()
            
            // Calculate random grades and manually ensure they never exceed 5.0
            val randomHomework = (30..50).random() / 10.0
            val randomQuizzes = (20..50).random() / 10.0
            val randomProjects = (40..50).random() / 10.0
            val randomParticipation = (3..5).random()

            // Insert a mock grade record with strictly validated values
            dao.insertGrade(
                Grade(
                    studentId = studentId,
                    homework = if (randomHomework > 5.0) 5.0 else randomHomework,
                    participation = randomParticipation,
                    quizzes = if (randomQuizzes > 5.0) 5.0 else randomQuizzes,
                    projects = if (randomProjects > 5.0) 5.0 else randomProjects
                )
            )
        }
    }
}
