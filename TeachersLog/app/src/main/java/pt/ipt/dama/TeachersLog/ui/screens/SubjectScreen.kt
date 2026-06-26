package pt.ipt.dama.TeachersLog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.data.model.AcademicYear
import pt.ipt.dama.TeachersLog.data.model.Semester
import pt.ipt.dama.TeachersLog.data.model.Subject
import pt.ipt.dama.TeachersLog.data.model.StudentsClass
import pt.ipt.dama.TeachersLog.data.model.StudentsClassWithStats
import pt.ipt.dama.TeachersLog.ui.components.*
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.viewmodel.AcademicViewModel
import pt.ipt.dama.TeachersLog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.TeachersLog.utils.LocalizedNameMapper
import pt.ipt.dama.TeachersLog.utils.IconMapper

/**
 * SubjectContent is the stateless UI component that displays classes for a specific subject.
 *
 * @param subjectName The name of the current subject.
 * @param subjectIcon Optional icon name for the subject.
 * @param classes List of [StudentsClassWithStats] associated with the subject.
 * @param isRefreshing Boolean state for the pull-to-refresh UI.
 * @param onRefresh Callback to trigger cloud data synchronization.
 * @param onClassClick Callback when a class card is selected.
 * @param onAddClassClick Callback to navigate to the class creation form.
 * @param onEditClassClick Callback to navigate to the class editing form.
 * @param onDeleteClass Callback to remove a class from the local database.
 * @param onCloneClassClick Callback to duplicate a class to another subject.
 * @param onBackClick Callback for navigating back to the previous screen.
 * @param academicYears Data for the cloning destination selection.
 * @param semestersProvider Provider for semesters based on a selected year.
 * @param subjectsProvider Provider for subjects based on a selected semester.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectContent(
    subjectName: String,
    currentSemesterName: String?,
    subjectIcon: String?,
    classes: List<StudentsClassWithStats>,
    isRefreshing: Boolean,
    isGuest: Boolean,
    syncEnabled: Boolean,
    onRefresh: () -> Unit,
    onClassClick: (Int, String) -> Unit,
    onAddClassClick: () -> Unit,
    onEditClassClick: (Int) -> Unit,
    onDeleteClass: (StudentsClass) -> Unit,
    onCloneClassClick: (Int, Int) -> Unit,
    onBackClick: () -> Unit,
    academicYears: List<AcademicYear>,
    semestersProvider: (Int) -> LiveData<List<Semester>>,
    subjectsProvider: (Int) -> LiveData<List<Subject>>
) {
    val classToDelete = remember { mutableStateOf<StudentsClass?>(null) }
    val classToClone = remember { mutableStateOf<StudentsClassWithStats?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()

    // ... (dialogs stay same) ...
    // --- Deletion Confirmation Dialog ---
    classToDelete.value?.let { clazz ->
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_class_title),
            message = stringResource(R.string.delete_class_message, clazz.name),
            onConfirm = {
                onDeleteClass(clazz)
                classToDelete.value = null
            },
            onDismiss = { classToDelete.value = null }
        )
    }

    // --- Clone Destination Dialog ---
    classToClone.value?.let { clazz ->
        CloneClassDestinationDialog(
            academicYears = academicYears,
            semestersProvider = semestersProvider,
            subjectsProvider = subjectsProvider,
            onConfirm = { targetSubjectId ->
                onCloneClassClick(clazz.id, targetSubjectId)
                classToClone.value = null
            },
            onDismiss = { classToClone.value = null }
        )
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = { 
            TeacherTopBar(
                title = subjectName,
                titleIcon = IconMapper.getIconByName(subjectIcon),
                showBackButton = true, 
                onBackClick = onBackClick
            ) 
        },
        floatingActionButton = { 
            FloatingActionButton(onClick = onAddClassClick) { 
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_class)) 
            } 
        }
    ) { paddingValues ->
        val content = @Composable {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp), 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Screen Header
                item {
                    val label = currentSemesterName?.let { 
                        LocalizedNameMapper.getLocalizedSemesterName(it) 
                    } ?: stringResource(R.string.class_header)

                    SectionHeader(
                        label = label,
                        title = subjectName
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Interactive class cards
                items(classes) { clazz ->
                    ClassCard(
                        name = clazz.name, 
                        studentCount = clazz.studentCount, 
                        averageGrade = clazz.averageGrade, 
                        color = AppTheme.parseColor(clazz.colorHex),
                        gradingScale = clazz.gradingScale, 
                        iconName = clazz.iconName,
                        onClick = { onClassClick(clazz.id, clazz.name) },
                        actions = {
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) { 
                                    Icon(Icons.Default.MoreVert, contentDescription = null) 
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit_class)) }, 
                                        onClick = {
                                            showMenu = false
                                            onEditClassClick(clazz.id)
                                        }, 
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.clone_label)) },
                                        onClick = {
                                            showMenu = false
                                            classToClone.value = clazz
                                        },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_class_title)) }, 
                                        onClick = {
                                            showMenu = false
                                            classToDelete.value = StudentsClass(
                                                id = clazz.id, 
                                                subjectId = clazz.subjectId, 
                                                name = clazz.name, 
                                                gradingScale = clazz.gradingScale
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
 * SubjectScreen connects the ViewModel to the [SubjectContent] UI.
 */
@Composable
fun SubjectScreen(
    academicViewModel: AcademicViewModel,
    teacherViewModel: TeacherViewModel,
    authViewModel: pt.ipt.dama.TeachersLog.ui.viewmodel.AuthViewModel,
    subjectId: Int,
    subjectName: String,
    onClassClick: (Int, String) -> Unit,
    onAddClassClick: (Int) -> Unit,
    onEditClassClick: (Int, Int) -> Unit,
    onBackClick: () -> Unit
) {
    val classes by teacherViewModel.getStudentsClassesForSubject(subjectId).observeAsState(emptyList())
    val subject by academicViewModel.getSubjectById(subjectId).observeAsState()
    val years by academicViewModel.allAcademicYears.observeAsState(emptyList())
    val semesters by academicViewModel.semesters.observeAsState(emptyList())
    val selectedSemesterId by academicViewModel.selectedSemesterId.observeAsState()
    val isRefreshing by academicViewModel.isRefreshing.collectAsState()
    val isGuest by authViewModel.isGuest.observeAsState(initial = false)
    val syncEnabled by authViewModel.isSyncEnabled.observeAsState(initial = false)

    val currentSemester = semesters.find { it.id == selectedSemesterId }

    SubjectContent(
        subjectName = subjectName,
        currentSemesterName = currentSemester?.name,
        subjectIcon = subject?.iconName,
        classes = classes,
        isRefreshing = isRefreshing,
        isGuest = isGuest,
        syncEnabled = syncEnabled,
        onRefresh = { academicViewModel.refreshData() },
        onClassClick = onClassClick,
        onAddClassClick = { onAddClassClick(subjectId) },
        onEditClassClick = { classId -> onEditClassClick(subjectId, classId) },
        onDeleteClass = { teacherViewModel.deleteStudentsClass(it) },
        onCloneClassClick = { sourceId, targetId -> teacherViewModel.cloneClass(sourceId, targetId) },
        onBackClick = onBackClick,
        academicYears = years,
        semestersProvider = { yearId -> academicViewModel.getSemestersForYear(yearId) },
        subjectsProvider = { semesterId -> academicViewModel.getSubjectsForSemester(semesterId) }
    )
}
