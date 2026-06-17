package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.AcademicYear
import pt.ipt.dama.teacherslog.data.model.Semester
import pt.ipt.dama.teacherslog.data.model.Subject
import pt.ipt.dama.teacherslog.data.model.SubjectWithStats
import pt.ipt.dama.teacherslog.ui.components.*
import pt.ipt.dama.teacherslog.ui.theme.*
import pt.ipt.dama.teacherslog.ui.viewmodel.AcademicViewModel
import pt.ipt.dama.teacherslog.utils.IconMapper

/**
 * SubjectsListContent is the stateless UI component that displays the list of subjects.
 *
 * It manages:
 * 1. The display of a scrollable list using [LazyColumn].
 * 2. The deletion confirmation dialog state using [DeleteConfirmationDialog].
 * 3. The layout for each subject card, including its stats and an options menu.
 *
 * @param subjects The list of [SubjectWithStats] to display.
 * @param onMenuClick Callback for when the navigation menu icon is clicked.
 * @param onSubjectClick Callback for when a subject card is clicked.
 * @param onAddSubjectClick Callback for the Floating Action Button to add a subject.
 * @param onEditSubjectClick Callback for the 'Edit' action in the subject menu.
 * @param onDeleteSubjectClick Callback for the 'Delete' action in the subject menu.
 */
@Composable
fun SubjectsListContent(
    subjects: List<SubjectWithStats>,
    onMenuClick: () -> Unit,
    onSubjectClick: (Int, String) -> Unit,
    onAddSubjectClick: () -> Unit,
    onEditSubjectClick: (Int) -> Unit,
    onDeleteSubjectClick: (Subject) -> Unit,
    onCloneSubjectClick: (Int, Int) -> Unit,
    academicYears: List<AcademicYear>,
    semestersProvider: (Int) -> LiveData<List<Semester>>
) {
    // State to hold the subject currently marked for deletion
    val subjectToDelete = remember { mutableStateOf<Subject?>(null) }
    val subjectToClone = remember { mutableStateOf<SubjectWithStats?>(null) }

    // --- Delete Confirmation Dialog ---
    subjectToDelete.value?.let { subject ->
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_subject_title),
            message = stringResource(R.string.delete_subject_message, subject.name),
            onConfirm = {
                onDeleteSubjectClick(subject)
                subjectToDelete.value = null
            },
            onDismiss = { subjectToDelete.value = null }
        )
    }

    // --- Clone Destination Dialog ---
    subjectToClone.value?.let { subject ->
        CloneDestinationDialog(
            title = stringResource(R.string.clone_subject_title),
            academicYears = academicYears,
            semestersProvider = semestersProvider,
            onConfirm = { targetSemesterId ->
                onCloneSubjectClick(subject.id, targetSemesterId)
                subjectToClone.value = null
            },
            onDismiss = { subjectToClone.value = null }
        )
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = { 
            TeacherTopBar(
                title = stringResource(R.string.nav_classes),
                titleIcon = IconMapper.Subjects,
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
                SectionHeader(
                    label = stringResource(R.string.my_subjects),
                    title = stringResource(R.string.active_curriculum)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- Subjects List ---
            items(subjects) { subject ->
                val subjectColor = AppTheme.parseColor(subject.colorHex)
                val subjectIcon = IconMapper.getIconByName(subject.iconName)

                SubjectCard(
                    name = subject.name,
                    average = String.format(Locale.getDefault(), "%.2f", subject.averageGrade),
                    students = subject.studentCount,
                    color = subjectColor,
                    icon = subjectIcon,
                    gradingScale = "100", // Subjects show normalized average
                    onClick = { onSubjectClick(subject.id, subject.name) },
                    actions = {
                        // --- Subject Options Menu ---
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert, 
                                    contentDescription = null,
                                    tint = AppTheme.colors.outline
                                )
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
                                    text = { Text(stringResource(R.string.clone_label)) },
                                    onClick = {
                                        showMenu = false
                                        subjectToClone.value = subject
                                    },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_subject_title)) },
                                    onClick = {
                                        showMenu = false
                                        // Set the state to trigger the confirmation dialog
                                        subjectToDelete.value = Subject(
                                            id = subject.id,
                                            semesterId = 0, // Not needed for deletion
                                            name = subject.name,
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
            
            // Add spacer at the bottom to prevent FAB from overlapping the last item
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * SubjectsListScreen is the stateful Composable that connects the [AcademicViewModel]
 * to the [SubjectsListContent] UI.
 *
 * @param academicViewModel ViewModel managing academic curriculum.
 * @param onMenuClick Callback to open drawer.
 * @param onSubjectClick Callback when a subject is selected.
 * @param onAddSubjectClick Callback to navigate to addition.
 * @param onEditSubjectClick Callback to navigate to editing.
 */
@Composable
fun SubjectsListScreen(
    academicViewModel: AcademicViewModel,
    onMenuClick: () -> Unit,
    onSubjectClick: (Int, String) -> Unit,
    onAddSubjectClick: () -> Unit,
    onEditSubjectClick: (Int) -> Unit,
) {
    // Observe subjects with statistics for the currently selected academic period
    val subjects by academicViewModel.subjectsWithStats.observeAsState(emptyList())
    val years by academicViewModel.allAcademicYears.observeAsState(emptyList())
    
    SubjectsListContent(
        subjects = subjects,
        onMenuClick = onMenuClick,
        onSubjectClick = onSubjectClick,
        onAddSubjectClick = onAddSubjectClick,
        onEditSubjectClick = onEditSubjectClick,
        onDeleteSubjectClick = { academicViewModel.deleteSubject(it) },
        onCloneSubjectClick = { sourceId, targetId -> academicViewModel.cloneSubject(sourceId, targetId) },
        academicYears = years,
        semestersProvider = { yearId -> academicViewModel.getSemestersForYear(yearId) }
    )
}
