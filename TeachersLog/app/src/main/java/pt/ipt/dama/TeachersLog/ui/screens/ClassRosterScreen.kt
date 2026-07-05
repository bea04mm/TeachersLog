package pt.ipt.dama.TeachersLog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LiveData
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.data.model.*
import pt.ipt.dama.TeachersLog.ui.components.*
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.components.SnackbarType
import pt.ipt.dama.TeachersLog.ui.components.showAppSnackbar
import pt.ipt.dama.TeachersLog.ui.viewmodel.AcademicViewModel
import pt.ipt.dama.TeachersLog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.TeachersLog.utils.LocalizedNameMapper
import pt.ipt.dama.TeachersLog.utils.ShakeDetector
import pt.ipt.dama.TeachersLog.utils.IconMapper
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.ui.platform.LocalLocale

/**
 * Component for the student selection popup when a shake is detected or triggered manually.
 *
 * @param title Header text for the popup.
 * @param student The selected [StudentWithGrade] to display.
 * @param onGoToGrading Callback to navigate to the student's grading screen.
 * @param onClose Callback to dismiss the popup.
 */
@Composable
fun RandomStudentPopupContent(
    title: String,
    student: StudentWithGrade,
    onGoToGrading: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = AppTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(AppTheme.circle)
                .background(AppTheme.colors.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (student.profileImageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(student.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    IconMapper.Account, 
                    contentDescription = null, 
                    modifier = Modifier.size(64.dp),
                    tint = AppTheme.colors.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = student.name,
            style = AppTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.student_number_label, student.numberInGroup),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGoToGrading,
            modifier = Modifier.fillMaxWidth(),
            shape = AppTheme.shapes.medium
        ) {
            Text(stringResource(R.string.go_to_grading))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.close))
        }
    }
}

/**
 * ClassRosterContent is the stateless UI component that displays the roster of students.
 *
 * @param className The name of the class being displayed.
 * @param classIcon Optional icon name for the class header.
 * @param students List of [StudentWithGrade] in the current class.
 * @param gradingScale The grading scale identifier (e.g., "5", "20").
 * @param isRefreshing Boolean state for the pull-to-refresh UI.
 * @param onRefresh Callback to trigger cloud data synchronization.
 * @param onStudentClick Callback when a student card is selected.
 * @param onAddStudentClick Callback to navigate to the student creation form.
 * @param onEditStudentClick Callback to navigate to the student editing form.
 * @param onDeleteStudent Callback to remove a student from the local database.
 * @param onCloneStudentClick Callback to duplicate a student to another class.
 * @param onBackClick Callback for navigating back to the previous screen.
 * @param academicYears Data for the cloning destination selection.
 * @param semestersProvider Provider for semesters based on a year.
 * @param subjectsProvider Provider for subjects based on a semester.
 * @param classesProvider Provider for classes based on a subject.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassRosterContent(
    className: String,
    currentSemesterName: String?,
    classIcon: String?,
    students: List<StudentWithGrade>,
    gradingScale: String,
    isRefreshing: Boolean,
    isGuest: Boolean,
    syncEnabled: Boolean,
    onRefresh: () -> Unit,
    onStudentClick: (Int, String) -> Unit,
    onAddStudentClick: () -> Unit,
    onEditStudentClick: (Int) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onCloneStudentClick: (Int, Int) -> Unit,
    onBackClick: () -> Unit,
    academicYears: List<AcademicYear>,
    semestersProvider: (Int) -> LiveData<List<Semester>>,
    subjectsProvider: (Int) -> LiveData<List<Subject>>,
    classesProvider: (Int) -> LiveData<List<StudentsClass>>
) {
    val studentToDelete = remember { mutableStateOf<Student?>(null) }
    val studentToClone = remember { mutableStateOf<StudentWithGrade?>(null) }
    val randomStudent = remember { mutableStateOf<StudentWithGrade?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
    
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val deleteSuccessMsg = stringResource(R.string.delete_success)
    val cloneSuccessMsg = stringResource(R.string.clone_success)

    // --- Clone Destination Dialog ---
    studentToClone.value?.let { student ->
        CloneStudentDestinationDialog(
            academicYears = academicYears,
            semestersProvider = semestersProvider,
            subjectsProvider = subjectsProvider,
            classesProvider = classesProvider,
            onConfirm = { targetClassId ->
                onCloneStudentClick(student.studentId, targetClassId)
                studentToClone.value = null
                scope.launch { snackbarHostState.showAppSnackbar(cloneSuccessMsg, type = SnackbarType.SUCCESS) }
            },
            onDismiss = { studentToClone.value = null }
        )
    }

    // --- Shake Detection Setup ---
    DisposableEffect(students) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        val shakeDetector = ShakeDetector {
            if (students.isNotEmpty() && randomStudent.value == null) {
                randomStudent.value = students.random()
            }
        }

        if (accelerometer != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(shakeDetector)
        }
    }

    // --- Random Student Selection Popup ---
    randomStudent.value?.let { student ->
        Dialog(onDismissRequest = { randomStudent.value = null }) {
            Surface(
                shape = AppTheme.shapes.extraLarge,
                color = AppTheme.colors.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                RandomStudentPopupContent(
                    title = stringResource(R.string.random_selection_title),
                    student = student,
                    onGoToGrading = {
                        onStudentClick(student.studentId, student.name)
                        randomStudent.value = null
                    },
                    onClose = { randomStudent.value = null }
                )
            }
        }
    }

    // --- Deletion Confirmation ---
    studentToDelete.value?.let { student ->
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_student_title),
            message = stringResource(R.string.delete_student_message, student.name),
            onConfirm = {
                onDeleteStudent(student)
                studentToDelete.value = null
                scope.launch { snackbarHostState.showAppSnackbar(deleteSuccessMsg, type = SnackbarType.SUCCESS) }
            },
            onDismiss = { studentToDelete.value = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = AppTheme.colors.background,
        topBar = { 
            TeacherTopBar(
                title = className, 
                titleIcon = IconMapper.getIconByName(classIcon),
                showBackButton = true, 
                onBackClick = onBackClick
            ) 
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { if (students.isNotEmpty()) randomStudent.value = students.random() },
                    containerColor = AppTheme.colors.secondaryContainer,
                    contentColor = AppTheme.colors.onSecondaryContainer,
                    shape = AppTheme.circle
                ) { Icon(IconMapper.AutoAwesome, contentDescription = stringResource(R.string.pick_random_student)) }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(
                    onClick = onAddStudentClick,
                    containerColor = AppTheme.colors.primaryContainer,
                    contentColor = AppTheme.colors.onPrimaryContainer,
                    shape = AppTheme.shapes.medium
                ) { 
                    Icon(IconMapper.Add, contentDescription = stringResource(R.string.add_student)) 
                }
            }
        }
    ) { paddingValues ->
        val content = @Composable {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    val label = currentSemesterName?.let { 
                        LocalizedNameMapper.getLocalizedSemesterName(it) 
                    } ?: stringResource(R.string.students_roster)

                    SectionHeader(
                        label = label,
                        title = className
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(students) { student ->
                    val percentage = AppTheme.calculatePercentage(student.weightedGrade, gradingScale)
                    val gradeColor = AppTheme.getGradeColor(percentage)

                    TonalCard(modifier = Modifier.clickable { onStudentClick(student.studentId, student.name) }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(32.dp), shape = AppTheme.circle, color = AppTheme.colors.primaryContainer) {
                                Box(contentAlignment = Alignment.Center) { Text(text = student.numberInGroup.toString(), style = AppTheme.typography.labelMedium, color = AppTheme.colors.onPrimaryContainer) }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Box(modifier = Modifier.size(48.dp).clip(AppTheme.circle).background(AppTheme.colors.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                                if (student.profileImageUrl != null) {
                                    AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(student.profileImageUrl).crossfade(true).build(), contentDescription = stringResource(R.string.cd_profile_photo), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(IconMapper.Account, contentDescription = null, tint = AppTheme.colors.outline)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = student.name, 
                                style = AppTheme.typography.titleMedium, 
                                modifier = Modifier.weight(1f),
                                color = AppTheme.colors.onSurface
                            )
                            
                            Surface(
                                color = gradeColor.copy(alpha = 0.1f), 
                                shape = AppTheme.shapes.extraLarge
                            ) {
                                Text(
                                    text = String.format(LocalLocale.current.platformLocale, "%.2f", student.weightedGrade),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), 
                                    style = AppTheme.typography.titleMedium,
                                    color = gradeColor
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) { 
                                    Icon(IconMapper.More, contentDescription = null, tint = AppTheme.colors.outline) 
                                }
                                DropdownMenu(
                                    expanded = showMenu, 
                                    onDismissRequest = { showMenu = false },
                                    containerColor = AppTheme.colors.surfaceContainerHigh
                                ) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.edit_student)) }, onClick = { showMenu = false; onEditStudentClick(student.studentId) }, leadingIcon = { Icon(IconMapper.Edit, contentDescription = null) })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.clone_label)) }, onClick = { showMenu = false; studentToClone.value = student }, leadingIcon = { Icon(IconMapper.Copy, contentDescription = null) })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.delete_student_title)) }, onClick = { 
                                        showMenu = false
                                        studentToDelete.value = Student(student.studentId, student.studentsClassId, student.numberInGroup, student.name, student.processNumber, student.profileImageUrl) 
                                    }, leadingIcon = { Icon(IconMapper.Delete, contentDescription = null, tint = AppTheme.colors.error) })
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(120.dp)) }
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
 * ClassRosterScreen stateful container.
 */
@Composable
fun ClassRosterScreen(
    academicViewModel: AcademicViewModel,
    viewModel: TeacherViewModel,
    authViewModel: pt.ipt.dama.TeachersLog.ui.viewmodel.AuthViewModel,
    studentsClassId: Int,
    className: String,
    onStudentClick: (Int, String) -> Unit,
    onAddStudentClick: (Int) -> Unit,
    onEditStudentClick: (Int, Int) -> Unit,
    onBackClick: () -> Unit
) {
    val students by viewModel.getStudentsWithGradesForStudentsClass(studentsClassId).observeAsState(emptyList())
    val classWithStats by viewModel.getStudentsClassWithStatsById(studentsClassId).observeAsState()
    val years by academicViewModel.allAcademicYears.observeAsState(emptyList())
    val semesters by academicViewModel.semesters.observeAsState(emptyList())
    val selectedSemesterId by academicViewModel.selectedSemesterId.observeAsState()
    val isRefreshing by academicViewModel.isRefreshing.collectAsState()
    val isGuest by authViewModel.isGuest.observeAsState(initial = false)
    val syncEnabled by authViewModel.isSyncEnabled.observeAsState(initial = false)
    var gradingScale by remember { mutableStateOf("5") }

    LaunchedEffect(studentsClassId) {
        gradingScale = viewModel.getGradingScaleForClass(studentsClassId)
        viewModel.updateClassLastAccessed(studentsClassId)
    }

    val currentSemester = semesters.find { it.id == selectedSemesterId }

    ClassRosterContent(
        className = className,
        currentSemesterName = currentSemester?.name,
        classIcon = classWithStats?.iconName,
        students = students,
        gradingScale = gradingScale,
        isRefreshing = isRefreshing,
        isGuest = isGuest,
        syncEnabled = syncEnabled,
        onRefresh = { academicViewModel.refreshData() },
        onStudentClick = onStudentClick,
        onAddStudentClick = { onAddStudentClick(studentsClassId) },
        onEditStudentClick = { id -> onEditStudentClick(studentsClassId, id) },
        onDeleteStudent = { viewModel.deleteStudent(it) },
        onCloneStudentClick = { sourceId, targetId -> viewModel.cloneStudent(sourceId, targetId) },
        onBackClick = onBackClick,
        academicYears = years,
        semestersProvider = { yearId -> academicViewModel.getSemestersForYear(yearId) },
        subjectsProvider = { semesterId -> academicViewModel.getSubjectsForSemester(semesterId) },
        classesProvider = { subjectId -> viewModel.getClassesForSubject(subjectId) }
    )
}
