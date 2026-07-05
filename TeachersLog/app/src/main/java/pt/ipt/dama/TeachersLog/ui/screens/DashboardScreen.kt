package pt.ipt.dama.TeachersLog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.data.model.*
import pt.ipt.dama.TeachersLog.ui.components.*
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.viewmodel.AcademicViewModel
import pt.ipt.dama.TeachersLog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.TeachersLog.utils.IconMapper
import pt.ipt.dama.TeachersLog.utils.LocalizedNameMapper
import androidx.compose.ui.platform.LocalLocale

/**
 * DashboardContent is the stateless UI component for the main landing screen.
 *
 * @param recentClasses List of [StudentsClassWithStats] recently accessed.
 * @param stats [GlobalStats] object containing aggregate academic data.
 * @param isRefreshing Boolean state for the pull-to-refresh indicator.
 * @param isGuest Boolean indicating if user is in guest mode.
 * @param syncEnabled Boolean indicating if sync is enabled.
 * @param onRefresh Callback to trigger a data refresh from the cloud.
 * @param onMenuClick Callback to trigger the side navigation drawer.
 * @param onClassClick Callback when a specific class card is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    recentClasses: List<StudentsClassWithStats>,
    stats: GlobalStats?,
    currentSemesterName: String?,
    isRefreshing: Boolean,
    isGuest: Boolean,
    syncEnabled: Boolean,
    onRefresh: () -> Unit,
    onMenuClick: () -> Unit,
    onClassClick: (Int, String) -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = { 
            TeacherTopBar(
                title = stringResource(R.string.app_name),
                titleIcon = IconMapper.Dashboard,
                onNavigationIconClick = onMenuClick
            ) 
        }
    ) { paddingValues ->
        val content = @Composable {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- Header Section ---
                item {
                    val label = currentSemesterName?.let { 
                        LocalizedNameMapper.getLocalizedSemesterName(it) 
                    } ?: stringResource(R.string.academic_dashboard)
                    
                    SectionHeader(
                        label = label,
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
                            icon = IconMapper.Account,
                            modifier = Modifier.weight(1f)
                        )
                        
                        val avgPercentage = stats?.globalAverage ?: 0.0
                        val avgColor = AppTheme.getGradeColor(avgPercentage)

                        StatCard(
                            title = stringResource(R.string.global_average),
                            value = String.format(LocalLocale.current.platformLocale, "%.1f %%", avgPercentage),
                            icon = IconMapper.Analytics,
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

        Box(modifier = Modifier.padding(paddingValues)) {
            if (isGuest || !syncEnabled) {
                content()
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing,
                            containerColor = AppTheme.colors.surface,
                            color = AppTheme.colors.primary,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * DashboardScreen observed states from both Academic and Teacher ViewModels.
 */
@Composable
fun DashboardScreen(
    academicViewModel: AcademicViewModel,
    teacherViewModel: TeacherViewModel,
    authViewModel: pt.ipt.dama.TeachersLog.ui.viewmodel.AuthViewModel,
    onMenuClick: () -> Unit,
    onClassClick: (Int, String) -> Unit
) {
    val selectedSemesterId by academicViewModel.selectedSemesterId.observeAsState()
    val semesters by academicViewModel.semesters.observeAsState(emptyList())
    val recentClasses by if (selectedSemesterId != null) {
        teacherViewModel.getRecentClassesForPeriod(selectedSemesterId!!).observeAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val stats by academicViewModel.globalStats.observeAsState()
    val isRefreshing by academicViewModel.isRefreshing.collectAsState()
    val isGuest by authViewModel.isGuest.observeAsState(initial = false)
    val syncEnabled by authViewModel.isSyncEnabled.observeAsState(initial = false)

    val currentSemester = semesters.find { it.id == selectedSemesterId }
    
    DashboardContent(
        recentClasses = recentClasses,
        stats = stats,
        currentSemesterName = currentSemester?.name,
        isRefreshing = isRefreshing,
        isGuest = isGuest,
        syncEnabled = syncEnabled,
        onRefresh = { academicViewModel.refreshData() },
        onMenuClick = onMenuClick,
        onClassClick = onClassClick
    )
}
