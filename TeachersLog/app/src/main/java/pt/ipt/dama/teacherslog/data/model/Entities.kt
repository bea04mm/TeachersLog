package pt.ipt.dama.teacherslog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Represents a full school cycle (e.g., "2024/2025").
 * Serves as the top-level filter for all academic data in the application.
 * 
 * @property id Unique identifier for the academic year (auto-generated).
 * @property year The string representation of the year (e.g., "2024/2025").
 * @property isCurrent Flag indicating if this is the currently active year.
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
 * Represents a school subject (e.g., "Mathematics", "Science").
 * Each subject is linked to a specific [AcademicYear].
 * 
 * @property id Unique identifier for the subject (auto-generated).
 * @property academicYearId Foreign key linking to [AcademicYear].
 * @property name The name of the subject.
 * @property iconName The identifier for the icon to be displayed in the UI.
 * @property colorHex The hex code for the subject's theme color.
 * @property gradingScale The grading scale used for this subject (e.g., "5", "20", "100").
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
    val colorHex: String,
    val gradingScale: String = "5"
)

/**
 * Represents a specific class of students (StudentsClass) within a [Subject].
 * Named 'StudentsClass' to avoid conflict with java.lang.Class.
 * 
 * @property id Unique identifier for the class (auto-generated).
 * @property subjectId Foreign key linking to the parent [Subject].
 * @property name The name of the class (e.g., "10-A").
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
 * A student belongs to a specific [StudentsClass].
 * 
 * @property studentId Unique identifier for the student (auto-generated).
 * @property studentsClassId Foreign key linking to [StudentsClass].
 * @property numberInGroup The student's number within their class roster.
 * @property name The full name of the student.
 * @property processNumber Optional administrative process number.
 * @property profileImageUrl Optional URL or path to the student's profile image.
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
 * Performance metrics and qualitative data for a [Student].
 * Links directly to a [Student] record.
 * 
 * @property id Unique identifier for the grade record (auto-generated).
 * @property studentId Foreign key linking to the [Student].
 * @property homework Grade for homework and assignments.
 * @property participation Score for classroom participation.
 * @property quizzes Grade for tests and quizzes.
 * @property projects Grade for collaborative projects.
 * @property teacherNotes Qualitative feedback or observations from the teacher.
 * @property tags Categorization tags for the student's performance.
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

/**
 * DTO combining class information with calculated statistics for UI display.
 * 
 * @property id The class ID.
 * @property subjectId The parent subject ID.
 * @property name The class name.
 * @property averageGrade The calculated average grade of all students in this class.
 * @property studentCount The total number of students enrolled in this class.
 * @property gradingScale The scale identifier (e.g., "5", "20", "100").
 */
data class StudentsClassWithStats(
    val id: Int,
    val subjectId: Int,
    val name: String,
    val averageGrade: Double,
    val studentCount: Int,
    val gradingScale: String
)

/**
 * DTO combining subject information with aggregated statistics for UI display.
 * 
 * @property id The subject ID.
 * @property name The subject name.
 * @property iconName The display icon identifier.
 * @property colorHex The display theme color.
 * @property studentCount Total students across all classes of this subject.
 * @property averageGrade Aggregated average grade for the subject.
 * @property gradingScale The scale identifier (e.g., "5", "20", "100").
 */
data class SubjectWithStats(
    val id: Int,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val studentCount: Int,
    val averageGrade: Double,
    val gradingScale: String
)

/**
 * DTO for displaying a student within a roster, including their weighted average grade.
 * 
 * @property studentId The student's ID.
 * @property studentsClassId The ID of the class the student belongs to.
 * @property numberInGroup Roster number.
 * @property name Student's name.
 * @property processNumber Admin process number.
 * @property weightedGrade The student's final calculated grade based on weights.
 */
data class StudentWithGrade(
    val studentId: Int,
    val studentsClassId: Int,
    val numberInGroup: Int,
    val name: String,
    val processNumber: String?,
    val weightedGrade: Double
)

/**
 * DTO representing global academic metrics for an [AcademicYear].
 * 
 * @property totalStudents Count of all students in the year.
 * @property totalSubjects Count of all subjects in the year.
 * @property globalAverage The overall average grade across all subjects and students.
 */
data class GlobalStats(
    val totalStudents: Int,
    val totalSubjects: Int,
    val globalAverage: Double
)
