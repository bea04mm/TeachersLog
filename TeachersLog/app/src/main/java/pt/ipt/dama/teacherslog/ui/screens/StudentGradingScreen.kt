package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.*
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Stateless UI component for student grading.
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
    componentScores: List<ComponentScore>,
    gradingScale: String,
    teacherNotes: String,
    onScoreChange: (Int, Double) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit
) {
    // Derive max grade and suffix from the class scale
    val maxGradeValue = remember(gradingScale) {
        when(gradingScale) { "20" -> 20f; "100" -> 100f; else -> 5f }
    }
    val scaleSuffix = remember(gradingScale) {
        when(gradingScale) { "20" -> " / 20"; "100" -> " %"; else -> " / 5.0" }
    }

    // Weighted average logic supporting hierarchy: sum of (score * absoluteWeight)
    val totalWeightedGrade = componentScores.filter { !it.isGroup }.sumOf { 
        it.score * (it.absoluteWeight / 100.0) 
    }

    val percentage = (totalWeightedGrade.toFloat() / maxGradeValue) * 100f
    val gradeColor = when {
        percentage >= 80f -> Color(0xFF22C55E) // Positive
        percentage >= 50f -> Color(0xFFF59E0B) // Warning
        else -> Color(0xFFEF4444) // Critical
    }

    Scaffold(
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
            // --- Student Header ---
            item {
                TonalCard {
                    Row(
                        modifier = Modifier.padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Profile Photo and Name on the left
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                if (student?.profileImageUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(student.profileImageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(text = student?.name ?: stringResource(R.string.loading), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                student?.processNumber?.let { 
                                    Text(text = stringResource(R.string.process_prefix, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) 
                                }
                            }
                        }
                        
                        // Student Number and Class on the right
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = student?.numberInGroup?.toString() ?: "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = className, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // --- Calculated Total Display ---
            item {
                TonalCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.total_grade_weighted), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = String.format(Locale.getDefault(), "%.2f", totalWeightedGrade), style = MaterialTheme.typography.displayLarge, color = gradeColor, fontWeight = FontWeight.Black)
                            Text(text = scaleSuffix, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                        }
                    }
                }
            }

            // --- Hierarchical Component Sliders ---
            val rootComponents = componentScores.filter { it.parentId == null }
            rootComponents.forEach { group ->
                item {
                    // Category Header with Total Group Weight
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = group.componentName.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(text = "${group.weight}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                val children = componentScores.filter { it.parentId == group.componentId }
                if (children.isEmpty()) {
                    // Standalone component (no children)
                    item {
                        GradeSliderCard(
                            title = group.componentName, weight = "${group.weight}%", icon = Icons.Default.Analytics, value = group.score.toFloat(), 
                            maxGrade = maxGradeValue, scaleSuffix = scaleSuffix, onValueChange = { onScoreChange(group.componentId, it.toDouble()) }, rangeLabels = listOf("0", "Max")
                        )
                    }
                } else {
                    // Group with sub-items
                    items(children) { child ->
                        GradeSliderCard(
                            title = child.componentName, weight = "${child.weight}%", icon = Icons.Default.Analytics, value = child.score.toFloat(),
                            maxGrade = maxGradeValue, scaleSuffix = scaleSuffix, onValueChange = { onScoreChange(child.componentId, it.toDouble()) }, rangeLabels = listOf("0", "Max")
                        )
                    }
                }
            }

            // --- Qualitative Feedback ---
            item {
                TonalCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.teachers_notes), style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = teacherNotes, onValueChange = onNotesChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.qualitative_feedback_hint)) },
                            minLines = 3
                        )
                    }
                }
            }

            // --- Save Action ---
            item {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.save_grades))
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * StudentGradingScreen is the stateful container for the student evaluation UI.
 */
@Composable
fun StudentGradingScreen(
    viewModel: TeacherViewModel,
    studentId: Int,
    className: String,
    onBackClick: () -> Unit
) {
    var student by remember { mutableStateOf<Student?>(null) }
    var gradingScale by remember { mutableStateOf("5") }
    val componentScoresFromDb by viewModel.getComponentScoresForStudent(studentId).observeAsState(emptyList())
    val currentGrade by viewModel.currentGrade.observeAsState()

    // Map to hold current UI score state: ComponentID -> Score
    val scoresMap = remember { mutableStateMapOf<Int, Double>() }
    val teacherNotesState = remember { mutableStateOf("") }

    // Initial data load
    LaunchedEffect(studentId) {
        student = viewModel.getStudentById(studentId)
        gradingScale = viewModel.getGradingScaleForStudent(studentId)
        viewModel.loadGradeForStudent(studentId)
    }

    // Sync state map when database scores are loaded
    LaunchedEffect(componentScoresFromDb) {
        componentScoresFromDb.forEach { 
            if (!it.isGroup && !scoresMap.containsKey(it.componentId)) {
                scoresMap[it.componentId] = it.score 
            }
        }
    }

    // Sync qualitative notes
    LaunchedEffect(currentGrade) {
        currentGrade?.let { teacherNotesState.value = it.teacherNotes }
    }

    // Transform raw scores from DB into a list that respects local UI edits
    val componentScores = componentScoresFromDb.map { 
        if (it.isGroup) it 
        else it.copy(score = scoresMap[it.componentId] ?: it.score)
    }

    StudentGradingContent(
        student = student,
        className = className,
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

/**
 * Reusable card with a slider for individual grade components.
 *
 * @param title Label for the criteria.
 * @param weight Percentage string.
 * @param icon Contextual icon.
 * @param value Current numerical value.
 * @param maxGrade Upper scale limit.
 * @param scaleSuffix Unit string.
 * @param onValueChange Callback for slider updates.
 * @param rangeLabels Visualization labels for low/high bounds.
 */
@Composable
fun GradeSliderCard(title: String, weight: String, icon: ImageVector, value: Float, maxGrade: Float, scaleSuffix: String, onValueChange: (Float) -> Unit, rangeLabels: List<String>) {
    TonalCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = stringResource(R.string.weight_label, weight), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = String.format(Locale.getDefault(), "%.1f", value), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = scaleSuffix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = 0f..maxGrade, steps = if (maxGrade > 20f) (maxGrade - 1).toInt() else (maxGrade * 10 - 1).toInt())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rangeLabels.forEach { Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
            }
        }
    }
}
