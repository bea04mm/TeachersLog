/**
 * Main landing screen providing high-level academic statistics.
 * Uses a reactive grid layout to display summary cards and recent activity.
 */
package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.*
import pt.ipt.dama.teacherslog.ui.components.*
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel

/**
 * Encapsulates the visual content of the Dashboard.
 * Separated for easier previewing and testing.
 */
@Composable
fun DashboardContent(
    subjects: List<SubjectWithStats>,
    stats: GlobalStats?,
    onMenuClick: () -> Unit,
    onSubjectClick: (Int, String) -> Unit,
    onViewAllSubjectsClick: () -> Unit,
    onRandomStudentClick: () -> Unit
) {
    Scaffold(
        topBar = { 
            TeacherTopBar(
                title = stringResource(R.string.app_name),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)), 
                onNavigationIconClick = onMenuClick
            ) 
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRandomStudentClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.pick_random_student))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.academic_dashboard),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.active_overview),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.total_students),
                        value = (stats?.totalStudents ?: 0).toString(),
                        icon = Icons.Default.Group,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.global_average),
                        value = String.format(Locale.getDefault(), "%.2f", stats?.globalAverage ?: 0.0),
                        icon = Icons.Default.BarChart,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.active_subjects),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        TextButton(onClick = onViewAllSubjectsClick) {
                            Text(stringResource(R.string.view_all), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    subjects.forEach { subject ->
                        SubjectCard(
                            name = subject.name,
                            average = String.format(Locale.getDefault(), "%.2f", subject.averageGrade),
                            students = subject.studentCount,
                            color = Color(subject.colorHex.toColorInt()),
                            icon = when(subject.iconName) {
                                "functions" -> Icons.Default.Functions
                                "biotech" -> Icons.Default.Biotech
                                else -> Icons.Default.HistoryEdu
                            },
                            modifier = Modifier.padding(vertical = 6.dp),
                            onClick = { onSubjectClick(subject.id, subject.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: TeacherViewModel,
    onMenuClick: () -> Unit,
    onSubjectClick: (Int, String) -> Unit,
    onViewAllSubjectsClick: () -> Unit,
    onRandomStudentClick: () -> Unit
) {
    val subjects by viewModel.subjectsWithStats.observeAsState(emptyList())
    val stats by viewModel.globalStats.observeAsState()
    
    DashboardContent(
        subjects = subjects,
        stats = stats,
        onMenuClick = onMenuClick,
        onSubjectClick = onSubjectClick,
        onViewAllSubjectsClick = onViewAllSubjectsClick,
        onRandomStudentClick = onRandomStudentClick
    )
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    TeachersLogTheme {
        DashboardContent(
            subjects = listOf(
                SubjectWithStats(1, "Mathematics", "functions", "#004AC6", 12, 4.5),
                SubjectWithStats(2, "Science", "biotech", "#006686", 8, 3.8)
            ),
            stats = GlobalStats(20, 2, 4.1),
            onMenuClick = {},
            onSubjectClick = { _, _ -> },
            onViewAllSubjectsClick = {},
            onRandomStudentClick = {}
        )
    }
}
