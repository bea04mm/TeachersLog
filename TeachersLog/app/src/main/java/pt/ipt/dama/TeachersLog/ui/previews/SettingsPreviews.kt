package pt.ipt.dama.TeachersLog.ui.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import pt.ipt.dama.TeachersLog.ui.screens.AccountDetailsContent
import pt.ipt.dama.TeachersLog.ui.screens.AppPreferencesContent
import pt.ipt.dama.TeachersLog.ui.screens.SettingsContent
import pt.ipt.dama.TeachersLog.ui.theme.TeachersLogTheme

/**
 * Preview for the [SettingsContent] hub in light mode.
 * Uses the stateless Content component to avoid ViewModel cast issues in previews.
 */
@Preview(showBackground = true, name = "Settings Hub Light")
@Composable
fun SettingsPreview() {
    TeachersLogTheme(darkTheme = false) {
        SettingsContent(
            userName = "John Doe",
            userEmail = "john.doe@ipt.pt",
            isGuest = false,
            onNavigateToAccount = {}, 
            onNavigateToPreferences = {},
            onLogout = {}
        )
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
            syncEnabled = true,
            isGuest = false,
            isSyncing = false,
            syncMessage = null,
            onToggleDarkMode = {},
            onLanguageChange = {},
            onToggleSync = {},
            onRestoreClick = {},
            onClearSyncMessage = {},
            onBackClick = {}
        )
    }
}

/**
 * Preview for the [AccountDetailsContent] in light mode.
 * Uses the stateless Content component to avoid ViewModel cast issues in previews.
 */
@Preview(showBackground = true, name = "Account Details Light")
@Composable
fun AccountDetailsPreview() {
    TeachersLogTheme(darkTheme = false) {
        AccountDetailsContent(
            userName = "John Doe",
            userEmail = "john.doe@ipt.pt",
            isDeleting = false,
            onSave = { _, _ -> },
            onDeleteAccount = {},
            onBackClick = {}
        )
    }
}
