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
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.AcademicYear
import pt.ipt.dama.teacherslog.data.model.Semester
import pt.ipt.dama.teacherslog.data.model.Subject
import pt.ipt.dama.teacherslog.data.model.StudentsClass
import pt.ipt.dama.teacherslog.data.model.StudentsClassWithStats
import pt.ipt.dama.teacherslog.ui.components.*
import pt.ipt.dama.teacherslog.ui.theme.*
import pt.ipt.dama.teacherslog.ui.viewmodel.AcademicViewModel
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.teacherslog.utils.IconMapper

/**
 * SubjectContent is the stateless UI component that displays classes for a subject.
 *
 * It manages:
 * 1. A list of [StudentsClassWithStats] for the selected subject.
 * 2. Navigation entry points for adding and editing classes.
 * 3. Deletion confirmation for classes using [DeleteConfirmationDialog].
 *
 * @param subjectName Name of the parent subject.
 * @param subjectIcon Optional icon identifier for the subject.
 * @param classes List of classes to display.
 * @param onClassClick Callback when a class is selected for roster view.
 * @param onAddClassClick Callback to navigate to class creation screen.
 * @param onEditClassClick Callback to navigate to class editing screen.
 * @param onDeleteClass Callback to remove a class from the database.
 * @param onBackClick Callback for back navigation.
 */
@Composable
fun SubjectContent(
    subjectName: String,
    subjectIcon: String?,
    classes: List<StudentsClassWithStats>,
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            item {
                SectionHeader(
                    label = stringResource(R.string.class_header),
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
}

/**
 * SubjectScreen connects the ViewModel to the [SubjectContent] UI.
 *
 * @param academicViewModel ViewModel for subject metadata.
 * @param teacherViewModel Shared ViewModel for class data access.
 * @param subjectId Current subject context.
 * @param subjectName Name for the header.
 * @param onClassClick Navigation to class roster.
 * @param onAddClassClick Navigation to class creation.
 * @param onEditClassClick Navigation to class editing.
 * @param onBackClick Navigation back.
 */
@Composable
fun SubjectScreen(
    academicViewModel: AcademicViewModel,
    teacherViewModel: TeacherViewModel,
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

    SubjectContent(
        subjectName = subjectName,
        subjectIcon = subject?.iconName,
        classes = classes,
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
