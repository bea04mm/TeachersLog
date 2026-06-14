/**
 * Main landing screen providing high-level academic statistics.
 * Uses a reactive grid layout to display summary cards and recent activity.
 */
package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
 * DashboardContent is the stateless UI component for the main landing screen.
 *
 * It manages:
 * 1. A top-level summary of academic statistics (total students, global average).
 * 2. A list of active subjects for the selected year.
 * 3. A random student selector button (FAB).
 *
 * @param subjects List of [SubjectWithStats] to be displayed as cards.
 * @param stats [GlobalStats] object containing aggregate academic data.
 * @param onMenuClick Callback to trigger the side navigation drawer.
 * @param onSubjectClick Callback when a specific subject card is selected.
 * @param onViewAllSubjectsClick Callback to navigate to the full subjects list.
 * @param onRandomStudentClick Callback to trigger the random student selection logic.
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
            // branding TopBar with drawer support
            TeacherTopBar(
                title = stringResource(R.string.app_name),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)), 
                onNavigationIconClick = onMenuClick
            ) 
        },
        floatingActionButton = {
            // FAB for picking a random student from the current classes
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
            // --- Header Section ---
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

            // --- Stats Summary Section ---
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
                        value = String.format(Locale.getDefault(), "%.1f %%", stats?.globalAverage ?: 0.0),
                        icon = Icons.Default.BarChart,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // --- Active Subjects Section ---
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
                    
                    // Render a card for each subject
                    subjects.forEach { subject ->
                        SubjectCard(
                            name = subject.name,
                            average = String.format(Locale.getDefault(), "%.2f", subject.averageGrade),
                            students = subject.studentCount,
                            color = Color(subject.colorHex.toColorInt()),
                            icon = when(subject.iconName) {
                                "functions" -> Icons.Default.Functions
                                "biotech" -> Icons.Default.Biotech
                                "history_edu" -> Icons.Default.HistoryEdu
                                "language" -> Icons.Default.Language
                                "brush" -> Icons.Default.Brush
                                "calculate" -> Icons.Default.Calculate
                                "code" -> Icons.Default.Code
                                "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
                                "music_note" -> Icons.Default.MusicNote
                                "sports_basketball" -> Icons.Default.SportsBasketball
                                else -> Icons.Default.HistoryEdu
                            },
                            gradingScale = subject.gradingScale,
                            modifier = Modifier.padding(vertical = 6.dp),
                            onClick = { onSubjectClick(subject.id, subject.name) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * DashboardScreen is the stateful Composable for the landing screen.
 *
 * It observes global academic statistics and subjects from the [TeacherViewModel]
 * to keep the Dashboard updated.
 *
 * @param viewModel The shared [TeacherViewModel] providing data and logic.
 * @param onMenuClick Callback to open the navigation drawer.
 * @param onSubjectClick Callback to navigate to a specific subject detail view.
 * @param onViewAllSubjectsClick Callback to navigate to the subjects list.
 * @param onRandomStudentClick Callback to execute the random student selection.
 */
@Composable
fun DashboardScreen(
    viewModel: TeacherViewModel,
    onMenuClick: () -> Unit,
    onSubjectClick: (Int, String) -> Unit,
    onViewAllSubjectsClick: () -> Unit,
    onRandomStudentClick: () -> Unit
) {
    // Observe state from ViewModel
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

/**
 * Preview for the [DashboardContent] UI in light mode.
 */
@Preview(showBackground = true, name = "Dashboard Light Mode")
@Composable
fun DashboardPreview() {
    TeachersLogTheme(darkTheme = false) {
        DashboardContent(
            subjects = listOf(
                SubjectWithStats(1, "Mathematics", "functions", "#004AC6", 12, 4.5, "5"),
                SubjectWithStats(2, "Science", "biotech", "#006686", 8, 3.8, "20")
            ),
            stats = GlobalStats(20, 2, 4.1),
            onMenuClick = {},
            onSubjectClick = { _, _ -> },
            onViewAllSubjectsClick = {},
            onRandomStudentClick = {}
        )
    }
}

/**
 * Preview for the [DashboardContent] UI in dark mode.
 */
@Preview(showBackground = true, name = "Dashboard Dark Mode")
@Composable
fun DashboardDarkPreview() {
    TeachersLogTheme(darkTheme = true) {
        DashboardContent(
            subjects = listOf(
                SubjectWithStats(1, "Mathematics", "functions", "#004AC6", 12, 4.5, "5"),
                SubjectWithStats(2, "Science", "biotech", "#006686", 8, 3.8, "20")
            ),
            stats = GlobalStats(20, 2, 4.1),
            onMenuClick = {},
            onSubjectClick = { _, _ -> },
            onViewAllSubjectsClick = {},
            onRandomStudentClick = {}
        )
    }
}
