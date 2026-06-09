package pt.ipt.dama.teacherslog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Represents a full school cycle (e.g., "2024/2025").
 * Serves as the top-level filter for all academic data.
 */
@Entity(
    tableName = "academic_years",
    indices = [Index(value = ["year"], unique = true)]
)
data class AcademicYear(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: String, 
    val isCurrent: Boolean = false
)

/**
 * Represents a school subject (e.g., "Mathematics").
 * Linked to a specific academic year.
 */
@Entity(
    tableName = "subjects",
    indices = [Index(value = ["name", "academicYearId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = AcademicYear::class,
            parentColumns = ["id"],
            childColumns = ["academicYearId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val academicYearId: Int,
    val name: String,
    val iconName: String,
    val colorHex: String
)

/**
 * Represents a specific class of students (StudentsClass) within a Subject.
 * Named 'StudentsClass' to avoid conflict with java.lang.Class.
 */
@Entity(
    tableName = "students_classes",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StudentsClass(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val name: String
)

/**
 * Individual student record.
 */
@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = StudentsClass::class,
            parentColumns = ["id"],
            childColumns = ["studentsClassId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Student(
    @PrimaryKey(autoGenerate = true) val studentId: Int = 0,
    val studentsClassId: Int,
    val numberInGroup: Int,
    val name: String,
    val processNumber: String? = null,
    val profileImageUrl: String? = null
)

/**
 * Performance metrics for a student.
 */
@Entity(
    tableName = "grades",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Grade(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val homework: Double = 0.0,
    val participation: Int = 0,
    val quizzes: Double = 0.0,
    val projects: Double = 0.0,
    val teacherNotes: String = "",
    val tags: String = ""
)

// --- Data Transfer Objects (DTOs) for UI ---

data class StudentsClassWithStats(
    val id: Int,
    val subjectId: Int,
    val name: String,
    val averageGrade: Double,
    val studentCount: Int
)

data class SubjectWithStats(
    val id: Int,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val studentCount: Int,
    val averageGrade: Double
)

data class StudentWithGrade(
    val studentId: Int,
    val studentsClassId: Int,
    val numberInGroup: Int,
    val name: String,
    val processNumber: String?,
    val weightedGrade: Double
)

data class GlobalStats(
    val totalStudents: Int,
    val totalSubjects: Int,
    val globalAverage: Double
)
