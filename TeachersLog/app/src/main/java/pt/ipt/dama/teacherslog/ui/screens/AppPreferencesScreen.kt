package pt.ipt.dama.teacherslog.ui.screens

import android.content.Context
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
import androidx.core.content.edit
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.theme.*
import pt.ipt.dama.teacherslog.ui.viewmodel.SettingsViewModel
import pt.ipt.dama.teacherslog.utils.LocaleManager

/**
 * AppPreferencesContent is the stateless UI for managing application settings.
 *
 * @param darkMode Boolean indicating if dark mode is enabled.
 * @param currentLang The current language code (e.g., "en", "pt").
 * @param onToggleDarkMode Callback to switch between light and dark themes.
 * @param onLanguageChange Callback to update the application language.
 * @param onBackClick Callback for the top bar back button.
 */
@Composable
fun AppPreferencesContent(
    darkMode: Boolean,
    currentLang: String,
    onToggleDarkMode: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val showLanguageDialog = remember { mutableStateOf(false) }

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
            // Language Selection Section
            item {
                PreferenceSection(title = stringResource(R.string.language_label), icon = Icons.Default.Language) {
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
                                Text(text = stringResource(R.string.app_language), style = AppTheme.typography.bodyLarge)
                                Text(
                                    text = if (currentLang == "pt") stringResource(R.string.portuguese) else stringResource(R.string.english), 
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

    AppPreferencesContent(
        darkMode = darkMode,
        currentLang = currentLang,
        onToggleDarkMode = { settingsViewModel.toggleDarkMode(it) },
        onLanguageChange = { lang ->
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit { putString("language", lang) }
            settingsViewModel.setLanguage(lang)
        },
        onBackClick = onBackClick
    )
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