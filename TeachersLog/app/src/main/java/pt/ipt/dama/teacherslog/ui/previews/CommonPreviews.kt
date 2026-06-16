package pt.ipt.dama.teacherslog.ui.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.data.model.AcademicYear
import pt.ipt.dama.teacherslog.data.model.Semester
import pt.ipt.dama.teacherslog.ui.components.*
import pt.ipt.dama.teacherslog.ui.theme.*

/**
 * Visualizes the [TeacherTopBar] in a standard dashboard context.
 */
@Preview(showBackground = true, name = "Top Bar Preview")
@Composable
fun TeacherTopBarPreview() {
    TeachersLogTheme {
        TeacherTopBar(title = "Dashboard", onNavigationIconClick = {})
    }
}

/**
 * Visualizes the [TeacherBottomBar] with a selected route.
 */
@Preview(showBackground = true, name = "Bottom Bar Preview")
@Composable
fun TeacherBottomBarPreview() {
    TeachersLogTheme {
        TeacherBottomBar(currentRoute = "dashboard", onNavigate = {})
    }
}

/**
 * Visualizes the [StatCard] with sample numeric data.
 */
@Preview(showBackground = true, name = "Stat Card Preview")
@Composable
fun StatCardPreview() {
    TeachersLogTheme {
        StatCard(
            title = stringResource(R.string.total_students),
            value = "45",
            icon = Icons.Default.Group,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Visualizes the [SubjectCard] as it would appear in a list.
 */
@Preview(showBackground = true, name = "Subject Card Preview")
@Composable
fun SubjectCardPreview() {
    TeachersLogTheme {
        SubjectCard(
            name = "Mathematics",
            average = "4.2",
            students = 12,
            color = Primary,
            icon = Icons.Default.Functions,
            gradingScale = "5",
            modifier = Modifier.padding(16.dp),
            onClick = {}
        )
    }
}

/**
 * Visualizes the [ClassCard] with its performance metrics.
 */
@Preview(showBackground = true, name = "Class Card Preview")
@Composable
fun ClassCardPreview() {
    TeachersLogTheme {
        ClassCard(
            name = "4.ºA",
            studentCount = 25,
            averageGrade = 3.85,
            color = Secondary,
            gradingScale = "5",
            modifier = Modifier.padding(16.dp),
            onClick = {}
        )
    }
}

/**
 * Visualizes the [TeacherDrawerContent] side menu.
 */
@Preview(showBackground = true, name = "Navigation Drawer Preview")
@Composable
fun TeacherDrawerPreview() {
    TeachersLogTheme {
        TeacherDrawerContent(
            academicYears = listOf(
                AcademicYear(1, "2024/2025", true),
                AcademicYear(2, "2023/2024", false)
            ),
            selectedYearId = 1,
            semesters = listOf(
                Semester(1, 1, "1st Semester"),
                Semester(2, 1, "2nd Semester")
            ),
            selectedSemesterId = 1,
            onYearSelect = {},
            onYearDelete = {},
            onAddYear = {},
            onSemesterSelect = {},
            onSemesterDelete = {},
            onAddPeriod = {}
        )
    }
}

/**
 * Visualizes the content of the Add Year and Period dialog.
 * Previewing the content directly ensures it is rendered in the preview pane,
 * as AlertDialog is a separate window and may not show up in some preview modes.
 */
@Preview(showBackground = true, name = "Add Year Dialog Content Preview")
@Composable
fun AddYearContentPreview() {
    TeachersLogTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AddYearAndPeriodContent(
                selectedStartYear = 2024,
                takenStartYears = setOf(2023),
                periodOptions = listOf(R.string.period_1st_semester, R.string.period_2nd_semester),
                selectedPeriodOption = R.string.period_1st_semester,
                onYearChange = {},
                onPeriodChange = {}
            )
        }
    }
}

/**
 * Comprehensive preview showing a combination of common components in a simulated dashboard layout.
 */
@Preview(showBackground = true, name = "All Components Summary")
@Composable
fun CommonComponentsSummaryPreview() {
    TeachersLogTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TeacherTopBar(title = "Components Overview", onNavigationIconClick = {})
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    title = stringResource(R.string.total_students),
                    value = "120",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.global_average),
                    value = "4.1",
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f),
                    color = AppTheme.colors.secondary
                )
            }
            
            SubjectCard(
                name = "Science",
                average = "3.9",
                students = 45,
                color = AppTheme.parseColor("#006874"),
                icon = Icons.Default.Biotech,
                gradingScale = "100",
                onClick = {}
            )
            
            ClassCard(
                name = "10.ºB",
                studentCount = 30,
                averageGrade = 4.5,
                color = AppTheme.colors.tertiary,
                gradingScale = "20",
                onClick = {}
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            TeacherBottomBar(currentRoute = "subjects", onNavigate = {})
        }
    }
}
