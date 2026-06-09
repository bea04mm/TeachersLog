package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.StudentWithGrade
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel

@Composable
fun ClassRosterContent(
    className: String,
    students: List<StudentWithGrade>,
    onStudentClick: (Int, String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TeacherTopBar(
                title = className,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.students_roster),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = className,
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(students) { student ->
                val gradeColor = when {
                    student.weightedGrade >= 4.0 -> Color(0xFF22C55E)
                    student.weightedGrade >= 3.0 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }

                TonalCard(
                    modifier = Modifier.clickable { onStudentClick(student.studentId, student.name) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = student.numberInGroup.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = student.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            student.processNumber?.let {
                                Text(
                                    text = stringResource(R.string.process_prefix, it), 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = gradeColor.copy(alpha = 0.1f),
                                shape = CircleShape,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.2f", student.weightedGrade),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = gradeColor,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
        onBackClick = onBackClick
    )
}
