package pt.ipt.dama.teacherslog.ui.screens

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.Student
import pt.ipt.dama.teacherslog.data.model.StudentWithGrade
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
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
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    imageVector = Icons.Default.Person, 
                    contentDescription = null, 
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = student.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = stringResource(R.string.student_number_label, student.numberInGroup),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.close))
            }
            Button(onClick = onGoToGrading, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.go_to_grading))
            }
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
            title = { Text(text = stringResource(R.string.random_selection_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
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
                Button(onClick = { onDeleteStudent(studentToDelete.value!!); studentToDelete.value = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
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
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape
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
                Text(text = stringResource(R.string.students_roster), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(text = className, style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(students) { student ->
                val percentage = (student.weightedGrade.toFloat() / maxGradeValue) * 100f
                val gradeColor = when { percentage >= 80f -> Color(0xFF22C55E); percentage >= 50f -> Color(0xFFF59E0B); else -> Color(0xFFEF4444) }

                TonalCard(modifier = Modifier.clickable { onStudentClick(student.studentId, student.name) }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) { Text(text = student.numberInGroup.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            if (student.profileImageUrl != null) {
                                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(student.profileImageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = student.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        
                        Surface(color = gradeColor.copy(alpha = 0.1f), shape = CircleShape) {
                            Text(text = String.format(Locale.getDefault(), "%.2f", student.weightedGrade), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.titleMedium, color = gradeColor, fontWeight = FontWeight.ExtraBold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.outline) }
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
