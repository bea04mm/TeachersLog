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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.theme.TeachersLogTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.teacherslog.utils.LocaleManager

/**
 * AppPreferencesContent is the stateless UI for managing application settings.
 *
 * @param darkMode Boolean indicating if dark mode is enabled.
 * @param currentLang The current language code (e.g., "en", "pt").
 * @param onToggleDarkMode Callback to switch between light and dark themes.
 * @param onLanguageChange Callback to update the application language.
 * @param onBackClick Callback for top bar back button.
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
                                Text(text = stringResource(R.string.app_language), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = if (currentLang == "pt") stringResource(R.string.portuguese) else stringResource(R.string.english), 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    color = MaterialTheme.colorScheme.primary
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
 */
@Composable
fun AppPreferencesScreen(
    viewModel: TeacherViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val darkMode by viewModel.isDarkMode.observeAsState(initial = true)
    val currentLang by viewModel.currentLanguage.observeAsState(initial = LocaleManager.getLanguage(context))

    AppPreferencesContent(
        darkMode = darkMode,
        currentLang = currentLang,
        onToggleDarkMode = { viewModel.toggleDarkMode(it) },
        onLanguageChange = { lang ->
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit { putString("language", lang) }
            viewModel.setLanguage(lang)
        },
        onBackClick = onBackClick
    )
}

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
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        RadioButton(selected = (code == current), onClick = onClick)
    }
}

@Composable
fun PreferenceSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

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
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// --- Previews ---

@Preview(showBackground = true, name = "Preferences Light Mode")
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

@Preview(showBackground = true, name = "Preferences Dark Mode")
@Composable
fun AppPreferencesDarkPreview() {
    TeachersLogTheme(darkTheme = true) {
        AppPreferencesContent(
            darkMode = true,
            currentLang = "pt",
            onToggleDarkMode = {},
            onLanguageChange = {},
            onBackClick = {}
        )
    }
}
