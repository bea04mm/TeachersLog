package pt.ipt.dama.teacherslog.data.repository

import androidx.room.withTransaction
import pt.ipt.dama.teacherslog.data.local.AppDatabase
import pt.ipt.dama.teacherslog.data.local.TeacherDao
import pt.ipt.dama.teacherslog.data.model.*

/**
 * CloningRepository handles the logic for duplicating academic data (Years, Periods, Subjects, Classes, Students).
 * It ensures that all structural data is preserved while operational data (grades) is reset.
 */
class CloningRepository(
    private val database: AppDatabase,
    private val teacherDao: TeacherDao
) {

    /**
     * Clones a specific class to a new subject.
     * Preserves students and grading criteria structure, but resets all scores to zero.
     */
    suspend fun cloneClass(sourceClassId: Int, targetSubjectId: Int, newClassName: String? = null) {
        database.withTransaction {
            val sourceClass = teacherDao.getStudentsClassByIdSync(sourceClassId)
                ?: return@withTransaction

            // 1. Create the new class with a potentially unique name
            val targetName = newClassName ?: sourceClass.name
            var attempt = 0
            var newClassId: Long = -1

            while (newClassId == -1L && attempt < 10) {
                val nameToTry = if (attempt == 0) targetName else "$targetName (${attempt + 1})"
                newClassId = teacherDao.insertStudentsClass(sourceClass.copy(
                    id = 0,
                    subjectId = targetSubjectId,
                    name = nameToTry,
                    lastAccessed = 0
                ))
                attempt++
            }
            
            if (newClassId == -1L) return@withTransaction // Failed to create class after 10 attempts

            // 2. Clone Grading Components (Criteria)
            val components = teacherDao.getGradingComponentsByClassId(sourceClassId)
            val idMap = mutableMapOf<Int, Int>() // Old ID -> New ID

            // Sort by parentId to ensure parents are created before children
            // Root components first (parentId == null)
            val sortedComponents = components.sortedBy { it.parentId != null }
            
            sortedComponents.forEach { comp ->
                val newCompId = teacherDao.insertGradingComponent(comp.copy(
                    id = 0,
                    classId = newClassId.toInt(),
                    parentId = comp.parentId?.let { idMap[it] }
                )).toInt()
                idMap[comp.id] = newCompId
            }

            // 3. Clone Students
            val students = teacherDao.getStudentsByClassId(sourceClassId)
            students.forEach { student ->
                val newStudentId = teacherDao.insertStudent(student.copy(
                    studentId = 0,
                    studentsClassId = newClassId.toInt()
                )).toInt()
                
                // Initialize a Grade entry for the student (notes and tags reset)
                teacherDao.insertGrade(Grade(studentId = newStudentId))
            }
        }
    }

    /**
     * Clones an entire subject and all its classes to a new semester.
     */
    suspend fun cloneSubject(sourceSubjectId: Int, targetSemesterId: Int, newSubjectName: String? = null) {
        database.withTransaction {
            val sourceSubject = teacherDao.getSubjectByIdSync(sourceSubjectId)
                ?: return@withTransaction

            // 1. Create the new subject with a unique name if needed
            val targetName = newSubjectName ?: sourceSubject.name
            var attempt = 0
            var newSubjectId: Long = -1

            while (newSubjectId == -1L && attempt < 10) {
                val nameToTry = if (attempt == 0) targetName else "$targetName (${attempt + 1})"
                newSubjectId = teacherDao.insertSubject(sourceSubject.copy(
                    id = 0,
                    semesterId = targetSemesterId,
                    name = nameToTry
                ))
                attempt++
            }

            if (newSubjectId == -1L) return@withTransaction

            // 2. Clone all classes within this subject
            val classes = teacherDao.getClassesBySubjectId(sourceSubjectId)
            classes.forEach { clazz ->
                cloneClass(clazz.id, newSubjectId.toInt())
            }
        }
    }

    /**
     * Clones an entire academic period (Semester) to a new year.
     */
    suspend fun cloneSemester(sourceSemesterId: Int, targetYearId: Int, newSemesterName: String? = null) {
        database.withTransaction {
            val sourceSemester = teacherDao.getSemesterByIdSync(sourceSemesterId)
                ?: return@withTransaction

            val targetName = newSemesterName ?: sourceSemester.name
            var attempt = 0
            var newSemesterId: Long = -1

            while (newSemesterId == -1L && attempt < 10) {
                val nameToTry = if (attempt == 0) targetName else "$targetName (${attempt + 1})"
                newSemesterId = teacherDao.insertSemester(sourceSemester.copy(
                    id = 0,
                    academicYearId = targetYearId,
                    name = nameToTry
                ))
                attempt++
            }

            if (newSemesterId == -1L) return@withTransaction

            val subjects = teacherDao.getSubjectsBySemesterId(sourceSemesterId)
            subjects.forEach { subject ->
                cloneSubject(subject.id, newSemesterId.toInt())
            }
        }
    }

    /**
     * Clones an entire academic year and all its semesters to a new year.
     */
    suspend fun cloneAcademicYear(sourceYearId: Int, newYearName: String) {
        database.withTransaction {
            var attempt = 0
            var newYearId: Long = -1

            while (newYearId == -1L && attempt < 10) {
                val nameToTry = if (attempt == 0) newYearName else "$newYearName (${attempt + 1})"
                newYearId = teacherDao.insertAcademicYear(AcademicYear(
                    year = nameToTry,
                    isCurrent = false
                ))
                attempt++
            }

            if (newYearId == -1L) return@withTransaction

            val semesters = teacherDao.getSemestersForYearList(sourceYearId)
            semesters.forEach { semester ->
                cloneSemester(semester.id, newYearId.toInt())
            }
        }
    }

    /**
     * Clones a single student to another class.
     * Resets their grades and notes in the new context.
     */
    suspend fun cloneStudent(studentId: Int, targetClassId: Int) {
        database.withTransaction {
            val sourceStudent = teacherDao.getStudentById(studentId)
                ?: return@withTransaction

            // Find next available number in target class
            val existingStudents = teacherDao.getStudentsByClassId(targetClassId)
            val nextNumber = (existingStudents.maxOfOrNull { it.numberInGroup } ?: 0) + 1

            val newStudentId = teacherDao.insertStudent(sourceStudent.copy(
                studentId = 0,
                studentsClassId = targetClassId,
                numberInGroup = nextNumber,
                processNumber = null // Reset process number to avoid global unique constraint conflict
            )).toInt()

            // Initialize empty grade for the new context
            teacherDao.insertGrade(Grade(studentId = newStudentId))
        }
    }
}
