package pt.ipt.dama.teacherslog.ui.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import pt.ipt.dama.teacherslog.data.model.GlobalStats
import pt.ipt.dama.teacherslog.data.model.StudentsClassWithStats
import pt.ipt.dama.teacherslog.ui.screens.DashboardContent
import pt.ipt.dama.teacherslog.ui.theme.*

/**
 * Preview for the [DashboardContent] UI in light mode.
 */
@Preview(showBackground = true, name = "Dashboard Light Mode")
@Composable
fun DashboardPreview() {
    TeachersLogTheme(darkTheme = false) {
        DashboardContent(
            recentClasses = listOf(
                StudentsClassWithStats(1, 1, "10.ºA", 4.5, 25, "5", "#004AC6", "functions"),
                StudentsClassWithStats(2, 1, "11.ºB", 3.8, 22, "20", "#006686", "biotech")
            ),
            stats = GlobalStats(20, 2, 85.0),
            isRefreshing = false,
            isGuest = false,
            syncEnabled = true,
            onRefresh = {},
            onMenuClick = {},
            onClassClick = { _, _ -> }
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
            recentClasses = listOf(
                StudentsClassWithStats(1, 1, "10.ºA", 4.5, 25, "5", "#004AC6", "functions"),
                StudentsClassWithStats(2, 1, "11.ºB", 3.8, 22, "20", "#006686", "biotech")
            ),
            stats = GlobalStats(20, 2, 85.0),
            isRefreshing = false,
            isGuest = false,
            syncEnabled = true,
            onRefresh = {},
            onMenuClick = {},
            onClassClick = { _, _ -> }
        )
    }
}
