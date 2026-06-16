package pt.ipt.dama.teacherslog.ui.previews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import pt.ipt.dama.teacherslog.data.model.ComponentScore
import pt.ipt.dama.teacherslog.data.model.Student
import pt.ipt.dama.teacherslog.ui.screens.GradeSliderCard
import pt.ipt.dama.teacherslog.ui.screens.StudentEditContent
import pt.ipt.dama.teacherslog.ui.screens.StudentGradingContent
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme

/**
 * Preview of the student editing screen in light mode.
 */
@Preview(showBackground = true, name = "Add Student Preview")
@Composable
fun StudentEditPreview() {
    TeachersLogTheme {
        StudentEditContent(
            isEditMode = false, 
            initialName = "", 
            initialNumber = 1, 
            initialProcess = "", 
            initialPhoto = "", 
            availableNumbers = listOf(1, 2, 3), 
            onSave = { _, _, _, _ -> }, 
            onBackClick = {}, 
            checkProcessTaken = { false }
        )
    }
}

/**
 * Visualizes the full Student Grading Screen using the stateless Content component.
 * This avoids rendering issues related to ViewModel initialization in previews.
 */
@Preview(showBackground = true, name = "Student Grading Screen Preview")
@Composable
fun FullStudentGradingPreview() {
    TeachersLogTheme {
        StudentGradingContent(
            student = Student(1, 1, 1, "Alice Johnson", "P001", null),
            className = "10.ºA",
            componentScores = listOf(
                ComponentScore(1, null, "Tests", 60, 60.0, 4.5, true),
                ComponentScore(2, 1, "1st Test", 50, 30.0, 4.0, false),
                ComponentScore(3, 1, "2nd Test", 50, 30.0, 5.0, false),
                ComponentScore(4, null, "Assignments", 40, 40.0, 3.8, false)
            ),
            gradingScale = "5",
            teacherNotes = "Good participation in class discussions.",
            onScoreChange = { _, _ -> },
            onNotesChange = {},
            onSave = {},
            onBackClick = {}
        )
    }
}

/**
 * Visualizes an individual grading slider card.
 */
@Preview(showBackground = true, name = "Grade Slider Card Preview")
@Composable
fun GradeSliderCardPreview() {
    TeachersLogTheme {
        GradeSliderCard(
            title = "Component", 
            weight = "50%", 
            icon = Icons.Default.Quiz, 
            value = 4.0f, 
            maxGrade = 5f, 
            scaleSuffix = " / 5.0", 
            onValueChange = {}, 
            rangeLabels = listOf("Low", "High")
        )
    }
}
