package pt.ipt.dama.teacherslog.ui.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.MutableLiveData
import pt.ipt.dama.teacherslog.data.model.AcademicYear
import pt.ipt.dama.teacherslog.data.model.StudentsClassWithStats
import pt.ipt.dama.teacherslog.data.model.SubjectWithStats
import pt.ipt.dama.teacherslog.ui.screens.SubjectContent
import pt.ipt.dama.teacherslog.ui.screens.SubjectEditContent
import pt.ipt.dama.teacherslog.ui.screens.SubjectsListContent
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme

/**
 * Preview for the [SubjectsListContent] UI in light mode.
 */
@Preview(showBackground = true, name = "Subjects List Light Mode")
@Composable
fun SubjectsListPreview() {
    TeachersLogTheme(darkTheme = false) {
        SubjectsListContent(
            subjects = listOf(
                SubjectWithStats(1, "Mathematics", "functions", "#004AC6", 12, 4.2),
                SubjectWithStats(2, "Natural Sciences", "biotech", "#006686", 8, 3.8)
            ),
            onMenuClick = {},
            onSubjectClick = { _, _ -> },
            onAddSubjectClick = {},
            onEditSubjectClick = {},
            onDeleteSubjectClick = {},
            onCloneSubjectClick = { _, _ -> },
            academicYears = listOf(AcademicYear(1, "2024/2025")),
            semestersProvider = { _ -> MutableLiveData(emptyList()) }
        )
    }
}

/**
 * Visualizes the class list in light mode.
 */
@Preview(showBackground = true, name = "Subject Classes Light Mode")
@Composable
fun SubjectPreview() {
    TeachersLogTheme(darkTheme = false) {
        SubjectContent(
            subjectName = "Mathematics",
            subjectIcon = "functions",
            classes = listOf(
                StudentsClassWithStats(1, 1, "10.ºA", 85.0, 25, "100", "#004AC6", "functions"),
                StudentsClassWithStats(2, 1, "11.ºB", 4.2, 22, "5", "#004AC6", "functions")
            ),
            onClassClick = { _, _ -> },
            onAddClassClick = {},
            onEditClassClick = {},
            onDeleteClass = {},
            onCloneClassClick = { _, _ -> },
            onBackClick = {},
            academicYears = listOf(AcademicYear(1, "2024/2025")),
            semestersProvider = { _ -> MutableLiveData(emptyList()) },
            subjectsProvider = { _ -> MutableLiveData(emptyList()) }
        )
    }
}

/**
 * Visualizes the subject editing screen in light mode.
 */
@Preview(showBackground = true, name = "Add Subject Light")
@Composable
fun AddSubjectPreview() {
    TeachersLogTheme(darkTheme = false) { 
        SubjectEditContent(
            isEditMode = false, 
            initialName = "", 
            initialIcon = "functions", 
            initialColor = "#004AC6", 
            onSave = { _, _, _ -> }, 
            onBackClick = {}
        ) 
    }
}

/**
 * Visualizes the subject editing screen in dark mode.
 */
@Preview(showBackground = true, name = "Subject Edit Dark")
@Composable
fun AddSubjectDarkPreview() {
    TeachersLogTheme(darkTheme = true) { 
        SubjectEditContent(
            isEditMode = true, 
            initialName = "Mathematics", 
            initialIcon = "calculate", 
            initialColor = "#B3261E", 
            onSave = { _, _, _ -> }, 
            onBackClick = {}
        ) 
    }
}
