package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.*
import pt.ipt.dama.teacherslog.ui.components.*
import pt.ipt.dama.teacherslog.ui.theme.*
import pt.ipt.dama.teacherslog.ui.viewmodel.AcademicViewModel
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.teacherslog.utils.IconMapper

/**
 * DashboardContent is the stateless UI component for the main landing screen.
 *
 * @param recentClasses List of [StudentsClassWithStats] recently accessed.
 * @param stats [GlobalStats] object containing aggregate academic data.
 * @param onMenuClick Callback to trigger the side navigation drawer.
 * @param onClassClick Callback when a specific class card is selected.
 */
@Composable
fun DashboardContent(
    recentClasses: List<StudentsClassWithStats>,
    stats: GlobalStats?,
    onMenuClick: () -> Unit,
    onClassClick: (Int, String) -> Unit
) {
    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = { 
            // Branding TopBar with drawer support
            TeacherTopBar(
                title = stringResource(R.string.app_name),
                titleIcon = IconMapper.Dashboard,
                onNavigationIconClick = onMenuClick
            ) 
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
                SectionHeader(
                    label = stringResource(R.string.academic_dashboard),
                    title = stringResource(R.string.active_overview)
                )
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
                    // Calculate semantic color for global average using Theme extension
                    val avgPercentage = stats?.globalAverage ?: 0.0
                    val avgColor = when {
                        avgPercentage >= 80.0 -> AppTheme.success
                        avgPercentage >= 50.0 -> AppTheme.warning
                        else -> AppTheme.critical
                    }

                    StatCard(
                        title = stringResource(R.string.global_average),
                        value = String.format(Locale.getDefault(), "%.1f %%", avgPercentage),
                        icon = Icons.Default.BarChart,
                        modifier = Modifier.weight(1f),
                        color = avgColor
                    )
                }
            }

            // --- Recent Classes Section ---
            item {
                Column {
                    Text(
                        text = stringResource(R.string.recent_classes_title),
                        style = AppTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (recentClasses.isEmpty()) {
                        TonalCard {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.no_recent_classes), 
                                    style = AppTheme.typography.bodyMedium,
                                    color = AppTheme.colors.outline
                                )
                            }
                        }
                    } else {
                        recentClasses.forEach { clazz ->
                            ClassCard(
                                name = clazz.name,
                                studentCount = clazz.studentCount,
                                averageGrade = clazz.averageGrade,
                                color = AppTheme.parseColor(clazz.colorHex),
                                gradingScale = clazz.gradingScale,
                                iconName = clazz.iconName,
                                modifier = Modifier.padding(vertical = 6.dp),
                                onClick = { onClassClick(clazz.id, clazz.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * DashboardScreen observed states from both Academic and Teacher ViewModels.
 *
 * @param academicViewModel ViewModel managing academic hierarchy.
 * @param teacherViewModel ViewModel managing operational class data.
 * @param onMenuClick Callback to open the navigation drawer.
 * @param onClassClick Callback to navigate to a specific class.
 */
@Composable
fun DashboardScreen(
    academicViewModel: AcademicViewModel,
    teacherViewModel: TeacherViewModel,
    onMenuClick: () -> Unit,
    onClassClick: (Int, String) -> Unit
) {
    val selectedSemesterId by academicViewModel.selectedSemesterId.observeAsState()
    val recentClasses by if (selectedSemesterId != null) {
        teacherViewModel.getRecentClassesForPeriod(selectedSemesterId!!).observeAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val stats by academicViewModel.globalStats.observeAsState()
    
    DashboardContent(
        recentClasses = recentClasses,
        stats = stats,
        onMenuClick = onMenuClick,
        onClassClick = onClassClick
    )
}
