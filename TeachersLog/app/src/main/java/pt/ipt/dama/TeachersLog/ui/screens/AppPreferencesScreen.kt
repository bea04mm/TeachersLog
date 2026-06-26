package pt.ipt.dama.TeachersLog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.ui.components.TeacherTopBar
import pt.ipt.dama.TeachersLog.ui.components.TonalCard
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.viewmodel.SettingsViewModel
import pt.ipt.dama.TeachersLog.utils.LocaleManager

/**
 * AppPreferencesContent is the stateless UI for managing application settings.
 *
 * @param darkMode Boolean indicating if dark mode is enabled.
 * @param currentLang The current language code (e.g., "en", "pt").
 * @param syncEnabled Boolean indicating if cloud sync is active.
 * @param isGuest Boolean indicating if user is in guest mode (limits sync options).
 * @param isSyncing Boolean indicating if a cloud operation is in progress.
 * @param syncMessage Optional feedback message from the last sync operation.
 * @param onToggleDarkMode Callback to switch between light and dark themes.
 * @param onLanguageChange Callback to update the application language.
 * @param onToggleSync Callback to enable/disable background sync.
 * @param onRestoreClick Callback to trigger cloud backup restoration.
 * @param onClearSyncMessage Callback to dismiss the sync feedback notification.
 * @param onBackClick Callback for the top bar back button.
 */
@Composable
fun AppPreferencesContent(
    darkMode: Boolean,
    currentLang: String,
    syncEnabled: Boolean,
    isGuest: Boolean,
    isSyncing: Boolean,
    syncMessage: String?,
    onToggleDarkMode: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onToggleSync: (Boolean) -> Unit,
    onRestoreClick: () -> Unit,
    onClearSyncMessage: () -> Unit,
    onBackClick: () -> Unit
) {
    val showLanguageDialog = remember { mutableStateOf(false) }
    val showRestoreDialog = remember { mutableStateOf(false) }

    if (showRestoreDialog.value) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog.value = false },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = { onRestoreClick(); showRestoreDialog.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.error)
                ) { Text(stringResource(R.string.restore_from_cloud)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog.value = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (syncMessage != null) {
        val displayMessage = when {
            syncMessage == "RESTORE_SUCCESS" -> stringResource(R.string.restore_success_message)
            syncMessage.startsWith("RESTORE_FAILED:") -> {
                val error = syncMessage.substringAfter("RESTORE_FAILED:")
                when {
                    error == "User not authenticated" -> stringResource(R.string.error_user_not_authenticated)
                    error.startsWith("Download failed:") -> stringResource(R.string.error_download_failed, error.substringAfter("Download failed:"))
                    else -> stringResource(R.string.restore_failed_message, error)
                }
            }
            syncMessage == "User not authenticated" -> stringResource(R.string.error_user_not_authenticated)
            else -> syncMessage
        }

        AlertDialog(
            onDismissRequest = onClearSyncMessage,
            title = { Text(stringResource(R.string.sync_notification_title)) },
            text = { Text(displayMessage) },
            confirmButton = {
                Button(onClick = onClearSyncMessage) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showLanguageDialog.value) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog.value = false },
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                Column {
                    LanguageOption(stringResource(R.string.english), "en", currentLang) { 
                        onLanguageChange("en")
                        showLanguageDialog.value = false 
                    }
                    LanguageOption(stringResource(R.string.portuguese), "pt", currentLang) { 
                        onLanguageChange("pt")
                        showLanguageDialog.value = false 
                    }
                }
            },
            confirmButton = { 
                TextButton(onClick = { showLanguageDialog.value = false }) { 
                    Text(stringResource(R.string.close)) 
                } 
            }
        )
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TeacherTopBar(
                title = stringResource(R.string.app_preferences),
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                PreferenceSection(
                    title = stringResource(R.string.language_label),
                    icon = Icons.Default.Language
                ) {
                    TonalCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog.value = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.app_language),
                                    style = AppTheme.typography.bodyLarge
                                )
                                val languageName = when (currentLang) {
                                    "pt" -> stringResource(R.string.portuguese)
                                    "en" -> stringResource(R.string.english)
                                    else -> currentLang
                                }
                                Text(
                                    text = languageName,
                                    style = AppTheme.typography.bodyMedium,
                                    color = AppTheme.colors.primary
                                )
                            }
                            Icon(Icons.Default.Translate, contentDescription = null)
                        }
                    }
                }
            }

            // Appearance/Theme Section
            item {
                PreferenceSection(title = stringResource(R.string.appearance), icon = Icons.Default.Palette) {
                    TonalCard {
                        PreferenceToggle(
                            title = stringResource(R.string.dark_mode),
                            subtitle = stringResource(R.string.dark_mode_desc),
                            checked = darkMode,
                            onCheckedChange = onToggleDarkMode
                        )
                    }
                }
            }

            // Data Sync Section
            if (!isGuest) {
                item {
                    PreferenceSection(title = stringResource(R.string.data_management_label), icon = Icons.Default.CloudSync) {
                        TonalCard {
                            Column {
                                PreferenceToggle(
                                    title = stringResource(R.string.sync_label),
                                    subtitle = stringResource(R.string.sync_desc),
                                    checked = syncEnabled,
                                    onCheckedChange = onToggleSync
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppTheme.colors.outlineVariant)
                                PreferenceAction(
                                    title = stringResource(R.string.restore_from_cloud),
                                    subtitle = stringResource(R.string.restore_desc),
                                    icon = Icons.Default.CloudDownload,
                                    isLoading = isSyncing,
                                    onClick = { showRestoreDialog.value = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * AppPreferencesScreen is the stateful container for the preferences UI.
 *
 * @param settingsViewModel The ViewModel managing global UI settings.
 * @param onBackClick Callback to return to the previous screen.
 */
@Composable
fun AppPreferencesScreen(
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val darkMode by settingsViewModel.isDarkMode.observeAsState(initial = true)
    val currentLang by settingsViewModel.currentLanguage.observeAsState(initial = LocaleManager.getLanguage(context))
    val syncEnabled by settingsViewModel.isSyncEnabled.observeAsState(initial = false)
    val isGuest by settingsViewModel.isGuest.observeAsState(initial = false)
    val isSyncing by settingsViewModel.isSyncing.collectAsState()
    val syncMessage by settingsViewModel.syncMessage.collectAsState()

    AppPreferencesContent(
        darkMode = darkMode,
        currentLang = currentLang,
        syncEnabled = syncEnabled,
        isGuest = isGuest,
        isSyncing = isSyncing,
        syncMessage = syncMessage,
        onToggleDarkMode = { settingsViewModel.toggleDarkMode(it) },
        onLanguageChange = { lang ->
            settingsViewModel.setLanguage(lang)
            LocaleManager.applyLocaleToActivity(context, lang)
        },
        onToggleSync = { settingsViewModel.toggleSync(it) },
        onRestoreClick = { settingsViewModel.restoreFromCloud() },
        onClearSyncMessage = { settingsViewModel.clearSyncMessage() },
        onBackClick = onBackClick
    )
}

/**
 * A reusable row containing a title, subtitle, and an action icon (or loading spinner).
 */
@Composable
fun PreferenceAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AppTheme.typography.bodyLarge)
            Text(text = subtitle, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Icon(imageVector = icon, contentDescription = null, tint = AppTheme.colors.primary)
        }
    }
}

/**
 * A selectable row for picking a language.
 *
 * @param label Human-readable name of the language.
 * @param code ISO language code (e.g., "en").
 * @param current Currently selected language code.
 * @param onClick Callback when the option is selected.
 */
@Composable
fun LanguageOption(label: String, code: String, current: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = AppTheme.typography.bodyLarge)
        RadioButton(selected = (code == current), onClick = onClick)
    }
}

/**
 * Standard section for grouping related preferences with an icon.
 *
 * @param title Title of the section.
 * @param icon Icon representing the section's purpose.
 * @param content The composable content to display within the section.
 */
@Composable
fun PreferenceSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = AppTheme.colors.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = AppTheme.typography.titleMedium)
        }
        content()
    }
}

/**
 * A reusable row containing a title, subtitle, and a toggle switch.
 *
 * @param title Primary text for the setting.
 * @param subtitle Secondary descriptive text.
 * @param checked Current state of the switch.
 * @param onCheckedChange Callback when the toggle is flipped.
 */
@Composable
fun PreferenceToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AppTheme.typography.bodyLarge)
            Text(text = subtitle, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}