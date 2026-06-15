package pt.ipt.dama.teacherslog.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.Student
import pt.ipt.dama.teacherslog.data.model.StudentWithGrade
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

/**
 * ClassRosterContent is the stateless UI component that displays the roster of students in a class.
 *
 * @param className The name of the class being displayed.
 * @param students The list of [StudentWithGrade] in the class.
 * @param onStudentClick Callback when a student is selected for detailed grading.
 * @param onAddStudent Callback to create a new student record.
 * @param onUpdateStudent Callback to update an existing student record.
 * @param onDeleteStudent Callback to delete a student record.
 * @param onBackClick Callback for the back button navigation.
 * @param checkNumberTaken Suspend function to check if a student number is already in use.
 * @param checkProcessTaken Suspend function to check if a process number is already in use.
 */
@Composable
fun ClassRosterContent(
    className: String,
    students: List<StudentWithGrade>,
    onStudentClick: (Int, String) -> Unit,
    onAddStudent: (String, Int, String?, String?) -> Unit,
    onUpdateStudent: (Student) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onBackClick: () -> Unit,
    checkNumberTaken: suspend (Int, Int) -> Boolean,
    checkProcessTaken: suspend (String, Int) -> Boolean
) {
    val showAddDialog = remember { mutableStateOf(false) }
    val studentToEdit = remember { mutableStateOf<Student?>(null) }
    val studentToDelete = remember { mutableStateOf<Student?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Internal state for form fields within the dialog
    var currentName by remember { mutableStateOf("") }
    var currentNumberStr by remember { mutableStateOf("") }
    var currentProcess by remember { mutableStateOf("") }
    var currentPhotoUri by remember { mutableStateOf("") }
    
    // Logic to find available numbers (1-100) not taken by other students
    val availableNumbers = remember(students, studentToEdit.value, showAddDialog.value) {
        val takenNumbers = students.map { it.numberInGroup }.toMutableSet()
        studentToEdit.value?.let { takenNumbers.remove(it.numberInGroup) }
        (1..100).filter { it !in takenNumbers }
    }

    // Validation state
    val processError = remember { mutableStateOf<String?>(null) }

    // Launcher for photo picking
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { 
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Ignore if persistence is not supported
                }
                currentPhotoUri = it.toString() 
            }
        }
    )

    // Sync internal state when dialog opens
    LaunchedEffect(showAddDialog.value, studentToEdit.value) {
        if (showAddDialog.value) {
            currentName = ""
            currentNumberStr = availableNumbers.firstOrNull()?.toString() ?: "1"
            currentProcess = ""
            currentPhotoUri = ""
            processError.value = null
        } else if (studentToEdit.value != null) {
            val s = studentToEdit.value!!
            currentName = s.name
            currentNumberStr = s.numberInGroup.toString()
            currentProcess = s.processNumber ?: ""
            currentPhotoUri = s.profileImageUrl ?: ""
            processError.value = null
        }
    }

    // --- Add/Edit Student Dialog ---
    if (showAddDialog.value || studentToEdit.value != null) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog.value = false
                studentToEdit.value = null
            },
            title = { Text(stringResource(if (studentToEdit.value == null) R.string.add_student else R.string.edit_student)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Profile Photo Picker Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { 
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentPhotoUri.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(currentPhotoUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        TextButton(onClick = { 
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text(stringResource(if (currentPhotoUri.isBlank()) R.string.add_photo else R.string.change_photo))
                        }
                    }

                    OutlinedTextField(
                        value = currentName,
                        onValueChange = { currentName = it },
                        label = { Text(stringResource(R.string.student_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Number Picker Row
                    Column {
                        Text(
                            text = stringResource(R.string.student_number),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    val current = currentNumberStr.toIntOrNull() ?: 1
                                    val prevAvailable = availableNumbers.lastOrNull { it < current }
                                    if (prevAvailable != null) {
                                        currentNumberStr = prevAvailable.toString()
                                    }
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                enabled = availableNumbers.any { it < (currentNumberStr.toIntOrNull() ?: 1) }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }

                            Box(modifier = Modifier.padding(horizontal = 24.dp).widthIn(min = 48.dp), contentAlignment = Alignment.Center) {
                                Text(text = currentNumberStr, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    val current = currentNumberStr.toIntOrNull() ?: 0
                                    val nextAvailable = availableNumbers.firstOrNull { it > current }
                                    if (nextAvailable != null) {
                                        currentNumberStr = nextAvailable.toString()
                                    }
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                enabled = availableNumbers.any { it > (currentNumberStr.toIntOrNull() ?: 0) }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = currentProcess,
                        onValueChange = { 
                            currentProcess = it
                            processError.value = null
                        },
                        label = { Text(stringResource(R.string.student_process)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = processError.value != null
                    )
                    if (processError.value != null) {
                        Text(text = processError.value!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                val errorProcExists = stringResource(R.string.error_process_exists)
                Button(
                    onClick = {
                        val num = currentNumberStr.toIntOrNull() ?: 0
                        val studentId = studentToEdit.value?.studentId ?: -1
                        scope.launch {
                            val isProcTaken = if (currentProcess.isNotBlank()) checkProcessTaken(currentProcess, studentId) else false
                            if (isProcTaken) {
                                processError.value = errorProcExists
                            } else {
                                if (studentToEdit.value == null) {
                                    onAddStudent(currentName, num, currentProcess.ifBlank { null }, currentPhotoUri.ifBlank { null })
                                } else {
                                    onUpdateStudent(studentToEdit.value!!.copy(
                                        name = currentName,
                                        numberInGroup = num,
                                        processNumber = currentProcess.ifBlank { null },
                                        profileImageUrl = currentPhotoUri.ifBlank { null }
                                    ))
                                }
                                showAddDialog.value = false
                                studentToEdit.value = null
                            }
                        }
                    },
                    enabled = currentName.isNotBlank() && currentNumberStr.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog.value = false
                    studentToEdit.value = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // --- Delete Confirmation Dialog ---
    if (studentToDelete.value != null) {
        AlertDialog(
            onDismissRequest = { studentToDelete.value = null },
            title = { Text(stringResource(R.string.delete_student_title)) },
            text = { Text(stringResource(R.string.delete_student_message, studentToDelete.value?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        studentToDelete.value?.let { onDeleteStudent(it) }
                        studentToDelete.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete.value = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TeacherTopBar(
                title = className,
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog.value = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_student))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(text = stringResource(R.string.students_roster), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(text = className, style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(students) { student ->
                val gradeColor = when {
                    student.weightedGrade >= 4.0 -> Color(0xFF22C55E)
                    student.weightedGrade >= 3.0 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }

                TonalCard(modifier = Modifier.clickable { onStudentClick(student.studentId, student.name) }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = student.numberInGroup.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            if (student.profileImageUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(student.profileImageUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = student.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        
                        Surface(color = gradeColor.copy(alpha = 0.1f), shape = CircleShape) {
                            Text(text = String.format(Locale.getDefault(), "%.2f", student.weightedGrade), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.titleMedium, color = gradeColor, fontWeight = FontWeight.ExtraBold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_student)) },
                                    onClick = {
                                        showMenu = false
                                        studentToEdit.value = Student(student.studentId, student.studentsClassId, student.numberInGroup, student.name, student.processNumber, student.profileImageUrl)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_student_title)) },
                                    onClick = {
                                        showMenu = false
                                        studentToDelete.value = Student(student.studentId, student.studentsClassId, student.numberInGroup, student.name, student.processNumber, student.profileImageUrl)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ClassRosterScreen is the stateful container for the class roster.
 */
@Composable
fun ClassRosterScreen(
    viewModel: TeacherViewModel,
    studentsClassId: Int,
    className: String,
    onStudentClick: (Int, String) -> Unit,
    onBackClick: () -> Unit
) {
    val students by viewModel.getStudentsWithGradesForStudentsClass(studentsClassId).observeAsState(emptyList())

    ClassRosterContent(
        className = className,
        students = students,
        onStudentClick = onStudentClick,
        onAddStudent = { n, num, p, photo -> viewModel.addStudent(n, num, p, photo, studentsClassId) },
        onUpdateStudent = { viewModel.updateStudent(it) },
        onDeleteStudent = { viewModel.deleteStudent(it) },
        onBackClick = onBackClick,
        checkNumberTaken = { num, id -> viewModel.isNumberTaken(studentsClassId, num, id) },
        checkProcessTaken = { p, id -> viewModel.isProcessNumberTaken(p, id) }
    )
}

// --- Previews ---

@Preview(showBackground = true, name = "Roster Light Mode")
@Composable
fun ClassRosterPreview() {
    TeachersLogTheme(darkTheme = false) {
        ClassRosterContent(
            className = "10.ºA",
            students = listOf(
                StudentWithGrade(1, 1, 1, "Alice Johnson", "P001", null, 4.5),
                StudentWithGrade(2, 1, 2, "Bob Smith", "P002", null, 3.2)
            ),
            onStudentClick = { _, _ -> },
            onAddStudent = { _, _, _, _ -> },
            onUpdateStudent = {},
            onDeleteStudent = {},
            onBackClick = {},
            checkNumberTaken = { _, _ -> false },
            checkProcessTaken = { _, _ -> false }
        )
    }
}

@Preview(showBackground = true, name = "Roster Dark Mode")
@Composable
fun ClassRosterDarkPreview() {
    TeachersLogTheme(darkTheme = true) {
        ClassRosterContent(
            className = "11.ºB",
            students = listOf(
                StudentWithGrade(3, 1, 1, "John Doe", "P003", null, 2.5)
            ),
            onStudentClick = { _, _ -> },
            onAddStudent = { _, _, _, _ -> },
            onUpdateStudent = {},
            onDeleteStudent = {},
            onBackClick = {},
            checkNumberTaken = { _, _ -> false },
            checkProcessTaken = { _, _ -> false }
        )
    }
}
