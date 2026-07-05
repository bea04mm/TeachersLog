package pt.ipt.dama.TeachersLog.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.data.model.Student
import pt.ipt.dama.TeachersLog.ui.components.TeacherTopBar
import pt.ipt.dama.TeachersLog.ui.components.TonalCard
import pt.ipt.dama.TeachersLog.ui.components.LocalSnackbarHostState
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.TeachersLog.utils.IconMapper

/**
 * StudentEditContent is the stateless UI component for student management.
 *
 * @param isEditMode Flag for modifying existing student vs adding new.
 * @param initialName Starting name string.
 * @param initialNumber Starting roster position.
 * @param initialProcess Starting administrative string.
 * @param initialPhoto Starting photo URI/Path.
 * @param availableNumbers List of free roster slots (1-100).
 * @param onSave Callback for persistence.
 * @param onBackClick Navigation callback.
 * @param checkProcessTaken Uniqueness validation for process numbers.
 */
@Composable
fun StudentEditContent(
    isEditMode: Boolean,
    initialName: String,
    initialNumber: Int,
    initialProcess: String,
    initialPhoto: String,
    availableNumbers: List<Int>,
    onSave: (String, Int, String?, String?) -> Unit,
    onBackClick: () -> Unit,
    checkProcessTaken: suspend (String) -> Boolean
) {
    var name by remember { mutableStateOf(initialName) }
    var numberStr by remember { mutableStateOf(if (isEditMode) initialNumber.toString() else "") }
    var isNumberAutoSet by remember { mutableStateOf(!isEditMode) }
    var process by remember { mutableStateOf(initialProcess) }
    var photoUri by remember { mutableStateOf(initialPhoto) }
    
    val processError = remember { mutableStateOf<String?>(null) }
    val numberError = remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Reactive effect to force the first available number as soon as the roster is loaded
    LaunchedEffect(availableNumbers, isEditMode) {
        if (!isEditMode && isNumberAutoSet) {
            val nextFree = availableNumbers.firstOrNull()?.toString() ?: "1"
            numberStr = nextFree
        }
    }

    // Update other fields when initial values (from DB) change
    LaunchedEffect(initialName, initialNumber, initialProcess, initialPhoto) {
        if (isEditMode) {
            name = initialName
            numberStr = initialNumber.toString()
            process = initialProcess
            photoUri = initialPhoto
        }
    }

    // Photo picker with permission persistence
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { 
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
                photoUri = it.toString() 
            }
        }
    )

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = { 
            TeacherTopBar(
                title = stringResource(if (isEditMode) R.string.edit_student else R.string.add_student), 
                showBackButton = true, 
                onBackClick = onBackClick
            ) 
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Photo
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(120.dp).clip(AppTheme.circle).background(AppTheme.colors.surfaceContainerHigh)
                            .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoUri)
                                    .crossfade(true)
                                    .build(), 
                                contentDescription = null, 
                                modifier = Modifier.fillMaxSize(), 
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = IconMapper.AddPhoto, 
                                contentDescription = null, 
                                modifier = Modifier.size(40.dp), 
                                tint = AppTheme.colors.outline
                            )
                        }
                    }
                    TextButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(stringResource(if (photoUri.isBlank()) R.string.add_photo else R.string.change_photo))
                    }
                }
            }

            // Identity Information
            item {
                TonalCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = stringResource(R.string.personal_information), 
                            style = AppTheme.typography.labelSmall, 
                            color = AppTheme.colors.primary
                        )
                        OutlinedTextField(
                            value = name, 
                            onValueChange = { name = it }, 
                            label = { Text(stringResource(R.string.student_name)) }, 
                            modifier = Modifier.fillMaxWidth(), 
                            singleLine = true,
                            shape = AppTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = process, 
                            onValueChange = { process = it; processError.value = null }, 
                            label = { Text(stringResource(R.string.student_process)) }, 
                            modifier = Modifier.fillMaxWidth(), 
                            singleLine = true, 
                            isError = processError.value != null,
                            shape = AppTheme.shapes.medium
                        )
                        processError.value?.let { 
                            Text(
                                text = it, 
                                color = AppTheme.colors.error, 
                                style = AppTheme.typography.labelSmall, 
                                modifier = Modifier.padding(start = 16.dp)
                            ) 
                        }
                    }
                }
            }

            // Roster Position
            item {
                TonalCard {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.student_number), 
                            style = AppTheme.typography.labelMedium, 
                            color = AppTheme.colors.primary, 
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            IconButton(
                                onClick = { 
                                    isNumberAutoSet = false
                                    val current = numberStr.toIntOrNull() ?: 1
                                    availableNumbers.lastOrNull { it < current }?.let { 
                                        numberStr = it.toString()
                                        numberError.value = null 
                                    } 
                                },
                                modifier = Modifier.background(AppTheme.colors.primaryContainer, AppTheme.shapes.extraLarge),
                                enabled = availableNumbers.any { it < (numberStr.toIntOrNull() ?: 1) }
                            ) { Icon(IconMapper.Remove, contentDescription = null, tint = AppTheme.colors.primary) }

                            Text(
                                text = numberStr, 
                                style = AppTheme.typography.displayMedium, 
                                modifier = Modifier.padding(horizontal = 32.dp),
                                color = AppTheme.colors.onSurface
                            )

                            IconButton(
                                onClick = { 
                                    isNumberAutoSet = false
                                    val current = numberStr.toIntOrNull() ?: 0
                                    availableNumbers.firstOrNull { it > current }?.let { 
                                        numberStr = it.toString()
                                        numberError.value = null 
                                    } 
                                },
                                modifier = Modifier.background(AppTheme.colors.primaryContainer, AppTheme.shapes.extraLarge),
                                enabled = availableNumbers.any { it > (numberStr.toIntOrNull() ?: 0) }
                            ) { Icon(IconMapper.Add, contentDescription = null, tint = AppTheme.colors.primary) }
                        }
                        numberError.value?.let { 
                            Text(
                                text = it, 
                                color = AppTheme.colors.error, 
                                style = AppTheme.typography.labelSmall, 
                                modifier = Modifier.padding(top = 8.dp)
                            ) 
                        }
                    }
                }
            }

            // Save Action
            item {
                val errorProcExists = stringResource(R.string.error_process_exists)
                val errorNumExists = stringResource(R.string.error_number_exists)
                Button(
                    onClick = {
                        val num = numberStr.toIntOrNull() ?: 0
                        scope.launch {
                            val isNumTaken = if (num != initialNumber) availableNumbers.all { it != num } else false
                            val isProcTaken = if (process.isNotBlank()) checkProcessTaken(process) else false
                            if (isNumTaken) numberError.value = errorNumExists
                            else if (isProcTaken) processError.value = errorProcExists
                            else onSave(name, num, process.ifBlank { null }, photoUri.ifBlank { null })
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = name.isNotBlank() && numberStr.isNotBlank(),
                    shape = AppTheme.shapes.medium
                ) {
                    Icon(IconMapper.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save))
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * StudentEditScreen connects data logic to the UI.
 * 
 * @param viewModel Shared teacher logic.
 * @param classId Contextual class identifier.
 * @param studentId Optional student identifier for editing.
 * @param onBackClick Navigation callback.
 */
@Composable
fun StudentEditScreen(
    viewModel: TeacherViewModel,
    classId: Int,
    studentId: Int?,
    onBackClick: () -> Unit
) {
    val existingStudent by if (studentId != null) produceState<Student?>(initialValue = null) { value = viewModel.getStudentById(studentId) } else remember { mutableStateOf(null) }
    val studentsInClass by viewModel.getStudentsWithGradesForStudentsClass(classId).observeAsState(emptyList())
    
    val availableNumbers = remember(studentsInClass, existingStudent) {
        val takenNumbers = studentsInClass.map { it.numberInGroup }.toMutableSet()
        existingStudent?.let { takenNumbers.remove(it.numberInGroup) }
        (1..100).filter { it !in takenNumbers }
    }
    
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val successMsg = stringResource(R.string.save_success)
    val shortNameMsg = stringResource(R.string.error_name_too_short)

    StudentEditContent(
        isEditMode = studentId != null, 
        initialName = existingStudent?.name ?: "", 
        initialNumber = existingStudent?.numberInGroup ?: 0, 
        initialProcess = existingStudent?.processNumber ?: "", 
        initialPhoto = existingStudent?.profileImageUrl ?: "",
        availableNumbers = availableNumbers,
        onSave = { n, num, proc, photo ->
            if (n.trim().length < 3) {
                scope.launch { snackbarHostState.showSnackbar(shortNameMsg) }
                return@StudentEditContent
            }
            if (studentId == null) viewModel.addStudent(n, num, proc, photo, classId)
            else existingStudent?.let { viewModel.updateStudent(it.copy(name = n, numberInGroup = num, processNumber = proc, profileImageUrl = photo)) }
            scope.launch { snackbarHostState.showSnackbar(successMsg) }
            onBackClick()
        },
        onBackClick = onBackClick,
        checkProcessTaken = { p -> viewModel.isProcessNumberTaken(p, studentId ?: -1) }
    )
}
