package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.StudentsClassWithStats
import pt.ipt.dama.teacherslog.ui.components.ClassCard
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel

/**
 * SubjectContent is the stateless UI component that displays the classes within a specific subject.
 *
 * It manages:
 * 1. The display of the subject name as a header.
 * 2. A scrollable list of classes using [LazyColumn].
 * 3. Individual class cards showing student count and average grade.
 *
 * @param subjectName The name of the subject being displayed.
 * @param classes The list of [StudentsClassWithStats] associated with this subject.
 * @param onClassClick Callback when a class card is clicked.
 * @param onBackClick Callback for the back button navigation.
 */
@Composable
fun SubjectContent(
    subjectName: String,
    classes: List<StudentsClassWithStats>,
    onClassClick: (Int, String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            // Standard top bar with the subject name as title and a back button
            TeacherTopBar(
                title = subjectName,
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Header Section ---
            item {
                Text(
                    text = stringResource(R.string.class_header),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- Classes List ---
            items(classes) { clazz ->
                ClassCard(
                    name = clazz.name,
                    studentCount = clazz.studentCount,
                    averageGrade = clazz.averageGrade,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onClassClick(clazz.id, clazz.name) }
                )
            }
        }
    }
}

/**
 * SubjectScreen is the stateful Composable that connects the [TeacherViewModel]
 * to the [SubjectContent] UI.
 *
 * @param viewModel The shared [TeacherViewModel] providing class data.
 * @param subjectId The ID of the subject whose classes should be fetched.
 * @param subjectName The name of the subject to display.
 * @param onClassClick Callback when a class is selected.
 * @param onBackClick Callback for navigating back to the previous screen.
 */
@Composable
fun SubjectScreen(
    viewModel: TeacherViewModel,
    subjectId: Int,
    subjectName: String,
    onClassClick: (Int, String) -> Unit,
    onBackClick: () -> Unit
) {
    // Observe the list of classes for the specific subject from the ViewModel
    val classes by viewModel.getStudentsClassesForSubject(subjectId).observeAsState(emptyList())
    
    SubjectContent(
        subjectName = subjectName,
        classes = classes,
        onClassClick = onClassClick,
        onBackClick = onBackClick
    )
}

/**
 * Preview for the [SubjectContent] UI with sample class data in light mode.
 */
@Preview(showBackground = true, name = "Subject Detail Light Mode")
@Composable
fun SubjectPreview() {
    TeachersLogTheme(darkTheme = false) {
        SubjectContent(
            subjectName = "Mathematics",
            classes = listOf(
                StudentsClassWithStats(1, 1, "10.ºA", 4.2, 25),
                StudentsClassWithStats(2, 1, "10.ºB", 3.8, 22),
                StudentsClassWithStats(3, 1, "11.ºA", 4.5, 20)
            ),
            onClassClick = { _, _ -> },
            onBackClick = {}
        )
    }
}

/**
 * Dark mode preview for the [SubjectContent] UI.
 */
@Preview(showBackground = true, name = "Subject Detail Dark Mode")
@Composable
fun SubjectDarkPreview() {
    TeachersLogTheme(darkTheme = true) {
        SubjectContent(
            subjectName = "Natural Sciences",
            classes = listOf(
                StudentsClassWithStats(4, 2, "8.ºC", 4.0, 18),
                StudentsClassWithStats(5, 2, "9.ºA", 3.5, 24)
            ),
            onClassClick = { _, _ -> },
            onBackClick = {}
        )
    }
}
