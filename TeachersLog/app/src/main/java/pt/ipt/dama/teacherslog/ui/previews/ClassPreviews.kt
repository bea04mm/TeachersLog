package pt.ipt.dama.teacherslog.ui.previews

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import pt.ipt.dama.teacherslog.data.model.AcademicYear
import pt.ipt.dama.teacherslog.data.model.StudentWithGrade
import pt.ipt.dama.teacherslog.ui.screens.ClassEditContent
import pt.ipt.dama.teacherslog.ui.screens.ClassRosterContent
import pt.ipt.dama.teacherslog.ui.screens.RandomStudentPopupContent
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme

/**
 * Preview of the student roster in light mode.
 */
@Preview(showBackground = true, name = "Roster Light Mode")
@Composable
fun ClassRosterPreview() {
    TeachersLogTheme(darkTheme = false) {
        ClassRosterContent(
            className = "10.ºA",
            classIcon = "functions",
            students = listOf(
                StudentWithGrade(1, 1, 1, "Alice Johnson", "P001", null, 4.5),
                StudentWithGrade(2, 1, 2, "Bob Smith", "P002", null, 3.2)
            ),
            gradingScale = "5",
            onStudentClick = { _, _ -> },
            onAddStudentClick = {},
            onEditStudentClick = {},
            onDeleteStudent = {},
            onCloneStudentClick = { _, _ -> },
            onBackClick = {},
            academicYears = listOf(AcademicYear(1, "2024/2025")),
            semestersProvider = { _ -> MutableLiveData(emptyList()) },
            subjectsProvider = { _ -> MutableLiveData(emptyList()) },
            classesProvider = { _ -> MutableLiveData(emptyList()) }
        )
    }
}

/**
 * Visualizes the content of the random student selection popup.
 */
@Preview(showBackground = true, name = "Random Selection Popup Preview")
@Composable
fun RandomStudentPopupPreview() {
    TeachersLogTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            RandomStudentPopupContent(
                title = "Random Selection",
                student = StudentWithGrade(1, 1, 7, "Alice Johnson", "P001", null, 4.5),
                onGoToGrading = {},
                onClose = {}
            )
        }
    }
}

/**
 * Preview of the class editing screen in light mode.
 */
@Preview(showBackground = true, name = "Add Class Preview")
@Composable
fun ClassEditPreview() {
    TeachersLogTheme {
        ClassEditContent(
            isEditMode = false, 
            initialName = "", 
            initialScale = "5", 
            initialComponents = emptyList(), 
            onSave = { _, _, _, _ -> }, 
            onBackClick = {}
        )
    }
}
