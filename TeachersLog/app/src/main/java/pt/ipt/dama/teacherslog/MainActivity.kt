package pt.ipt.dama.teacherslog

import android.content.Context
import android.os.Bundle
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Calendar
import androidx.activity.ComponentActivity
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
 * Entry point of the application. 
 * Manages the top-level Navigation Graph and the Navigation Drawer (Sidebar).
 */
class MainActivity : ComponentActivity() {
    // Shared ViewModel across all screens to maintain state (like selected year)
    private val viewModel: TeacherViewModel by viewModels {
        TeacherViewModelFactory(application)
    }

    override fun attachBaseContext(newBase: Context) {
        // Apply the saved language on attach
        val lang = newBase.getSharedPreferences("prefs", MODE_PRIVATE)
            .getString("language", "en") ?: "en"
        super.attachBaseContext(LocaleManager.updateResources(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Observe language to trigger UI updates
            val currentLang by viewModel.currentLanguage.observeAsState(initial = LocaleManager.getLanguage(this))
            
            // Observe the global theme state from the ViewModel
            val isDarkMode by viewModel.isDarkMode.observeAsState(initial = true)
            
            CompositionLocalProvider(LocalContext provides LocaleManager.updateResources(this, currentLang)) {
                TeachersLogTheme(darkTheme = isDarkMode) {
                    MainApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainApp(viewModel: TeacherViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val academicYears by viewModel.allAcademicYears.observeAsState(emptyList())
    val selectedYearId by viewModel.selectedYearId.observeAsState()

    val showAddYearDialog = remember { mutableStateOf(false) }
    val yearToDelete = remember { mutableStateOf<AcademicYear?>(null) }

    // --- Auxiliary Dialogs for Academic Year Management ---

    if (showAddYearDialog.value) {
        AddYearDialog(
            onDismiss = { showAddYearDialog.value = false },
            onAdd = { yearName ->
                if (academicYears.any { it.year == yearName }) {
                    scope.launch {
                        snackbarHostState.showSnackbar(viewModel.getApplication<android.app.Application>().getString(R.string.year_exists_error))
                    }
                } else {
                    viewModel.addAcademicYear(yearName)
                }
                showAddYearDialog.value = false
            }
        )
    }

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

    // --- Navigation Drawer Implementation ---

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
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
                
                // Snackbar Host inside the drawer to ensure visibility on top of the side menu
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
                // BottomBar is only shown on main top-level destinations
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
            // --- Navigation Host (Main App Router) ---
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSubjectClick = { subjectId, subjectName ->
                            // URL encoding is mandatory for names with special chars like "4ºA"
                            val encodedName = URLEncoder.encode(subjectName, StandardCharsets.UTF_8.toString())
                            navController.navigate("subject/$subjectId/$encodedName")
                        },
                        onViewAllSubjectsClick = { navController.navigate("subjects") },
                        onRandomStudentClick = {
                            // Fetch a random student from the current classes
                            val classes = viewModel.studentsClassesForYear.value
                            if (!classes.isNullOrEmpty()) {
                                val randomClass = classes.random()
                                scope.launch {
                                    val students = viewModel.getStudentsWithGradesForStudentsClass(randomClass.id).value
                                    if (!students.isNullOrEmpty()) {
                                        val student = students.random()
                                        val encodedName = URLEncoder.encode(student.name, StandardCharsets.UTF_8.toString())
                                        val encodedSubj = URLEncoder.encode(randomClass.name, StandardCharsets.UTF_8.toString())
                                        navController.navigate("grading/${student.studentId}/$encodedName/$encodedSubj")
                                    } else {
                                        snackbarHostState.showSnackbar(viewModel.getApplication<android.app.Application>().getString(R.string.no_students_found, randomClass.name))
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(viewModel.getApplication<android.app.Application>().getString(R.string.no_classes_available))
                                }
                            }
                        }
                    )
                }
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
                composable("add_subject") {
                    SubjectEditScreen(
                        viewModel = viewModel,
                        subjectId = null,
                        onBackClick = { navController.popBackStack() }
                    )
                }
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
                composable("settings") {
                    SettingsScreen(
                        onNavigateToAccount = { navController.navigate("account") },
                        onNavigateToPreferences = { navController.navigate("preferences") }
                    )
                }
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
                composable("account") {
                    AccountDetailsScreen(onBackClick = { navController.popBackStack() })
                }
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
 * Modal to pick a start year for a new academic cycle.
 */
@Composable
fun AddYearDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var selectedStartYear by remember { mutableIntStateOf(currentYear) }
    
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
                    IconButton(onClick = { selectedStartYear-- }) {
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
                    
                    IconButton(onClick = { selectedStartYear++ }) {
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
