package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.Grade
import pt.ipt.dama.teacherslog.data.model.Student
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel

@Composable
fun StudentGradingScreen(
    viewModel: TeacherViewModel,
    studentId: Int,
    className: String,
    onBackClick: () -> Unit
) {
    var student by remember { mutableStateOf<Student?>(null) }
    val currentGrade by viewModel.currentGrade.observeAsState()

    var homeworkGrade by remember { mutableFloatStateOf(0f) }
    var participationGrade by remember { mutableIntStateOf(0) }
    var quizzesGrade by remember { mutableFloatStateOf(0f) }
    var projectsGrade by remember { mutableFloatStateOf(0f) }
    var notes by remember { mutableStateOf("") }

    // Calculate total grade with weights: HW(20%), Part(10%), Quizzes(40%), Projects(30%)
    val totalGrade = (homeworkGrade * 0.2f) + (participationGrade.toFloat() * 0.1f) + (quizzesGrade * 0.4f) + (projectsGrade * 0.3f)

    val gradeColor = when {
        totalGrade >= 4.0 -> Color(0xFF22C55E) // Green
        totalGrade >= 3.0 -> Color(0xFFF59E0B) // Orange
        else -> Color(0xFFEF4444) // Red
    }

    LaunchedEffect(studentId) {
        student = viewModel.getStudentById(studentId)
        viewModel.loadGradeForStudent(studentId)
    }

    LaunchedEffect(currentGrade) {
        currentGrade?.let {
            homeworkGrade = it.homework.toFloat()
            participationGrade = it.participation
            quizzesGrade = it.quizzes.toFloat()
            projectsGrade = it.projects.toFloat()
            notes = it.teacherNotes
        }
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
            item {
                TonalCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = student?.numberInGroup?.toString() ?: "-",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = student?.name ?: stringResource(R.string.loading), style = MaterialTheme.typography.headlineMedium)
                            student?.processNumber?.let {
                                Text(
                                    text = stringResource(R.string.process_prefix, it), 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(text = className, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                TonalCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.total_grade_weighted),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", totalGrade),
                                style = MaterialTheme.typography.displayLarge,
                                color = gradeColor,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = stringResource(R.string.max_grade), 
                                style = MaterialTheme.typography.headlineSmall, 
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                GradeSliderCard(
                    title = stringResource(R.string.homework_assignments),
                    weight = "20%",
                    icon = Icons.Default.HomeWork,
                    value = homeworkGrade,
                    onValueChange = { homeworkGrade = it },
                    rangeLabels = listOf(stringResource(R.string.low_label), stringResource(R.string.mid_label), stringResource(R.string.high_label))
                )
            }

            item {
                ParticipationGradeCard(
                    title = stringResource(R.string.classroom_participation),
                    weight = "10%",
                    value = participationGrade,
                    onValueChange = { participationGrade = it }
                )
            }

            item {
                GradeSliderCard(
                    title = stringResource(R.string.quizzes_tests),
                    weight = "40%",
                    icon = Icons.Default.Quiz,
                    value = quizzesGrade,
                    onValueChange = { quizzesGrade = it },
                    rangeLabels = listOf("1", "2", "3", "4", "5")
                )
            }

            item {
                GradeSliderCard(
                    title = stringResource(R.string.collaborative_projects),
                    weight = "30%",
                    icon = Icons.Default.Groups,
                    value = projectsGrade,
                    onValueChange = { projectsGrade = it },
                    rangeLabels = listOf(stringResource(R.string.needs_focus_label), stringResource(R.string.standard_label), stringResource(R.string.exemplary_label))
                )
            }

            item {
                TonalCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.teachers_notes), style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.qualitative_feedback_hint)) },
                            minLines = 3,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val newGrade = Grade(
                            studentId = studentId,
                            homework = homeworkGrade.toDouble(),
                            participation = participationGrade,
                            quizzes = quizzesGrade.toDouble(),
                            projects = projectsGrade.toDouble(),
                            teacherNotes = notes
                        )
                        viewModel.saveGrade(newGrade)
                        onBackClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
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

@Composable
fun GradeSliderCard(
    title: String,
    weight: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    rangeLabels: List<String>
) {
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
                Text(text = stringResource(R.string.max_grade), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.weight(1f))
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..5f,
                steps = 49 // allows 0.1 increments
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rangeLabels.forEach { label ->
                    Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
fun ParticipationGradeCard(
    title: String,
    weight: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    TonalCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = stringResource(R.string.weight_label, weight), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..5).forEach { stars ->
                    IconButton(onClick = { onValueChange(stars) }) {
                        Icon(
                            imageVector = if (stars <= value) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (stars <= value) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(text = "$value / 5", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
