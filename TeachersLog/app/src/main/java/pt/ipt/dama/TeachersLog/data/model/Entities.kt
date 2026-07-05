package pt.ipt.dama.TeachersLog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Represents a full school cycle (e.g., "2024/2025").
 * 
 * @property id Unique identifier for the academic year.
 * @property year String representation of the year range.
 * @property isCurrent Flag indicating if this is the active year.
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
 * Represents a semester within an academic year (e.g., "1st Semester").
 * 
 * @property id Unique identifier for the semester.
 * @property academicYearId ID of the parent academic year.
 * @property name Descriptive name of the semester.
 */
@Entity(
    tableName = "semesters",
    indices = [Index(value = ["academicYearId"])],
    foreignKeys = [
        ForeignKey(
            entity = AcademicYear::class,
            parentColumns = ["id"],
            childColumns = ["academicYearId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Semester(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val academicYearId: Int,
    val name: String // e.g., "1st Semester", "2nd Semester"
)

/**
 * Represents a school subject (e.g., "Mathematics").
 * 
 * @property id Unique identifier for the subject.
 * @property semesterId ID of the parent semester.
 * @property name Name of the subject.
 * @property iconName Identifier for the UI icon.
 * @property colorHex Theme color hex code.
 */
@Entity(
    tableName = "subjects",
    indices = [
        Index(value = ["name", "semesterId"], unique = true),
        Index(value = ["semesterId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val semesterId: Int,
    val name: String,
    val iconName: String,
    val colorHex: String
)

/**
 * Represents a specific class of students (e.g., "10-A").
 * 
 * @property id Unique identifier for the class.
 * @property subjectId ID of the parent subject.
 * @property name Name or designation of the class.
 * @property gradingScale The grading system used (e.g., "5", "20", "100").
 * @property lastAccessed Timestamp (ms) of the last user interaction with this class.
 */
@Entity(
    tableName = "students_classes",
    indices = [Index(value = ["subjectId"])],
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
    val name: String,
    val gradingScale: String = "5",
    val lastAccessed: Long = 0
)

/**
 * Represents a grading component for a class.
 * Components can be standalone or nested within a parent component (group).
 * 
 * @property id Unique identifier for the component.
 * @property classId ID of the parent students class.
 * @property parentId Optional ID of the parent component (for sub-items).
 * @property name Name of the grading criterion.
 * @property weightPercentage Weight of this component relative to its siblings.
 */
@Entity(
    tableName = "grading_components",
    indices = [
        Index(value = ["classId"]),
        Index(value = ["parentId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = StudentsClass::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GradingComponent::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GradingComponent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: Int,
    val parentId: Int? = null,
    val name: String,
    val weightPercentage: Int
)

/**
 * Individual student record.
 * 
 * @property studentId Unique identifier for the student.
 * @property studentsClassId ID of the class the student belongs to.
 * @property numberInGroup Student's number in the class list.
 * @property name Student's full name.
 * @property processNumber Unique administrative process identifier.
 * @property profileImageUrl URI or URL to the student's profile photo.
 */
@Entity(
    tableName = "students",
    indices = [
        Index(value = ["studentsClassId", "numberInGroup"], unique = true),
        Index(value = ["processNumber"], unique = true),
        Index(value = ["studentsClassId"])
    ],
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
 * Stores the actual score of a student for a leaf grading component.
 * 
 * @property studentId ID of the student.
 * @property componentId ID of the specific leaf grading component.
 * @property score The numerical grade obtained.
 */
@Entity(
    tableName = "student_scores",
    primaryKeys = ["studentId", "componentId"],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["componentId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GradingComponent::class,
            parentColumns = ["id"],
            childColumns = ["componentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StudentScore(
    val studentId: Int,
    val componentId: Int,
    val score: Double
)

/**
 * Qualitative data and notes for a student.
 * 
 * @property id Unique identifier for the record.
 * @property studentId ID of the student.
 * @property teacherNotes Qualitative observations from the teacher.
 * @property tags Comma-separated labels for the student.
 */
@Entity(
    tableName = "grades",
    indices = [Index(value = ["studentId"])],
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
    val teacherNotes: String = "",
    val tags: String = ""
)

/**
 * Stores the application user's profile information.
 * 
 * @property id Primary key (singleton, usually 1).
 * @property name User's display name.
 * @property email User's email address.
 * @property photoUrl Cloud URL or local URI of the profile photo.
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val email: String,
    val photoUrl: String? = null
)
