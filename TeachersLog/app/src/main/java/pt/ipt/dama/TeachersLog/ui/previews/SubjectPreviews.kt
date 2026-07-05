package pt.ipt.dama.TeachersLog.ui.previews

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.MutableLiveData
import pt.ipt.dama.TeachersLog.data.model.AcademicYear
import pt.ipt.dama.TeachersLog.data.model.StudentsClassWithStats
import pt.ipt.dama.TeachersLog.data.model.SubjectWithStats
import pt.ipt.dama.TeachersLog.ui.components.LocalSnackbarHostState
import pt.ipt.dama.TeachersLog.ui.screens.SubjectContent
import pt.ipt.dama.TeachersLog.ui.screens.SubjectEditContent
import pt.ipt.dama.TeachersLog.ui.screens.SubjectsListContent
import pt.ipt.dama.TeachersLog.ui.theme.TeachersLogTheme

/**
 * Preview for the [SubjectsListContent] UI in light mode.
 */
@Preview(showBackground = true, name = "Subjects List Light Mode")
@Composable
fun SubjectsListPreview() {
    TeachersLogTheme(darkTheme = false) {
        val snackbarHostState = remember { SnackbarHostState() }
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            SubjectsListContent(
                subjects = listOf(
                    SubjectWithStats(1, "Mathematics", "functions", "#004AC6", 12, 4.2),
                    SubjectWithStats(2, "Natural Sciences", "biotech", "#006686", 8, 3.8)
                ),
                isRefreshing = false,
                isGuest = false,
                syncEnabled = true,
                onRefresh = {},
                onMenuClick = {},
                onSubjectClick = { _, _ -> },
                onAddSubjectClick = {},
                onEditSubjectClick = {},
                onDeleteSubjectClick = {},
                onCloneSubjectClick = { _, _ -> },
                academicYears = listOf(AcademicYear(1, "2024/2025")),
                semestersProvider = { _ -> MutableLiveData(emptyList()) },
                currentSemesterName = null
            )
        }
    }
}

/**
 * Visualizes the class list in light mode.
 */
@Preview(showBackground = true, name = "Subject Classes Light Mode")
@Composable
fun SubjectPreview() {
    TeachersLogTheme(darkTheme = false) {
        val snackbarHostState = remember { SnackbarHostState() }
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            SubjectContent(
                subjectName = "Mathematics",
                subjectIcon = "functions",
                classes = listOf(
                    StudentsClassWithStats(1, 1, "10.ºA", 85.0, 25, "100", "#004AC6", "functions"),
                    StudentsClassWithStats(2, 1, "11.ºB", 4.2, 22, "5", "#004AC6", "functions")
                ),
                isRefreshing = false,
                isGuest = false,
                syncEnabled = true,
                onRefresh = {},
                onClassClick = { _, _ -> },
                onAddClassClick = {},
                onEditClassClick = {},
                onDeleteClass = {},
                onCloneClassClick = { _, _ -> },
                onBackClick = {},
                academicYears = listOf(AcademicYear(1, "2024/2025")),
                semestersProvider = { _ -> MutableLiveData(emptyList()) },
                subjectsProvider = { _ -> MutableLiveData(emptyList()) },
                currentSemesterName = null
            )
        }
    }
}

/**
 * Visualizes the subject editing screen in light mode.
 */
@Preview(showBackground = true, name = "Add Subject Light")
@Composable
fun AddSubjectPreview() {
    TeachersLogTheme(darkTheme = false) {
        val snackbarHostState = remember { SnackbarHostState() }
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
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
}

/**
 * Visualizes the subject editing screen in dark mode.
 */
@Preview(showBackground = true, name = "Subject Edit Dark")
@Composable
fun AddSubjectDarkPreview() {
    TeachersLogTheme(darkTheme = true) {
        val snackbarHostState = remember { SnackbarHostState() }
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
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
}
