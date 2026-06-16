package pt.ipt.dama.teacherslog

import android.content.Context
import android.os.Bundle
import java.net.URLEncoder
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import pt.ipt.dama.teacherslog.data.model.AcademicYear
import pt.ipt.dama.teacherslog.data.model.Semester
import pt.ipt.dama.teacherslog.ui.components.TeacherBottomBar
import pt.ipt.dama.teacherslog.ui.components.TeacherDrawerContent
import pt.ipt.dama.teacherslog.ui.components.AddYearAndPeriodDialog
import pt.ipt.dama.teacherslog.ui.screens.*
import pt.ipt.dama.teacherslog.ui.theme.*
import pt.ipt.dama.teacherslog.ui.viewmodel.*
import pt.ipt.dama.teacherslog.utils.LocaleManager

/**
 * MainActivity is the primary entry point for the Teacher's Log application.
 * 
 * It manages the initialization of domain-specific ViewModels, handles global 
 * configuration such as language and theme, and provides the top-level 
 * Composable structure of the app.
 */
class MainActivity : ComponentActivity() {
    
    // Domain-specific ViewModels initialized via shared factories
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModelFactory(application) }
    private val academicViewModel: AcademicViewModel by viewModels { AcademicViewModelFactory(application) }
    private val teacherViewModel: TeacherViewModel by viewModels { TeacherViewModelFactory(application) }

    /**
     * Attaches the base context with the persisted language preference.
     * 
     * @param newBase The underlying context from the Android system.
     */
    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("prefs", MODE_PRIVATE)
            .getString("language", "en") ?: "en"
        super.attachBaseContext(LocaleManager.updateResources(newBase, lang))
    }

    /**
     * Called when the activity is first created. Sets up the Compose content view.
     * 
     * @param savedInstanceState Saved state from a previous instance of this activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            // Observe global state from the specialized SettingsViewModel
            val currentLang by settingsViewModel.currentLanguage.observeAsState(initial = LocaleManager.getLanguage(this))
            val isDarkMode by settingsViewModel.isDarkMode.observeAsState(initial = true)
            
            // Provide context-aware locale resources and activity result registry to the tree
            CompositionLocalProvider(
                LocalContext provides LocaleManager.updateResources(this, currentLang),
                LocalActivityResultRegistryOwner provides this
            ) {
                TeachersLogTheme(darkTheme = isDarkMode) {
                    MainApp(settingsViewModel, academicViewModel, teacherViewModel)
                }
            }
        }
    }
}

/**
 * MainApp orchestrates the top-level navigation, the side drawer, and global UI components.
 * 
 * It manages the central [NavHost], the [Scaffold] with its bottom bar, and handles 
 * the visibility of global academic management dialogs.
 * 
 * @param settingsViewModel View model for application-wide settings (Theme, Locale).
 * @param academicViewModel View model for the academic hierarchy (Years, Periods).
 * @param teacherViewModel View model for operational data (Classes, Students, Grades).
 */
@Composable
fun MainApp(
    settingsViewModel: SettingsViewModel,
    academicViewModel: AcademicViewModel,
    teacherViewModel: TeacherViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Global academic state observation
    val academicYears by academicViewModel.allAcademicYears.observeAsState(emptyList())
    val selectedYearId by academicViewModel.selectedYearId.observeAsState()
    val semesters by academicViewModel.semesters.observeAsState(emptyList())
    val selectedSemesterId by academicViewModel.selectedSemesterId.observeAsState()

    // Dialog and deletion visibility states
    val showAddYearDialog = remember { mutableStateOf(false) }
    val showAddPeriodDialog = remember { mutableStateOf(false) }
    val yearToDelete = remember { mutableStateOf<AcademicYear?>(null) }
    val periodToDelete = remember { mutableStateOf<Semester?>(null) }

    // --- Global Dialog Logic ---

    // Atomic dialog for adding a new Year + its initial Period
    if (showAddYearDialog.value) {
        AddYearAndPeriodDialog(
            onDismiss = { showAddYearDialog.value = false },
            onAdd = { yearName, periodName ->
                academicViewModel.addYearWithPeriod(yearName, periodName)
                showAddYearDialog.value = false
            },
            existingYears = academicYears
        )
    }

    // Dialog for adding additional periods to an existing year
    if (showAddPeriodDialog.value) {
        val allOptions = listOf(
            R.string.period_1st_semester, R.string.period_2nd_semester,
            R.string.period_1st_period, R.string.period_2nd_period,
            R.string.period_3rd_period, R.string.period_annual
        )
        val existingNames = semesters.map { it.name }.toSet()
        val available = allOptions.filter { stringResource(it) !in existingNames }
        
        if (available.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showAddPeriodDialog.value = false },
                title = { Text(stringResource(R.string.add_period)) },
                text = { Text(text = stringResource(R.string.all_periods_registered)) },
                confirmButton = { Button(onClick = { showAddPeriodDialog.value = false }) { Text(stringResource(R.string.close)) } }
            )
        } else {
            var selectedOption by remember { mutableIntStateOf(available[0]) }
            AlertDialog(
                onDismissRequest = { showAddPeriodDialog.value = false },
                title = { Text(stringResource(R.string.add_period)) },
                text = {
                    Column {
                        Text(text = stringResource(R.string.select_period_type), style = AppTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                        available.forEach { optionRes ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier.fillMaxWidth().clickable { selectedOption = optionRes }.padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = (selectedOption == optionRes), onClick = { selectedOption = optionRes })
                                Text(text = stringResource(optionRes), modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = { academicViewModel.addSemester(stringResource(selectedOption)); showAddPeriodDialog.value = false },
                dismissButton = { TextButton(onClick = { showAddPeriodDialog.value = false }) { Text(stringResource(R.string.cancel)) } }
            )
        }
    }

    // Confirmation dialog for deleting an academic year
    yearToDelete.value?.let { year ->
        AlertDialog(
            onDismissRequest = { yearToDelete.value = null },
            title = { Text(stringResource(R.string.delete_year_title)) },
            text = { Text(stringResource(R.string.delete_year_message, year.year)) },
            confirmButton = {
                Button(
                    onClick = { academicViewModel.deleteAcademicYear(year); yearToDelete.value = null }, 
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.error)
                ) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = { TextButton(onClick = { yearToDelete.value = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // Confirmation dialog for deleting an academic period
    periodToDelete.value?.let { semester ->
        AlertDialog(
            onDismissRequest = { periodToDelete.value = null },
            title = { Text(stringResource(R.string.delete_period_title)) },
            text = { Text(stringResource(R.string.delete_period_message, semester.name)) },
            confirmButton = {
                Button(
                    onClick = { academicViewModel.deleteSemester(semester); periodToDelete.value = null }, 
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.error)
                ) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = { TextButton(onClick = { periodToDelete.value = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // --- Main Layout Structure ---

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            TeacherDrawerContent(
                academicYears = academicYears,
                selectedYearId = selectedYearId,
                semesters = semesters,
                selectedSemesterId = selectedSemesterId,
                onYearSelect = { academicViewModel.selectYear(it); scope.launch { drawerState.close() } },
                onYearDelete = { yearToDelete.value = it },
                onAddYear = { showAddYearDialog.value = true },
                onSemesterSelect = { academicViewModel.selectSemester(it); scope.launch { drawerState.close() } },
                onSemesterDelete = { periodToDelete.value = it },
                onAddPeriod = { showAddPeriodDialog.value = true }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Show navigation bar only on top-level screens
                if (currentRoute in listOf("dashboard", "subjects", "settings")) {
                    TeacherBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { navController.navigate(it) { 
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true 
                        } }
                    )
                }
            }
        ) { innerPadding ->
            // --- Application Navigation Graph ---
            NavHost(
                navController = navController, 
                startDestination = "dashboard", 
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        academicViewModel = academicViewModel,
                        teacherViewModel = teacherViewModel,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onClassClick = { id, name -> navController.navigate("students_class/$id/${URLEncoder.encode(name, "UTF-8")}") }
                    )
                }
                composable("subjects") {
                    SubjectsListScreen(
                        academicViewModel = academicViewModel,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSubjectClick = { id, name -> navController.navigate("subject/$id/${URLEncoder.encode(name, "UTF-8")}") },
                        onAddSubjectClick = { navController.navigate("add_subject") },
                        onEditSubjectClick = { id -> navController.navigate("edit_subject/$id") }
                    )
                }
                composable("add_subject") { 
                    SubjectEditScreen(academicViewModel, null) { navController.popBackStack() } 
                }
                composable("edit_subject/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { 
                    SubjectEditScreen(academicViewModel, it.arguments?.getInt("id")) { navController.popBackStack() }
                }
                composable("settings") { 
                    SettingsScreen(
                        onNavigateToAccount = { navController.navigate("account") }, 
                        onNavigateToPreferences = { navController.navigate("preferences") }
                    ) 
                }
                composable("preferences") { AppPreferencesScreen(settingsViewModel) { navController.popBackStack() } }
                composable("account") { AccountDetailsScreen { navController.popBackStack() } }
                
                composable("subject/{id}/{name}", arguments = listOf(navArgument("id") { type = NavType.IntType }, navArgument("name") { type = NavType.StringType })) { 
                    SubjectScreen(
                        academicViewModel = academicViewModel,
                        teacherViewModel = teacherViewModel,
                        subjectId = it.arguments?.getInt("id") ?: 0,
                        subjectName = it.arguments?.getString("name") ?: "",
                        onClassClick = { id, name -> navController.navigate("students_class/$id/${URLEncoder.encode(name, "UTF-8")}") },
                        onAddClassClick = { sid -> navController.navigate("add_class/$sid") },
                        onEditClassClick = { sid, cid -> navController.navigate("edit_class/$sid/$cid") },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                
                composable("add_class/{subjectId}", arguments = listOf(navArgument("subjectId") { type = NavType.IntType })) { 
                    ClassEditScreen(teacherViewModel, it.arguments?.getInt("subjectId") ?: 0, null) { navController.popBackStack() }
                }
                
                composable("edit_class/{subjectId}/{classId}", arguments = listOf(navArgument("subjectId") { type = NavType.IntType }, navArgument("classId") { type = NavType.IntType })) { 
                    ClassEditScreen(teacherViewModel, it.arguments?.getInt("subjectId") ?: 0, it.arguments?.getInt("classId")) { navController.popBackStack() }
                }
                
                composable("students_class/{id}/{name}", arguments = listOf(navArgument("id") { type = NavType.IntType }, navArgument("name") { type = NavType.StringType })) { 
                    ClassRosterScreen(
                        viewModel = teacherViewModel,
                        studentsClassId = it.arguments?.getInt("id") ?: 0,
                        className = it.arguments?.getString("name") ?: "",
                        onStudentClick = { id, name -> navController.navigate("grading/$id/${URLEncoder.encode(name, "UTF-8")}/${URLEncoder.encode(it.arguments?.getString("name") ?: "", "UTF-8")}") },
                        onAddStudentClick = { cid -> navController.navigate("add_student/$cid") },
                        onEditStudentClick = { cid, sid -> navController.navigate("edit_student/$cid/$sid") },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                
                composable("add_student/{classId}", arguments = listOf(navArgument("classId") { type = NavType.IntType })) { 
                    StudentEditScreen(teacherViewModel, it.arguments?.getInt("classId") ?: 0, null) { navController.popBackStack() }
                }
                
                composable("edit_student/{classId}/{studentId}", arguments = listOf(navArgument("classId") { type = NavType.IntType }, navArgument("studentId") { type = NavType.IntType })) { 
                    StudentEditScreen(teacherViewModel, it.arguments?.getInt("classId") ?: 0, it.arguments?.getInt("studentId")) { navController.popBackStack() }
                }

                composable("grading/{id}/{name}/{sub}", arguments = listOf(navArgument("id") { type = NavType.IntType }, navArgument("name") { type = NavType.StringType }, navArgument("sub") { type = NavType.StringType })) {
                    StudentGradingScreen(teacherViewModel, it.arguments?.getInt("id") ?: 0, it.arguments?.getString("sub") ?: "") { navController.popBackStack() }
                }
            }
        }
    }
}
