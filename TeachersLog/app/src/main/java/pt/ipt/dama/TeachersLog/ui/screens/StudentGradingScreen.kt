package pt.ipt.dama.TeachersLog.ui.screens

import android.app.Activity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.data.model.*
import pt.ipt.dama.TeachersLog.ui.components.TeacherTopBar
import pt.ipt.dama.TeachersLog.ui.components.TonalCard
import pt.ipt.dama.TeachersLog.ui.components.LocalSnackbarHostState
import pt.ipt.dama.TeachersLog.ui.components.SnackbarType
import pt.ipt.dama.TeachersLog.ui.components.showAppSnackbar
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.TeachersLog.utils.IconMapper
import pt.ipt.dama.TeachersLog.utils.LocalizedNameMapper

/**
 * StudentGradingContent is the stateless UI component for student grading.
 * Allows for better preview support and isolation from database logic.
 *
 * @param student The student being graded.
 * @param className Current class context.
 * @param componentScores List of grading criteria with current scores.
 * @param gradingScale Scale identifier ("5", "20", etc.).
 * @param teacherNotes Current qualitative feedback.
 * @param onScoreChange Callback when a slider is moved.
 * @param onNotesChange Callback when text is typed in the notes field.
 * @param onSave Callback for the final save action.
 * @param onBackClick Navigation callback.
 */
@Composable
fun StudentGradingContent(
    student: Student?,
    className: String,
    currentSemesterName: String?,
    componentScores: List<ComponentScore>,
    gradingScale: String,
    teacherNotes: String,
    onScoreChange: (Int, Double) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var isOcrProcessing by remember { mutableStateOf(false) }

    val isInPreview = LocalInspectionMode.current
    val textRecognizer = remember {
        try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        } catch (_: Exception) {
            null
        }
    }

    val ocrNoTextMsg = stringResource(R.string.ocr_no_text_found)
    val ocrErrorMsg = stringResource(R.string.ocr_error)
    val activityErrorMsg = stringResource(R.string.error_activity_not_found)

    // Set up the Document Scanner client
    val scannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setResultFormats(RESULT_FORMAT_JPEG)
        .setScannerMode(SCANNER_MODE_FULL)
        .build()

    val scanner = remember {
        try {
            GmsDocumentScanning.getClient(scannerOptions)
        } catch (_: Exception) {
            null
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val firstPage = scanResult?.pages?.firstOrNull()
                firstPage?.imageUri?.let { uri ->
                    isOcrProcessing = true
                    try {
                        val image = InputImage.fromFilePath(context, uri)
                        textRecognizer?.process(image)
                            ?.addOnSuccessListener { visionText ->
                                isOcrProcessing = false
                                val scannedText = visionText.text
                                if (scannedText.isNotBlank()) {
                                    onNotesChange(if (teacherNotes.isBlank()) scannedText else "$teacherNotes\n$scannedText")
                                } else {
                                    scope.launch { snackbarHostState.showAppSnackbar(ocrNoTextMsg, type = SnackbarType.ERROR) }
                                }
                            }
                            ?.addOnFailureListener { e ->
                                isOcrProcessing = false
                                scope.launch { snackbarHostState.showAppSnackbar("${ocrErrorMsg}: ${e.message}", type = SnackbarType.ERROR) }
                            }
                    } catch (_: Exception) {
                        isOcrProcessing = false
                        scope.launch { snackbarHostState.showAppSnackbar(ocrErrorMsg, type = SnackbarType.ERROR) }
                    }
                }
            }
        }
    )

    val maxGradeValue = remember(gradingScale) {
        when(gradingScale) { "20" -> 20f; "100" -> 100f; else -> 5f }
    }
    val scaleSuffix = when(gradingScale) {
        "20" -> stringResource(R.string.scale_suffix_20)
        "100" -> stringResource(R.string.scale_suffix_100)
        else -> stringResource(R.string.scale_suffix_5)
    }

    val totalWeightedGrade = componentScores.filter { !it.isGroup }.sumOf { 
        it.score * (it.absoluteWeight / 100.0) 
    }

    val percentage = AppTheme.calculatePercentage(totalWeightedGrade, gradingScale)
    val gradeColor = AppTheme.getGradeColor(percentage)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = AppTheme.colors.background,
        topBar = {
            TeacherTopBar(
                title = stringResource(R.string.student_grading),
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                TonalCard {
                    Row(
                        modifier = Modifier.padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(64.dp).clip(AppTheme.circle).background(AppTheme.colors.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                                if (student?.profileImageUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(student.profileImageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = stringResource(R.string.cd_profile_photo),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(IconMapper.Account, contentDescription = null, modifier = Modifier.size(32.dp), tint = AppTheme.colors.outline)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                val label = currentSemesterName?.let { LocalizedNameMapper.getLocalizedSemesterName(it) } ?: className
                                Text(text = label, style = AppTheme.typography.labelMedium, color = AppTheme.colors.primary)
                                Text(text = student?.name ?: stringResource(R.string.loading), style = AppTheme.typography.titleLarge)
                                student?.processNumber?.let { Text(text = stringResource(R.string.process_prefix, it), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.outline) }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(modifier = Modifier.size(36.dp), shape = AppTheme.shapes.extraLarge, color = AppTheme.colors.primaryContainer) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = student?.numberInGroup?.toString() ?: "-", style = AppTheme.typography.titleMedium, color = AppTheme.colors.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }

            item {
                TonalCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.total_grade_weighted), style = AppTheme.typography.labelSmall, color = AppTheme.colors.outline)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = String.format(locale, "%.2f", totalWeightedGrade), style = AppTheme.typography.displayLarge, color = gradeColor)
                            Text(text = scaleSuffix, style = AppTheme.typography.headlineSmall, color = AppTheme.colors.outline, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                        }
                    }
                }
            }

            val rootComponents = componentScores.filter { it.parentId == null }
            rootComponents.forEach { group ->
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = group.componentName.uppercase(locale), style = AppTheme.typography.labelMedium, color = AppTheme.colors.primary)
                        Text(text = stringResource(R.string.weight_percentage_format, group.weight), style = AppTheme.typography.labelSmall, color = AppTheme.colors.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                val children = componentScores.filter { it.parentId == group.componentId }
                if (children.isEmpty()) {
                    item {
                        GradeSliderCard(
                            title = group.componentName, weight = stringResource(R.string.weight_percentage_format, group.weight), icon = IconMapper.Analytics, value = group.score.toFloat(), 
                            maxGrade = maxGradeValue, scaleSuffix = scaleSuffix, onValueChange = { onScoreChange(group.componentId, it.toDouble()) }, rangeLabels = listOf(stringResource(R.string.grade_range_min), stringResource(R.string.grade_range_max))
                        )
                    }
                } else {
                    items(children) { child ->
                        GradeSliderCard(
                            title = child.componentName, weight = stringResource(R.string.weight_percentage_format, child.weight), icon = IconMapper.Analytics, value = child.score.toFloat(),
                            maxGrade = maxGradeValue, scaleSuffix = scaleSuffix, onValueChange = { onScoreChange(child.componentId, it.toDouble()) }, rangeLabels = listOf(stringResource(R.string.grade_range_min), stringResource(R.string.grade_range_max))
                        )
                    }
                }
            }

            item {
                TonalCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = IconMapper.Notes, contentDescription = null, tint = AppTheme.colors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.teachers_notes), style = AppTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            if (isOcrProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                val activityOwner = LocalActivityResultRegistryOwner.current
                                IconButton(onClick = {
                                    val activity = activityOwner as? Activity
                                    
                                    if (activity != null && scanner != null) {
                                        scanner.getStartScanIntent(activity)
                                            .addOnSuccessListener { intentSender ->
                                                try {
                                                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                                } catch (_: Exception) {
                                                    scope.launch { snackbarHostState.showAppSnackbar(ocrErrorMsg, type = SnackbarType.ERROR) }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                scope.launch { snackbarHostState.showAppSnackbar("${ocrErrorMsg}: ${e.message}", type = SnackbarType.ERROR) }
                                            }
                                    } else {
                                        if (!isInPreview) {
                                            scope.launch { snackbarHostState.showAppSnackbar(activityErrorMsg, type = SnackbarType.ERROR) }
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = IconMapper.Camera,
                                        contentDescription = stringResource(R.string.ocr_scan_button),
                                        tint = AppTheme.colors.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = teacherNotes, onValueChange = onNotesChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.qualitative_feedback_hint)) },
                            minLines = 3,
                            shape = AppTheme.shapes.medium
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = AppTheme.shapes.medium
                ) {
                    Icon(IconMapper.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.save_grades))
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StudentGradingScreen(
    academicViewModel: pt.ipt.dama.TeachersLog.ui.viewmodel.AcademicViewModel,
    viewModel: TeacherViewModel,
    studentId: Int,
    className: String,
    onBackClick: () -> Unit
) {
    var student by remember { mutableStateOf<Student?>(null) }
    var gradingScale by remember { mutableStateOf("5") }
    val componentScoresFromDb by viewModel.getComponentScoresForStudent(studentId).observeAsState(emptyList())
    val currentGrade by viewModel.currentGrade.observeAsState()
    val semesters by academicViewModel.semesters.observeAsState(emptyList())
    val selectedSemesterId by academicViewModel.selectedSemesterId.observeAsState()

    val scoresMap = remember { mutableStateMapOf<Int, Double>() }
    val teacherNotesState = remember { mutableStateOf("") }

    LaunchedEffect(studentId) {
        student = viewModel.getStudentById(studentId)
        gradingScale = viewModel.getGradingScaleForStudent(studentId)
        viewModel.loadGradeForStudent(studentId)
    }

    LaunchedEffect(componentScoresFromDb) {
        componentScoresFromDb.forEach { 
            if (!it.isGroup && !scoresMap.containsKey(it.componentId)) {
                scoresMap[it.componentId] = it.score 
            }
        }
    }

    LaunchedEffect(currentGrade) {
        currentGrade?.let { teacherNotesState.value = it.teacherNotes }
    }

    val componentScores = componentScoresFromDb.map { 
        if (it.isGroup) it 
        else it.copy(score = scoresMap[it.componentId] ?: it.score)
    }

    val currentSemester = semesters.find { it.id == selectedSemesterId }

    StudentGradingScreenContentWrapper(
        student = student,
        className = className,
        currentSemesterName = currentSemester?.name,
        componentScores = componentScores,
        gradingScale = gradingScale,
        teacherNotes = teacherNotesState.value,
        onScoreChange = { id, score -> scoresMap[id] = score },
        onNotesChange = { teacherNotesState.value = it },
        onSave = {
            val toSave = scoresMap.map { (id, score) -> StudentScore(studentId, id, score) }
            viewModel.saveGrade(studentId, toSave, teacherNotesState.value)
            onBackClick()
        },
        onBackClick = onBackClick
    )
}

@Composable
fun StudentGradingScreenContentWrapper(
    student: Student?,
    className: String,
    currentSemesterName: String?,
    componentScores: List<ComponentScore>,
    gradingScale: String,
    teacherNotes: String,
    onScoreChange: (Int, Double) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit
) {
    StudentGradingContent(
        student = student,
        className = className,
        currentSemesterName = currentSemesterName,
        componentScores = componentScores,
        gradingScale = gradingScale,
        teacherNotes = teacherNotes,
        onScoreChange = onScoreChange,
        onNotesChange = onNotesChange,
        onSave = onSave,
        onBackClick = onBackClick
    )
}

@Composable
fun GradeSliderCard(title: String, weight: String, icon: ImageVector, value: Float, maxGrade: Float, scaleSuffix: String, onValueChange: (Float) -> Unit, rangeLabels: List<String>) {
    val locale = LocalConfiguration.current.locales[0]
    TonalCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = AppTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = stringResource(R.string.weight_label, weight), style = AppTheme.typography.labelSmall, color = AppTheme.colors.outline)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = String.format(locale, "%.1f", value), style = AppTheme.typography.headlineMedium, color = AppTheme.colors.primary)
                Text(text = scaleSuffix, style = AppTheme.typography.labelSmall, color = AppTheme.colors.outline)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = 0f..maxGrade, steps = if (maxGrade > 20f) (maxGrade - 1).toInt() else (maxGrade * 10 - 1).toInt())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rangeLabels.forEach { Text(text = it, style = AppTheme.typography.labelSmall, color = AppTheme.colors.outline) }
            }
        }
    }
}
