package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.StudentsClassWithStats
import pt.ipt.dama.teacherslog.ui.components.ClassCard
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel
import androidx.compose.ui.tooling.preview.Preview

/**
 * AllClassesContent is the stateless UI component that displays a complete list 
 * of all registered classes for the currently active academic year.
 *
 * It manages:
 * 1. A header section with academic overview labels.
 * 2. A scrollable [LazyColumn] containing all classes.
 * 3. Individual [ClassCard] items for each class with their specific statistics and scales.
 *
 * @param classes The list of [StudentsClassWithStats] to be displayed.
 * @param onMenuClick Callback for when the navigation menu icon is clicked.
 * @param onClassClick Callback triggered when a class card is selected.
 * @param onBackClick Callback for the top bar back button.
 */
@Composable
fun AllClassesContent(
    classes: List<StudentsClassWithStats>,
    onMenuClick: () -> Unit,
    onClassClick: (String, Int) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            // Standard top bar for overview screens with drawer and back support
            TeacherTopBar(
                title = stringResource(R.string.all_registered_classes),
                showBackButton = true,
                onBackClick = onBackClick,
                onNavigationIconClick = onMenuClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Header Section ---
            item {
                Text(
                    text = stringResource(R.string.academic_overview),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.active_year_classes),
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- Classes List ---
            items(classes) { clazz ->
                ClassCard(
                    name = clazz.name,
                    studentCount = clazz.studentCount,
                    averageGrade = clazz.averageGrade,
                    color = MaterialTheme.colorScheme.primary,
                    gradingScale = clazz.gradingScale,
                    onClick = { onClassClick(clazz.name, clazz.id) }
                )
            }
        }
    }
}

/**
 * AllClassesScreen is the stateful Composable that connects the [TeacherViewModel]
 * to the [AllClassesContent] UI.
 *
 * It observes the [TeacherViewModel.studentsClassesForYear] state to keep the 
 * class list updated based on the selected academic year.
 *
 * @param viewModel The shared [TeacherViewModel] providing academic data.
 * @param onMenuClick Callback to open the side navigation drawer.
 * @param onClassClick Callback to navigate to a specific class roster.
 * @param onBackClick Callback to return to the previous screen.
 */
@Composable
fun AllClassesScreen(
    viewModel: TeacherViewModel,
    onMenuClick: () -> Unit,
    onClassClick: (String, Int) -> Unit,
    onBackClick: () -> Unit
) {
    // Observe the stream of all classes for the active year
    val classes by viewModel.studentsClassesForYear.observeAsState(emptyList())

    AllClassesContent(
        classes = classes,
        onMenuClick = onMenuClick,
        onClassClick = onClassClick,
        onBackClick = onBackClick
    )
}

/**
 * Preview for the [AllClassesContent] UI with sample data in light mode.
 */
@Preview(showBackground = true, name = "All Classes Light Mode")
@Composable
fun AllClassesPreview() {
    TeachersLogTheme(darkTheme = false) {
        AllClassesContent(
            classes = listOf(
                StudentsClassWithStats(1, 1, "10.ºA", 4.2, 25, "5"),
                StudentsClassWithStats(2, 1, "11.ºB", 3.8, 22, "20"),
                StudentsClassWithStats(3, 2, "12.ºC", 4.5, 20, "100")
            ),
            onMenuClick = {},
            onClassClick = { _, _ -> },
            onBackClick = {}
        )
    }
}

/**
 * Preview for the [AllClassesContent] UI in dark mode.
 */
@Preview(showBackground = true, name = "All Classes Dark Mode")
@Composable
fun AllClassesDarkPreview() {
    TeachersLogTheme(darkTheme = true) {
        AllClassesContent(
            classes = listOf(
                StudentsClassWithStats(1, 1, "10.ºA", 4.2, 25, "5"),
                StudentsClassWithStats(2, 1, "11.ºB", 3.8, 22, "20")
            ),
            onMenuClick = {},
            onClassClick = { _, _ -> },
            onBackClick = {}
        )
    }
}
