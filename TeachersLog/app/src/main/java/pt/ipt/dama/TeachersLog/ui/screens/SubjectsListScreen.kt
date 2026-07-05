package pt.ipt.dama.TeachersLog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.data.model.AcademicYear
import pt.ipt.dama.TeachersLog.data.model.Semester
import pt.ipt.dama.TeachersLog.data.model.Subject
import pt.ipt.dama.TeachersLog.data.model.SubjectWithStats
import pt.ipt.dama.TeachersLog.ui.components.*
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.components.SnackbarType
import pt.ipt.dama.TeachersLog.ui.components.showAppSnackbar
import pt.ipt.dama.TeachersLog.ui.viewmodel.AcademicViewModel
import pt.ipt.dama.TeachersLog.utils.IconMapper
import pt.ipt.dama.TeachersLog.utils.LocalizedNameMapper

/**
 * SubjectsListContent is the stateless UI component that displays the list of subjects.
 *
 * @param subjects The list of [SubjectWithStats] to display.
 * @param isRefreshing Boolean state for the pull-to-refresh UI.
 * @param onRefresh Callback to trigger cloud data synchronization.
 * @param onMenuClick Callback for when the navigation menu icon is clicked.
 * @param onSubjectClick Callback for when a subject card is clicked.
 * @param onAddSubjectClick Callback for the Floating Action Button to add a subject.
 * @param onEditSubjectClick Callback for the 'Edit' action in the subject menu.
 * @param onDeleteSubjectClick Callback for the 'Delete' action in the subject menu.
 * @param onCloneSubjectClick Callback to duplicate a subject to another period.
 * @param academicYears Data for the cloning destination selection.
 * @param semestersProvider Provider for semesters based on a selected year (for cloning).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsListContent(
    subjects: List<SubjectWithStats>,
    currentSemesterName: String?,
    isRefreshing: Boolean,
    isGuest: Boolean,
    syncEnabled: Boolean,
    onRefresh: () -> Unit,
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
    val pullToRefreshState = rememberPullToRefreshState()
    
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val deleteSuccessMsg = stringResource(R.string.delete_success)
    val cloneSuccessMsg = stringResource(R.string.clone_success)

    // --- Delete Confirmation Dialog ---
    subjectToDelete.value?.let { subject ->
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_subject_title),
            message = stringResource(R.string.delete_subject_message, subject.name),
            onConfirm = {
                onDeleteSubjectClick(subject)
                subjectToDelete.value = null
                scope.launch { snackbarHostState.showAppSnackbar(deleteSuccessMsg, type = SnackbarType.SUCCESS) }
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
                scope.launch { snackbarHostState.showAppSnackbar(cloneSuccessMsg, type = SnackbarType.SUCCESS) }
            },
            onDismiss = { subjectToClone.value = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = AppTheme.colors.background,
        topBar = { 
            TeacherTopBar(
                title = stringResource(R.string.nav_classes),
                titleIcon = IconMapper.Subjects,
                onNavigationIconClick = onMenuClick
            ) 
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSubjectClick,
                containerColor = AppTheme.colors.primaryContainer,
                contentColor = AppTheme.colors.onPrimaryContainer,
                shape = AppTheme.shapes.medium
            ) {
                Icon(IconMapper.Add, contentDescription = stringResource(R.string.add_subject))
            }
        }
    ) { paddingValues ->
        val content = @Composable {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Header Section ---
                item {
                    val label = currentSemesterName?.let { 
                        LocalizedNameMapper.getLocalizedSemesterName(it) 
                    } ?: stringResource(R.string.my_subjects)

                    SectionHeader(
                        label = label,
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
                        averageGrade = subject.averageGrade,
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
                                        imageVector = IconMapper.More, 
                                        contentDescription = null,
                                        tint = AppTheme.colors.outline
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    containerColor = AppTheme.colors.surfaceContainerHigh
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit_subject)) },
                                        onClick = {
                                            showMenu = false
                                            onEditSubjectClick(subject.id)
                                        },
                                        leadingIcon = { Icon(IconMapper.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.clone_label)) },
                                        onClick = {
                                            showMenu = false
                                            subjectToClone.value = subject
                                        },
                                        leadingIcon = { Icon(IconMapper.Copy, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_subject_title)) },
                                        onClick = {
                                            showMenu = false
                                            subjectToDelete.value = Subject(
                                                id = subject.id,
                                                semesterId = 0, 
                                                name = subject.name,
                                                iconName = subject.iconName,
                                                colorHex = subject.colorHex
                                            )
                                        },
                                        leadingIcon = { Icon(IconMapper.Delete, contentDescription = null, tint = AppTheme.colors.error) }
                                    )
                                }
                            }
                        }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        Box(modifier = Modifier.padding(paddingValues)) {
            if (isGuest || !syncEnabled) {
                content()
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing,
                            containerColor = AppTheme.colors.surface,
                            color = AppTheme.colors.primary,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * SubjectsListScreen is the stateful Composable.
 */
@Composable
fun SubjectsListScreen(
    academicViewModel: AcademicViewModel,
    authViewModel: pt.ipt.dama.TeachersLog.ui.viewmodel.AuthViewModel,
    onMenuClick: () -> Unit,
    onSubjectClick: (Int, String) -> Unit,
    onAddSubjectClick: () -> Unit,
    onEditSubjectClick: (Int) -> Unit,
) {
    // Observe subjects with statistics for the currently selected academic period
    val subjects by academicViewModel.subjectsWithStats.observeAsState(emptyList())
    val years by academicViewModel.allAcademicYears.observeAsState(emptyList())
    val semesters by academicViewModel.semesters.observeAsState(emptyList())
    val selectedSemesterId by academicViewModel.selectedSemesterId.observeAsState()
    val isRefreshing by academicViewModel.isRefreshing.collectAsState()
    val isGuest by authViewModel.isGuest.observeAsState(initial = false)
    val syncEnabled by authViewModel.isSyncEnabled.observeAsState(initial = false)
    
    val currentSemester = semesters.find { it.id == selectedSemesterId }

    SubjectsListContent(
        subjects = subjects,
        currentSemesterName = currentSemester?.name,
        isRefreshing = isRefreshing,
        isGuest = isGuest,
        syncEnabled = syncEnabled,
        onRefresh = { academicViewModel.refreshData() },
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
