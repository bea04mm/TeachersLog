package pt.ipt.dama.teacherslog

import android.content.Context
import android.os.Bundle
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Calendar
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import pt.ipt.dama.teacherslog.data.model.AcademicYear
import pt.ipt.dama.teacherslog.ui.components.TeacherBottomBar
import pt.ipt.dama.teacherslog.ui.screens.*
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModelFactory
import pt.ipt.dama.teacherslog.utils.LocaleManager

/**
 * MainActivity is the primary entry point for the Teacher's Log application.
 *
 * It is responsible for:
 * 1. Initializing the shared [TeacherViewModel].
 * 2. Managing the application's locale and theme based on user preferences.
 * 3. Hosting the [MainApp] Composable which contains the navigation and global UI components.
 */
class MainActivity : ComponentActivity() {
    
    // Shared ViewModel across all screens to maintain global state (e.g., selected academic year)
    private val viewModel: TeacherViewModel by viewModels {
        TeacherViewModelFactory(application)
    }

    /**
     * Updates the base context to apply the selected language before the activity is created.
     * 
     * @param newBase The base context to be wrapped with the new locale configuration.
     */
    override fun attachBaseContext(newBase: Context) {
        // Retrieve the saved language preference from SharedPreferences
        val lang = newBase.getSharedPreferences("prefs", MODE_PRIVATE)
            .getString("language", "en") ?: "en"
        // Apply the locale using the LocaleManager utility
        super.attachBaseContext(LocaleManager.updateResources(newBase, lang))
    }

    /**
     * Called when the activity is first created.
     * Sets up the edge-to-edge display and the Compose content.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display for a modern immersive UI
        enableEdgeToEdge()
        
        setContent {
            // Observe the current language from the ViewModel to trigger UI updates on change
            val currentLang by viewModel.currentLanguage.observeAsState(initial = LocaleManager.getLanguage(this))
            
            // Observe the dark mode preference from the ViewModel
            val isDarkMode by viewModel.isDarkMode.observeAsState(initial = true)
            
            // Provide the updated LocalContext with the correct locale to the Composable tree
            CompositionLocalProvider(
                LocalContext provides LocaleManager.updateResources(this, currentLang),
                LocalActivityResultRegistryOwner provides this
            ) {
                // Apply the application theme
                TeachersLogTheme(darkTheme = isDarkMode) {
                    MainApp(viewModel)
                }
            }
        }
    }
}

/**
 * MainApp is the root Composable of the application's UI.
 * 
 * It manages:
 * 1. Navigation using [NavHost] and [rememberNavController].
 * 2. The global [ModalNavigationDrawer] for academic year management.
 * 3. The [Scaffold] containing the [TeacherBottomBar] and [SnackbarHost].
 *
 * @param viewModel The shared [TeacherViewModel] providing data and state.
 */
@Composable
fun MainApp(viewModel: TeacherViewModel) {
    // Controller for managing application navigation
    val navController = rememberNavController()
    // Current backstack entry used to determine the active route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    
    // State and scope for the Navigation Drawer (Sidebar)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Host state for displaying Snackbars
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Observe academic years and the currently selected year
    val academicYears by viewModel.allAcademicYears.observeAsState(emptyList())
    val selectedYearId by viewModel.selectedYearId.observeAsState()

    // UI state for management dialogs
    val showAddYearDialog = remember { mutableStateOf(false) }
    val yearToDelete = remember { mutableStateOf<AcademicYear?>(null) }

    // --- Global Dialogs ---

    // Dialog for adding a new academic year cycle
    if (showAddYearDialog.value) {
        AddYearDialog(
            onDismiss = { showAddYearDialog.value = false },
            onAdd = { yearName ->
                viewModel.addAcademicYear(yearName)
                showAddYearDialog.value = false
            },
            existingYears = academicYears
        )
    }

    // Confirmation dialog for deleting an academic year
    yearToDelete.value?.let { year ->
        AlertDialog(
            onDismissRequest = { yearToDelete.value = null },
            title = { Text(stringResource(R.string.delete_year_title)) },
            text = { Text(stringResource(R.string.delete_year_message, year.year)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAcademicYear(year)
                        yearToDelete.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { yearToDelete.value = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // --- Main Layout Structure ---

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen, // Disable swipe-to-open, but allow swipe-to-close
        drawerContent = {
            // Sidebar content for switching and managing academic years
            Box(modifier = Modifier.fillMaxHeight()) {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.academic_years_header),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // List of available academic years
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(academicYears) { year ->
                                val isSelected = year.id == selectedYearId
                                NavigationDrawerItem(
                                    label = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(year.year, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            // Allow deletion of non-selected years
                                            if (!isSelected) {
                                                IconButton(
                                                    onClick = { yearToDelete.value = year },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = stringResource(R.string.delete_label),
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.selectYear(year.id)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        
                        // Button to trigger the 'Add New Year' dialog
                        OutlinedButton(
                            onClick = { showAddYearDialog.value = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.add_new_year))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // Snackbar Host inside the drawer to ensure visibility on top of the menu
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                )
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Display the BottomBar only on top-level main screens
                if (currentRoute in listOf("dashboard", "subjects", "settings", "all_subjects")) {
                    TeacherBottomBar(
                        currentRoute = if (currentRoute == "all_subjects") "subjects" else currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            // --- Navigation Host (Main Router) ---
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                // Dashboard Destination
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSubjectClick = { subjectId, subjectName ->
                            val encodedName = URLEncoder.encode(subjectName, StandardCharsets.UTF_8.toString())
                            navController.navigate("subject/$subjectId/$encodedName")
                        },
                        onViewAllSubjectsClick = { navController.navigate("subjects") },
                        onRandomStudentClick = {
                            scope.launch {
                                val allStudents = viewModel.getAllStudentsForCurrentYear()
                                if (allStudents.isNotEmpty()) {
                                    val student = allStudents.random()
                                    val encodedName = URLEncoder.encode(student.name, StandardCharsets.UTF_8.toString())
                                    // Navigate directly to grading with student name and a default subject label
                                    navController.navigate("grading/${student.studentId}/$encodedName/Random%20Selection")
                                } else {
                                    snackbarHostState.showSnackbar(
                                        viewModel.getApplication<android.app.Application>()
                                            .getString(R.string.no_students_found, "any class")
                                    )
                                }
                            }
                        }
                    )
                }

                // Subjects List Destination
                composable("subjects") {
                    SubjectsListScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSubjectClick = { subjectId, subjectName ->
                            val encodedName = URLEncoder.encode(subjectName, StandardCharsets.UTF_8.toString())
                            navController.navigate("subject/$subjectId/$encodedName")
                        },
                        onViewAllClassesClick = { navController.navigate("all_subjects") },
                        onAddSubjectClick = { navController.navigate("add_subject") },
                        onEditSubjectClick = { subjectId -> navController.navigate("edit_subject/$subjectId") }
                    )
                }

                // Add Subject Destination
                composable("add_subject") {
                    SubjectEditScreen(
                        viewModel = viewModel,
                        subjectId = null,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Edit Subject Destination (with dynamic ID)
                composable(
                    route = "edit_subject/{subjectId}",
                    arguments = listOf(navArgument("subjectId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val subjectId = backStackEntry.arguments?.getInt("subjectId")
                    SubjectEditScreen(
                        viewModel = viewModel,
                        subjectId = subjectId,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // All Classes Overview Destination
                composable("all_subjects") {
                    AllClassesScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onClassClick = { className, classId ->
                            val encodedName = URLEncoder.encode(className, StandardCharsets.UTF_8.toString())
                            navController.navigate("students_class/$classId/$encodedName")
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Settings Main Destination
                composable("settings") {
                    SettingsScreen(
                        onNavigateToAccount = { navController.navigate("account") },
                        onNavigateToPreferences = { navController.navigate("preferences") }
                    )
                }

                // Subject Details / Classes within a Subject Destination
                composable(
                    route = "subject/{subjectId}/{subjectName}",
                    arguments = listOf(
                        navArgument("subjectId") { type = NavType.IntType },
                        navArgument("subjectName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 1
                    val subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
                    SubjectScreen(
                        viewModel = viewModel,
                        subjectId = subjectId,
                        subjectName = subjectName,
                        onClassClick = { id, name ->
                            val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                            navController.navigate("students_class/$id/$encodedName")
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Class Roster / Students within a Class Destination
                composable(
                    route = "students_class/{studentsClassId}/{className}",
                    arguments = listOf(
                        navArgument("studentsClassId") { type = NavType.IntType },
                        navArgument("className") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val studentsClassId = backStackEntry.arguments?.getInt("studentsClassId") ?: 1
                    val className = backStackEntry.arguments?.getString("className") ?: ""
                    ClassRosterScreen(
                        viewModel = viewModel,
                        studentsClassId = studentsClassId,
                        className = className,
                        onStudentClick = { id, name ->
                            val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                            val encodedSubj = URLEncoder.encode(className, StandardCharsets.UTF_8.toString())
                            navController.navigate("grading/$id/$encodedName/$encodedSubj")
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Student Grading Form Destination
                composable(
                    route = "grading/{studentId}/{studentName}/{subjectName}",
                    arguments = listOf(
                        navArgument("studentId") { type = NavType.IntType },
                        navArgument("studentName") { type = NavType.StringType },
                        navArgument("subjectName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val studentId = backStackEntry.arguments?.getInt("studentId") ?: 1
                    val subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
                    StudentGradingScreen(
                        viewModel = viewModel,
                        studentId = studentId,
                        className = subjectName,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // User Account Management Destination
                composable("account") {
                    AccountDetailsScreen(onBackClick = { navController.popBackStack() })
                }

                // App Preferences (Dark Mode, Language) Destination
                composable("preferences") {
                    AppPreferencesScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/**
 * AddYearDialog is a modal component that allows the user to select a starting year
 * for a new academic cycle (e.g., "2024/2025").
 *
 * @param onDismiss Callback executed when the dialog is dismissed without action.
 * @param onAdd Callback executed when the user confirms the addition of a new year.
 * @param existingYears List of already registered [AcademicYear] objects to avoid duplicates.
 */
@Composable
fun AddYearDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    existingYears: List<AcademicYear>
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    // Convert existing years to a set of starting integers (e.g., "2024/2025" -> 2024)
    val takenStartYears = existingYears.mapNotNull { year ->
        year.year.split("/").firstOrNull()?.toIntOrNull()
    }.toSet()

    // Find the first available start year starting from the current year
    var initialYear = currentYear
    while (initialYear in takenStartYears) {
        initialYear++
    }

    var selectedStartYear by remember { mutableIntStateOf(initialYear) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_new_year)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.select_starting_year),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decrease start year button (skips taken years)
                    IconButton(onClick = { 
                        var year = selectedStartYear - 1
                        // Limit to a reasonable past year, e.g., 2000
                        while (year in takenStartYears && year > 2000) {
                            year--
                        }
                        if (year !in takenStartYears) {
                            selectedStartYear = year
                        }
                    }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "$selectedStartYear / ${selectedStartYear + 1}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Increase start year button (skips taken years)
                    IconButton(onClick = { 
                        var year = selectedStartYear + 1
                        while (year in takenStartYears) {
                            year++
                        }
                        selectedStartYear = year
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd("$selectedStartYear/${selectedStartYear + 1}") }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
