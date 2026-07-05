package pt.ipt.dama.TeachersLog

import android.content.Context
import android.os.Bundle
import java.net.URLEncoder
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
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
import pt.ipt.dama.TeachersLog.data.model.AcademicYear
import pt.ipt.dama.TeachersLog.data.model.Semester
import pt.ipt.dama.TeachersLog.ui.components.TeacherBottomBar
import pt.ipt.dama.TeachersLog.ui.components.TeacherDrawerContent
import pt.ipt.dama.TeachersLog.ui.components.AddYearAndPeriodDialog
import pt.ipt.dama.TeachersLog.ui.components.CloneYearDialog
import pt.ipt.dama.TeachersLog.ui.components.CloneSemesterDestinationDialog
import pt.ipt.dama.TeachersLog.ui.components.AppSnackbar
import pt.ipt.dama.TeachersLog.ui.components.LocalSnackbarHostState
import pt.ipt.dama.TeachersLog.ui.components.SnackbarType
import pt.ipt.dama.TeachersLog.ui.components.showAppSnackbar
import pt.ipt.dama.TeachersLog.ui.screens.*
import pt.ipt.dama.TeachersLog.ui.screens.auth.OnboardingScreen
import pt.ipt.dama.TeachersLog.ui.screens.auth.WelcomeScreen
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.viewmodel.*
import pt.ipt.dama.TeachersLog.utils.LocaleManager
import pt.ipt.dama.TeachersLog.utils.LocalizedNameMapper

/**
 * MainActivity is the primary entry point for the Teacher's Log application.
 * 
 * It manages the initialization of domain-specific ViewModels, handles global 
 * configuration such as language and theme, and provides the top-level 
 * Composable structure of the app.
 */
class MainActivity : ComponentActivity() {
    
    // Domain-specific ViewModels initialized via shared factories
    private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory(application) }
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
            .getString("language", java.util.Locale.getDefault().language) ?: java.util.Locale.getDefault().language
        super.attachBaseContext(LocaleManager.updateResources(newBase, lang))
    }

    /**
     * Called when the activity is first created. Sets up the Compose content view.
     * 
     * @param savedInstanceState Saved state from a previous instance of this activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Ensure fully transparent system bars
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        
        // Keep splash screen visible until initial state is loaded
        splashScreen.setKeepOnScreenCondition { authViewModel.isLoading.value }
        
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            // Observe global state from ViewModels
            val currentLang by settingsViewModel.currentLanguage.observeAsState(initial = LocaleManager.getLanguage(this))
            val isDarkModePref by settingsViewModel.isDarkMode.observeAsState(initial = null)
            val isDarkMode = isDarkModePref ?: isSystemInDarkTheme()
            
            // Handle edge-to-edge system bars style reactively
            SideEffect {
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkMode }
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }

            val onboardingCompleted by authViewModel.onboardingCompletedFlow.collectAsState(initial = false)
            val isAuthenticated by authViewModel.isAuthenticatedFlow.collectAsState(initial = false)
            
            // Log for debugging (only in debug builds)
            LaunchedEffect(onboardingCompleted, isAuthenticated) {
                android.util.Log.d("TeachersLog", "onboardingCompleted: $onboardingCompleted, isAuthenticated: $isAuthenticated")
            }

            TeachersLogTheme(darkTheme = isDarkMode) {
                // Use a key to force complete UI tree refresh when language changes
                key(currentLang) {
                    val localizedContext = remember(currentLang) {
                        LocaleManager.updateResources(this@MainActivity, currentLang)
                    }
                    
                    CompositionLocalProvider(
                        LocalContext provides localizedContext,
                        LocalActivityResultRegistryOwner provides this@MainActivity,
                        LocalSnackbarHostState provides snackbarHostState
                    ) {
                        when {
                            !onboardingCompleted -> {
                                OnboardingScreen(onFinished = { authViewModel.completeOnboarding() })
                            }
                            !isAuthenticated -> {
                                WelcomeScreen(
                                    authViewModel = authViewModel,
                                    onGuestClick = { authViewModel.continueAsGuest() }
                                )
                            }
                            else -> {
                                MainApp(authViewModel, settingsViewModel, academicViewModel, teacherViewModel)
                            }
                        }
                    }
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
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    academicViewModel: AcademicViewModel,
    teacherViewModel: TeacherViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    
    // Global academic state observation
    val academicYears by academicViewModel.allAcademicYears.observeAsState()
    val selectedYearId by academicViewModel.selectedYearId.observeAsState()
    val semesters by academicViewModel.semesters.observeAsState(emptyList())
    val selectedSemesterId by academicViewModel.selectedSemesterId.observeAsState()

    val isGuest by authViewModel.isGuest.observeAsState(initial = false)
    val isAuthenticated by authViewModel.isAuthenticated.observeAsState(initial = false)
    val justLoggedIn by authViewModel.justLoggedIn.collectAsState()
    val isSyncing by settingsViewModel.isSyncing.collectAsState()
    val showSyncChoiceDialog = rememberSaveable { mutableStateOf(false) }

    // Logic to detect first login with existing data or empty state
    LaunchedEffect(isAuthenticated, isGuest, academicYears, justLoggedIn) {
        academicYears ?: return@LaunchedEffect
        if (isAuthenticated && !isGuest && justLoggedIn) {
            // If user has local data, show conflict dialog. 
            // If local data is empty, still show the choice to restore or start fresh.
            showSyncChoiceDialog.value = true
        }
    }

    // Dialog and deletion visibility states
    val showAddYearDialog = remember { mutableStateOf(false) }
    val isYearMandatory = remember { mutableStateOf(false) }

    // Enforce initial setup only if not just logged in and not syncing
    LaunchedEffect(academicYears, justLoggedIn, showSyncChoiceDialog.value, isSyncing) {
        academicYears?.let { years ->
            if (years.isEmpty() && !justLoggedIn && !showSyncChoiceDialog.value && !isSyncing) {
                isYearMandatory.value = true
                showAddYearDialog.value = true
            } else if (years.isNotEmpty() || isSyncing) {
                isYearMandatory.value = false
                showAddYearDialog.value = false
            }
        }
    }

    val showAddPeriodDialog = remember { mutableStateOf(false) }
    val yearToDelete = remember { mutableStateOf<AcademicYear?>(null) }
    val periodToDelete = remember { mutableStateOf<Semester?>(null) }
    val yearToClone = remember { mutableStateOf<AcademicYear?>(null) }
    val periodToClone = remember { mutableStateOf<Semester?>(null) }

    // --- Global Dialog Logic ---

    // Dialog for choosing between local data or cloud restore after login
    if (showSyncChoiceDialog.value) {
        val hasLocalData = (academicYears?.size ?: 0) > 0
        AlertDialog(
            onDismissRequest = { 
                showSyncChoiceDialog.value = false 
                authViewModel.clearJustLoggedIn()
            },
            title = { Text(if (hasLocalData) stringResource(R.string.sync_choice_title) else stringResource(R.string.welcome_title)) },
            text = { Text(if (hasLocalData) stringResource(R.string.sync_choice_message) else stringResource(R.string.restore_desc)) },
            confirmButton = {
                Button(
                    onClick = { 
                        settingsViewModel.restoreFromCloud()
                        showSyncChoiceDialog.value = false 
                        authViewModel.clearJustLoggedIn()
                    }
                ) { Text(stringResource(R.string.restore_cloud_data)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        settingsViewModel.clearCloudBackup()
                        showSyncChoiceDialog.value = false 
                        authViewModel.clearJustLoggedIn()
                    }
                ) { Text(if (hasLocalData) stringResource(R.string.keep_local_data) else stringResource(R.string.get_started)) }
            }
        )
    }

    // Atomic dialog for adding a new Year + its initial Period
    if (showAddYearDialog.value) {
        val snackbarHostState = LocalSnackbarHostState.current
        val successMsg = stringResource(R.string.save_success)
        AddYearAndPeriodDialog(
            onDismiss = { showAddYearDialog.value = false },
            onAdd = { yearName, periodName ->
                academicViewModel.addYearWithPeriod(yearName, periodName)
                showAddYearDialog.value = false
                scope.launch { snackbarHostState.showAppSnackbar(successMsg, type = SnackbarType.SUCCESS) }
            },
            existingYears = academicYears ?: emptyList(),
            isMandatory = isYearMandatory.value
        )
    }

    // Dialog for adding additional periods to an existing year
    if (showAddPeriodDialog.value) {
        val snackbarHostState = LocalSnackbarHostState.current
        val successMsg = stringResource(R.string.save_success)
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
                confirmButton = { 
                    val periodName = stringResource(selectedOption)
                    Button(onClick = { 
                        academicViewModel.addSemester(periodName)
                        showAddPeriodDialog.value = false 
                        scope.launch { snackbarHostState.showAppSnackbar(successMsg, type = SnackbarType.SUCCESS) }
                    }) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = { TextButton(onClick = { showAddPeriodDialog.value = false }) { Text(stringResource(R.string.cancel)) } }
            )
        }
    }

    // Confirmation dialog for deleting an academic year
    yearToDelete.value?.let { year ->
        val snackbarHostState = LocalSnackbarHostState.current
        val deleteSuccessMsg = stringResource(R.string.delete_success)
        AlertDialog(
            onDismissRequest = { yearToDelete.value = null },
            title = { Text(stringResource(R.string.delete_year_title)) },
            text = { Text(stringResource(R.string.delete_year_message, year.year)) },
            confirmButton = {
                Button(
                    onClick = { 
                        academicViewModel.deleteAcademicYear(year)
                        yearToDelete.value = null 
                        scope.launch { snackbarHostState.showAppSnackbar(deleteSuccessMsg, type = SnackbarType.SUCCESS) }
                    }, 
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
        val snackbarHostState = LocalSnackbarHostState.current
        val deleteSuccessMsg = stringResource(R.string.delete_success)
        val localizedName = LocalizedNameMapper.getLocalizedSemesterName(semester.name)
        AlertDialog(
            onDismissRequest = { periodToDelete.value = null },
            title = { Text(stringResource(R.string.delete_period_title)) },
            text = { Text(stringResource(R.string.delete_period_message, localizedName)) },
            confirmButton = {
                Button(
                    onClick = { 
                        academicViewModel.deleteSemester(semester)
                        periodToDelete.value = null 
                        scope.launch { snackbarHostState.showAppSnackbar(deleteSuccessMsg, type = SnackbarType.SUCCESS) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.error)
                ) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = { TextButton(onClick = { periodToDelete.value = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // Dialog for cloning an academic year
    yearToClone.value?.let { year ->
        val snackbarHostState = LocalSnackbarHostState.current
        val cloneSuccessMsg = stringResource(R.string.clone_success)
        CloneYearDialog(
            sourceYear = year,
            existingYears = academicYears ?: emptyList(),
            onConfirm = { newName ->
                academicViewModel.cloneAcademicYear(year.id, newName)
                yearToClone.value = null
                scope.launch { snackbarHostState.showAppSnackbar(cloneSuccessMsg, type = SnackbarType.SUCCESS) }
            },
            onDismiss = { yearToClone.value = null }
        )
    }

    // Dialog for cloning an academic period (semester)
    periodToClone.value?.let { semester ->
        val snackbarHostState = LocalSnackbarHostState.current
        val cloneSuccessMsg = stringResource(R.string.clone_success)
        val localizedName = LocalizedNameMapper.getLocalizedSemesterName(semester.name)
        CloneSemesterDestinationDialog(
            title = stringResource(R.string.clone_item_title, localizedName),
            academicYears = academicYears ?: emptyList(),
            semestersProvider = { yearId -> academicViewModel.getSemestersForYear(yearId) },
            onConfirm = { targetYearId, newName ->
                academicViewModel.cloneSemester(semester.id, targetYearId, newName)
                periodToClone.value = null
                scope.launch { snackbarHostState.showAppSnackbar(cloneSuccessMsg, type = SnackbarType.SUCCESS) }
            },
            onDismiss = { periodToClone.value = null }
        )
    }

    // --- Main Layout Structure ---

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            TeacherDrawerContent(
                academicYears = academicYears ?: emptyList(),
                selectedYearId = selectedYearId,
                semesters = semesters,
                selectedSemesterId = selectedSemesterId,
                onYearSelect = { academicViewModel.selectYear(it); scope.launch { drawerState.close() } },
                onYearDelete = { yearToDelete.value = it },
                onYearClone = { yearToClone.value = it },
                onAddYear = { showAddYearDialog.value = true },
                onSemesterSelect = { academicViewModel.selectSemester(it); scope.launch { drawerState.close() } },
                onSemesterDelete = { periodToDelete.value = it },
                onSemesterClone = { periodToClone.value = it },
                onAddPeriod = { showAddPeriodDialog.value = true }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) { AppSnackbar(it) } },
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
                        authViewModel = authViewModel,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onClassClick = { id, name -> navController.navigate("students_class/$id/${URLEncoder.encode(name, "UTF-8")}") }
                    )
                }
                composable("subjects") {
                    SubjectsListScreen(
                        academicViewModel = academicViewModel,
                        authViewModel = authViewModel,
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
                        viewModel = settingsViewModel,
                        onNavigateToAccount = { navController.navigate("account") }, 
                        onNavigateToPreferences = { navController.navigate("preferences") },
                        onNavigateToAbout = { navController.navigate("about") },
                        onLogout = { authViewModel.logout() }
                    ) 
                }
                composable("preferences") { AppPreferencesScreen(settingsViewModel) { navController.popBackStack() } }
                composable("account") { AccountDetailsScreen(settingsViewModel) { navController.popBackStack() } }
                composable("about") { AboutScreen { navController.popBackStack() } }
                
                composable("subject/{id}/{name}", arguments = listOf(navArgument("id") { type = NavType.IntType }, navArgument("name") { type = NavType.StringType })) { 
                    SubjectScreen(
                        academicViewModel = academicViewModel,
                        teacherViewModel = teacherViewModel,
                        authViewModel = authViewModel,
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
                                academicViewModel = academicViewModel,
                                viewModel = teacherViewModel,
                                authViewModel = authViewModel,
                                studentsClassId = it.arguments?.getInt("id") ?: 0,
                                className = it.arguments?.getString("name") ?: "",
                                onStudentClick = { id: Int, name: String -> navController.navigate("grading/$id/${URLEncoder.encode(name, "UTF-8")}/${URLEncoder.encode(it.arguments?.getString("name") ?: "", "UTF-8")}") },
                                onAddStudentClick = { cid: Int -> navController.navigate("add_student/$cid") },
                                onEditStudentClick = { cid: Int, sid: Int -> navController.navigate("edit_student/$cid/$sid") },
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
                    StudentGradingScreen(academicViewModel, teacherViewModel, it.arguments?.getInt("id") ?: 0, it.arguments?.getString("sub") ?: "") { navController.popBackStack() }
                }
            }
        }
    }
}
