package pt.ipt.dama.TeachersLog.data.model

/**
 * Data Transfer Objects (DTOs) specifically designed for UI consumption.
 * These models aggregate data from multiple entities to simplify screen rendering.
 * 
 * This specific DTO is for class list display with aggregated statistics.
 * 
 * @property id Unique identifier for the class.
 * @property subjectId ID of the parent subject.
 * @property name Name of the class (e.g., "10-A").
 * @property averageGrade Calculated weighted average of all students in the class.
 * @property studentCount Total number of students enrolled in the class.
 * @property gradingScale The grading system used (e.g., "5", "20", "100").
 * @property colorHex Theme color hex code inherited from the parent subject.
 * @property iconName Icon identifier inherited from the parent subject.
 */
data class StudentsClassWithStats(
    val id: Int,
    val subjectId: Int,
    val name: String,
    val averageGrade: Double,
    val studentCount: Int,
    val gradingScale: String,
    val colorHex: String,
    val iconName: String
)

/**
 * DTO for subject summary display.
 * 
 * @property id Unique identifier for the subject.
 * @property name Name of the subject (e.g., "Mathematics").
 * @property iconName Identifier for the UI icon.
 * @property colorHex Theme color hex code.
 * @property studentCount Total students across all classes within this subject.
 * @property averageGrade Aggregated percentage average for all classes in this subject.
 */
data class SubjectWithStats(
    val id: Int,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val studentCount: Int,
    val averageGrade: Double
)

/**
 * DTO for student component scores, including metadata about the grading criteria.
 * Supports hierarchical grouping by including parent information.
 * 
 * @property componentId ID of the grading component.
 * @property parentId Optional ID of the parent group.
 * @property componentName Name of the criteria (e.g., "Exam").
 * @property weight Percentage weight of this component (relative to parent or class).
 * @property absoluteWeight Calculated percentage of this component relative to the whole class.
 * @property score The numerical grade obtained by the student (only for leaf components).
 * @property isGroup Flag indicating if this is a container for other subcomponents.
 */
data class ComponentScore(
    val componentId: Int,
    val parentId: Int?,
    val componentName: String,
    val weight: Int,
    val absoluteWeight: Double,
    val score: Double,
    val isGroup: Boolean
)

/**
 * DTO for student roster with calculated weighted grades.
 * 
 * @property studentId Unique identifier for the student.
 * @property studentsClassId ID of the class the student belongs to.
 * @property numberInGroup Student's number in the class list.
 * @property name Student's full name.
 * @property processNumber Unique administrative process identifier.
 * @property profileImageUrl URI or URL to the student's profile photo.
 * @property weightedGrade The final calculated grade based on component weights.
 */
data class StudentWithGrade(
    val studentId: Int,
    val studentsClassId: Int,
    val numberInGroup: Int,
    val name: String,
    val processNumber: String?,
    val profileImageUrl: String?,
    val weightedGrade: Double
)

/**
 * DTO for top-level academic stats (e.g., for a specific semester or year).
 * 
 * @property totalStudents Sum of all students in the selected scope.
 * @property totalSubjects Count of subjects in the selected scope.
 * @property globalAverage Weighted percentage average across all subjects/students.
 */
data class GlobalStats(
    val totalStudents: Int,
    val totalSubjects: Int,
    val globalAverage: Double
)
