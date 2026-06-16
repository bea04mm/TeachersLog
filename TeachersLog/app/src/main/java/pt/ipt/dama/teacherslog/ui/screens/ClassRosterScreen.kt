package pt.ipt.dama.teacherslog.ui.screens

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.Student
import pt.ipt.dama.teacherslog.data.model.StudentWithGrade
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.components.SectionHeader
import pt.ipt.dama.teacherslog.ui.theme.*
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.teacherslog.utils.ShakeDetector
import pt.ipt.dama.teacherslog.utils.IconMapper
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Stateless content for the Random Student Selection dialog.
 * Extracted for better preview support and consistent design.
 *
 * @param student The student selected randomly.
 * @param onGoToGrading Callback to navigate to the evaluation screen.
 * @param onClose Callback to dismiss the popup.
 */
@Composable
fun RandomStudentPopupContent(
    student: StudentWithGrade,
    onGoToGrading: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(AppTheme.circle)
                .background(AppTheme.colors.surfaceContainerHigh),
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
                    imageVector = IconMapper.Account, 
                    contentDescription = null, 
                    modifier = Modifier.size(80.dp),
                    tint = AppTheme.colors.outline
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
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
        // Stacked buttons for better centering and prominence
        Button(
            onClick = onGoToGrading,
            modifier = Modifier.fillMaxWidth()
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
 * It manages:
 * 1. A scrollable student list with calculated grades.
 * 2. Shake-to-pick student feature registration.
 * 3. Conditional rendering of the random selection and deletion dialogs.
 *
 * @param className The name of the class being displayed.
 * @param classIcon Optional icon identifier inherited from parent subject.
 * @param students The list of [StudentWithGrade] in the class.
 * @param gradingScale The active grading scale for this class (e.g., "5", "20").
 * @param onStudentClick Callback when a student is selected for detailed grading.
 * @param onAddStudentClick Callback to navigate to student creation screen.
 * @param onEditStudentClick Callback to navigate to student editing screen.
 * @param onDeleteStudent Callback to remove a student record.
 * @param onBackClick Callback for back navigation.
 */
@Composable
fun ClassRosterContent(
    className: String,
    classIcon: String?,
    students: List<StudentWithGrade>,
    gradingScale: String,
    onStudentClick: (Int, String) -> Unit,
    onAddStudentClick: () -> Unit,
    onEditStudentClick: (Int) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onBackClick: () -> Unit
) {
    val studentToDelete = remember { mutableStateOf<Student?>(null) }
    val randomStudent = remember { mutableStateOf<StudentWithGrade?>(null) }
    val context = LocalContext.current

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
        AlertDialog(
            onDismissRequest = { randomStudent.value = null },
            title = { 
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.random_selection_title), 
                        style = AppTheme.typography.headlineSmall
                    ) 
                }
            },
            text = {
                RandomStudentPopupContent(
                    student = student,
                    onGoToGrading = {
                        onStudentClick(student.studentId, student.name)
                        randomStudent.value = null
                    },
                    onClose = { randomStudent.value = null }
                )
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // --- Deletion Confirmation ---
    if (studentToDelete.value != null) {
        AlertDialog(
            onDismissRequest = { studentToDelete.value = null },
            title = { Text(stringResource(R.string.delete_student_title)) },
            text = { Text(stringResource(R.string.delete_student_message, studentToDelete.value?.name ?: "")) },
            confirmButton = {
                Button(onClick = { onDeleteStudent(studentToDelete.value!!); studentToDelete.value = null }, colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.error)) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = { TextButton(onClick = { studentToDelete.value = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    Scaffold(
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
                ) { Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.pick_random_student)) }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(onClick = onAddStudentClick) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_student)) }
            }
        }
    ) { paddingValues ->
        val maxGradeValue = remember(gradingScale) {
            when(gradingScale) { "20" -> 20f; "100" -> 100f; else -> 5f }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionHeader(
                    label = stringResource(R.string.students_roster),
                    title = className
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(students) { student ->
                val percentage = (student.weightedGrade.toFloat() / maxGradeValue) * 100f
                val gradeColor = when {
                    percentage >= 80f -> AppTheme.success
                    percentage >= 50f -> AppTheme.warning
                    else -> AppTheme.critical
                }

                TonalCard(modifier = Modifier.clickable { onStudentClick(student.studentId, student.name) }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(32.dp), shape = AppTheme.circle, color = AppTheme.colors.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) { Text(text = student.numberInGroup.toString(), style = AppTheme.typography.labelMedium, color = AppTheme.colors.onPrimaryContainer) }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Box(modifier = Modifier.size(48.dp).clip(AppTheme.circle).background(AppTheme.colors.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                            if (student.profileImageUrl != null) {
                                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(student.profileImageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(IconMapper.Account, contentDescription = null, tint = AppTheme.colors.outline)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = student.name, 
                            style = AppTheme.typography.titleMedium, 
                            modifier = Modifier.weight(1f)
                        )
                        
                        Surface(
                            color = gradeColor.copy(alpha = 0.1f), 
                            shape = AppTheme.shapes.extraLarge
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", student.weightedGrade), 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), 
                                style = AppTheme.typography.titleMedium,
                                color = gradeColor
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null, tint = AppTheme.colors.outline) }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.edit_student)) }, onClick = { showMenu = false; onEditStudentClick(student.studentId) }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) })
                                DropdownMenuItem(text = { Text(stringResource(R.string.delete_student_title)) }, onClick = { showMenu = false; studentToDelete.value = Student(student.studentId, student.studentsClassId, student.numberInGroup, student.name, student.processNumber, student.profileImageUrl) }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}

/**
 * ClassRosterScreen stateful container.
 */
@Composable
fun ClassRosterScreen(
    viewModel: TeacherViewModel,
    studentsClassId: Int,
    className: String,
    onStudentClick: (Int, String) -> Unit,
    onAddStudentClick: (Int) -> Unit,
    onEditStudentClick: (Int, Int) -> Unit,
    onBackClick: () -> Unit
) {
    val students by viewModel.getStudentsWithGradesForStudentsClass(studentsClassId).observeAsState(emptyList())
    val classWithStats by viewModel.getStudentsClassWithStatsById(studentsClassId).observeAsState()
    var gradingScale by remember { mutableStateOf("5") }

    LaunchedEffect(studentsClassId) {
        gradingScale = viewModel.getGradingScaleForClass(studentsClassId)
        viewModel.updateClassLastAccessed(studentsClassId)
    }

    ClassRosterContent(
        className = className,
        classIcon = classWithStats?.iconName,
        students = students,
        gradingScale = gradingScale,
        onStudentClick = onStudentClick,
        onAddStudentClick = { onAddStudentClick(studentsClassId) },
        onEditStudentClick = { id -> onEditStudentClick(studentsClassId, id) },
        onDeleteStudent = { viewModel.deleteStudent(it) },
        onBackClick = onBackClick
    )
}
