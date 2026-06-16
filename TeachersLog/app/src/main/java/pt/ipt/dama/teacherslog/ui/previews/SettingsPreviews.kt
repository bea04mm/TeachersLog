package pt.ipt.dama.teacherslog.ui.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import pt.ipt.dama.teacherslog.ui.screens.AccountDetailsScreen
import pt.ipt.dama.teacherslog.ui.screens.AppPreferencesContent
import pt.ipt.dama.teacherslog.ui.screens.SettingsScreen
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme

/**
 * Preview for the [SettingsScreen] hub in light mode.
 */
@Preview(showBackground = true, name = "Settings Hub Light")
@Composable
fun SettingsPreview() {
    TeachersLogTheme(darkTheme = false) {
        SettingsScreen(onNavigateToAccount = {}, onNavigateToPreferences = {})
    }
}

/**
 * Preview for the [AppPreferencesContent] in light mode.
 */
@Preview(showBackground = true, name = "App Preferences Light")
@Composable
fun AppPreferencesPreview() {
    TeachersLogTheme(darkTheme = false) {
        AppPreferencesContent(
            darkMode = false,
            currentLang = "en",
            onToggleDarkMode = {},
            onLanguageChange = {},
            onBackClick = {}
        )
    }
}

/**
 * Preview for the [AccountDetailsScreen] in light mode.
 */
@Preview(showBackground = true, name = "Account Details Light")
@Composable
fun AccountDetailsPreview() {
    TeachersLogTheme(darkTheme = false) {
        AccountDetailsScreen(onBackClick = {})
    }
}
