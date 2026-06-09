package pt.ipt.dama.teacherslog.data.local

import kotlinx.coroutines.flow.first
import pt.ipt.dama.teacherslog.data.model.*

object DatabaseSeeder {
    suspend fun seed(teacherDao: TeacherDao) {
        val existingYears = teacherDao.getAllAcademicYears().first()
        if (existingYears.isNotEmpty()) return

        val year2324 = AcademicYear(year = "2023/2024", isCurrent = false)
        val year2425 = AcademicYear(year = "2024/2025", isCurrent = true)
        val year1Id = teacherDao.insertAcademicYear(year2324).toInt()
        val year2Id = teacherDao.insertAcademicYear(year2425).toInt()

        val sub1Id = teacherDao.insertSubject(Subject(academicYearId = year2Id, name = "Mathematics", iconName = "functions", colorHex = "#004AC6")).toInt()
        val sub2Id = teacherDao.insertSubject(Subject(academicYearId = year2Id, name = "Science", iconName = "biotech", colorHex = "#006686")).toInt()
        val sub3Id = teacherDao.insertSubject(Subject(academicYearId = year2Id, name = "History", iconName = "history_edu", colorHex = "#4E565B")).toInt()
        val sub4Id = teacherDao.insertSubject(Subject(academicYearId = year1Id, name = "Mathematics", iconName = "functions", colorHex = "#004AC6")).toInt()

        val grp1Id = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub1Id, name = "4ºA")).toInt()
        val grp2Id = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub1Id, name = "5ºB")).toInt()
        val grp3Id = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub2Id, name = "3ºA")).toInt()
        val grp4Id = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub4Id, name = "6ºC")).toInt()
        val grp5Id = teacherDao.insertStudentsClass(StudentsClass(subjectId = sub3Id, name = "6ºC")).toInt()
        
        seedDataForGroup(teacherDao, grp1Id, 12)
        seedDataForGroup(teacherDao, grp2Id, 10)
        seedDataForGroup(teacherDao, grp3Id, 8)
        seedDataForGroup(teacherDao, grp4Id, 15)
        seedDataForGroup(teacherDao, grp5Id, 5)
    }

    private suspend fun seedDataForGroup(dao: TeacherDao, studentsClassId: Int, count: Int) {
        val names = listOf(
            "Leo Bennett", "Emma Wilson", "Marcus Chen", "Sophia Rodriguez",
            "Lucas Silva", "Ana Martins", "João Pereira", "Bia Santos",
            "David Oliveira", "Clara Costa", "Hugo Moreira", "Inês Rocha"
        )
        
        for (i in 0 until count) {
            val name = if (i < names.size) names[i] else "Student ${i + 1}"
            val studentId = dao.insertStudent(
                Student(
                    studentsClassId = studentsClassId, 
                    numberInGroup = i + 1,
                    name = name, 
                    processNumber = if (i % 2 == 0) "PROC-${2000 + i}" else null 
                )
            ).toInt()
            
            dao.insertGrade(
                Grade(
                    studentId = studentId,
                    homework = (3..5).random().toDouble() + (0..9).random() * 0.1,
                    participation = (3..5).random(),
                    quizzes = (2..5).random().toDouble() + (0..9).random() * 0.1,
                    projects = (4..5).random().toDouble() + (0..9).random() * 0.1
                )
            )
        }
    }
}
