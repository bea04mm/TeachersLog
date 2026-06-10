package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.Subject
import pt.ipt.dama.teacherslog.data.model.SubjectWithStats
import pt.ipt.dama.teacherslog.ui.components.SubjectCard
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel

/**
 * SubjectsListContent is the stateless UI component that displays the list of subjects.
 *
 * It manages:
 * 1. The display of a scrollable list using [LazyColumn].
 * 2. The deletion confirmation dialog state.
 * 3. The layout for each subject card, including its stats and an options menu.
 *
 * @param subjects The list of [SubjectWithStats] to display.
 * @param onMenuClick Callback for when the navigation menu icon is clicked.
 * @param onSubjectClick Callback for when a subject card is clicked.
 * @param onViewAllClassesClick Callback for the 'View All Subjects' button.
 * @param onAddSubjectClick Callback for the Floating Action Button to add a subject.
 * @param onEditSubjectClick Callback for the 'Edit' action in the subject menu.
 * @param onDeleteSubjectClick Callback for the 'Delete' action in the subject menu.
 */
@Composable
fun SubjectsListContent(
    subjects: List<SubjectWithStats>,
    onMenuClick: () -> Unit,
    onSubjectClick: (Int, String) -> Unit,
    onViewAllClassesClick: () -> Unit,
    onAddSubjectClick: () -> Unit,
    onEditSubjectClick: (Int) -> Unit,
    onDeleteSubjectClick: (Subject) -> Unit,
) {
    // State to hold the subject currently marked for deletion
    val subjectToDelete = remember { mutableStateOf<Subject?>(null) }

    // --- Delete Confirmation Dialog ---
    if (subjectToDelete.value != null) {
        AlertDialog(
            onDismissRequest = { subjectToDelete.value = null },
            title = { Text(stringResource(R.string.delete_subject_title)) },
            text = { Text(stringResource(R.string.delete_subject_message, subjectToDelete.value?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        // Execute deletion callback and close dialog
                        subjectToDelete.value?.let { onDeleteSubjectClick(it) }
                        subjectToDelete.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete.value = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = { 
            TeacherTopBar(
                title = stringResource(R.string.nav_classes),
                onNavigationIconClick = onMenuClick
            ) 
        },
        floatingActionButton = {
            // Button to navigate to the Add Subject screen
            FloatingActionButton(onClick = onAddSubjectClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_subject))
            }
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
                    text = stringResource(R.string.my_subjects),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.active_curriculum),
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Button to view an overview of all classes across subjects
                OutlinedButton(
                    onClick = onViewAllClassesClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.School, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.view_all_classes))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- Subjects List ---
            items(subjects) { subject ->
                val subjectColor = Color(subject.colorHex.toColorInt())
                val subjectIcon = when(subject.iconName) {
                    "functions" -> Icons.Default.Functions
                    "biotech" -> Icons.Default.Biotech
                    "history_edu" -> Icons.Default.HistoryEdu
                    "language" -> Icons.Default.Language
                    "brush" -> Icons.Default.Brush
                    "calculate" -> Icons.Default.Calculate
                    "code" -> Icons.Default.Code
                    "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
                    "music_note" -> Icons.Default.MusicNote
                    "sports_basketball" -> Icons.Default.SportsBasketball
                    else -> Icons.Default.HistoryEdu
                }

                SubjectCard(
                    name = subject.name,
                    average = String.format(Locale.getDefault(), "%.2f", subject.averageGrade),
                    students = subject.studentCount,
                    color = subjectColor,
                    icon = subjectIcon,
                    onClick = { onSubjectClick(subject.id, subject.name) },
                    actions = {
                        // --- Subject Options Menu ---
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            // Custom clickable Box to remove all default IconButton padding
                            Box(
                                modifier = Modifier
                                    .clickable { showMenu = true }
                                    .padding(4.dp) // Minimal padding just for better touch target
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_subject)) },
                                    onClick = {
                                        showMenu = false
                                        onEditSubjectClick(subject.id)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_subject_title)) },
                                    onClick = {
                                        showMenu = false
                                        // Set the state to trigger the confirmation dialog
                                        subjectToDelete.value = Subject(
                                            id = subject.id,
                                            name = subject.name,
                                            academicYearId = 0, // Not needed for deletion
                                            iconName = subject.iconName,
                                            colorHex = subject.colorHex
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * SubjectsListScreen is the stateful Composable that connects the [TeacherViewModel]
 * to the [SubjectsListContent] UI.
 *
 * @param viewModel The shared [TeacherViewModel] providing the subjects data.
 * @param onMenuClick Callback for the side menu navigation.
 * @param onSubjectClick Callback when a subject is selected.
 * @param onViewAllClassesClick Callback to view the global classes list.
 * @param onAddSubjectClick Callback to navigate to the Add Subject screen.
 * @param onEditSubjectClick Callback to navigate to the Edit Subject screen.
 */
@Composable
fun SubjectsListScreen(
    viewModel: TeacherViewModel,
    onMenuClick: () -> Unit,
    onSubjectClick: (Int, String) -> Unit,
    onViewAllClassesClick: () -> Unit,
    onAddSubjectClick: () -> Unit,
    onEditSubjectClick: (Int) -> Unit,
) {
    // Observe subjects with statistics for the currently selected academic year
    val subjects by viewModel.subjectsWithStats.observeAsState(emptyList())
    
    SubjectsListContent(
        subjects = subjects,
        onMenuClick = onMenuClick,
        onSubjectClick = onSubjectClick,
        onViewAllClassesClick = onViewAllClassesClick,
        onAddSubjectClick = onAddSubjectClick,
        onEditSubjectClick = onEditSubjectClick,
        onDeleteSubjectClick = { viewModel.deleteSubject(it) }
    )
}

/**
 * Preview for the [SubjectsListContent] UI with sample data.
 */
@Preview(showBackground = true, name = "Subjects List Light Mode")
@Composable
fun SubjectsListPreview() {
    TeachersLogTheme(darkTheme = false) {
        SubjectsListContent(
            subjects = listOf(
                SubjectWithStats(1, "Mathematics", "functions", "#004AC6", 12, 4.2),
                SubjectWithStats(2, "Natural Sciences", "biotech", "#006686", 8, 3.8),
                SubjectWithStats(3, "History", "history_edu", "#7D5260", 15, 4.5),
                SubjectWithStats(4, "Arts", "brush", "#A03D2F", 10, 4.0)
            ),
            onMenuClick = {},
            onSubjectClick = { _, _ -> },
            onViewAllClassesClick = {},
            onAddSubjectClick = {},
            onEditSubjectClick = {},
            onDeleteSubjectClick = {}
        )
    }
}

/**
 * Dark mode preview for the [SubjectsListContent] UI.
 */
@Preview(showBackground = true, name = "Subjects List Dark Mode")
@Composable
fun SubjectsListDarkPreview() {
    TeachersLogTheme(darkTheme = true) {
        SubjectsListContent(
            subjects = listOf(
                SubjectWithStats(1, "Mathematics", "functions", "#004AC6", 12, 4.2),
                SubjectWithStats(2, "Natural Sciences", "biotech", "#006686", 8, 3.8)
            ),
            onMenuClick = {},
            onSubjectClick = { _, _ -> },
            onViewAllClassesClick = {},
            onAddSubjectClick = {},
            onEditSubjectClick = {},
            onDeleteSubjectClick = {}
        )
    }
}
