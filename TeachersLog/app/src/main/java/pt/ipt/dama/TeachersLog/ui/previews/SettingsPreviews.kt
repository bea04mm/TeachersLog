package pt.ipt.dama.TeachersLog.ui.previews

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import pt.ipt.dama.TeachersLog.ui.components.LocalSnackbarHostState
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
            userPhoto = "",
            isGuest = false,
            onNavigateToAccount = {}, 
            onNavigateToPreferences = {},
            onNavigateToAbout = {},
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
        val snackbarHostState = remember { SnackbarHostState() }
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
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
}

/**
 * Preview for the [AccountDetailsContent] in light mode.
 * Uses the stateless Content component to avoid ViewModel cast issues in previews.
 */
@Preview(showBackground = true, name = "Account Details Light")
@Composable
fun AccountDetailsPreview() {
    TeachersLogTheme(darkTheme = false) {
        val snackbarHostState = remember { SnackbarHostState() }
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            AccountDetailsContent(
                userName = "John Doe",
                userEmail = "john.doe@ipt.pt",
                userPhoto = "",
                isDeleting = false,
                onSave = { _, _, _ -> },
                onDeleteAccount = {},
                onBackClick = {}
            )
        }
    }
}
