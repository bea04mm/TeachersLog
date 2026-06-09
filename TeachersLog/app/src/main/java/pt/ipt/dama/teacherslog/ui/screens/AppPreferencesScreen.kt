package pt.ipt.dama.teacherslog.ui.screens

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
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.viewmodel.TeacherViewModel
import pt.ipt.dama.teacherslog.utils.LocaleManager
import android.content.Context
import androidx.core.content.edit

@Composable
fun AppPreferencesScreen(
    viewModel: TeacherViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val darkMode by viewModel.isDarkMode.observeAsState(initial = true)
    val currentLang by viewModel.currentLanguage.observeAsState(initial = LocaleManager.getLanguage(context))

    val showLanguageDialog = remember { mutableStateOf(false) }

    if (showLanguageDialog.value) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog.value = false },
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                Column {
                    LanguageOption(stringResource(R.string.english), "en", currentLang) { 
                        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit { putString("language", "en") }
                        viewModel.setLanguage("en")
                        showLanguageDialog.value = false 
                    }
                    LanguageOption(stringResource(R.string.portuguese), "pt", currentLang) { 
                        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit { putString("language", "pt") }
                        viewModel.setLanguage("pt")
                        showLanguageDialog.value = false 
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog.value = false }) { Text(stringResource(R.string.close)) } }
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
            // Language
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

            // Appearance
            item {
                PreferenceSection(title = stringResource(R.string.appearance), icon = Icons.Default.Palette) {
                    TonalCard {
                        PreferenceToggle(
                            title = stringResource(R.string.dark_mode),
                            subtitle = stringResource(R.string.dark_mode_desc),
                            checked = darkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) }
                        )
                    }
                }
            }

            // Grading
            item {
                PreferenceSection(title = stringResource(R.string.grading), icon = Icons.Default.Analytics) {
                    TonalCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = stringResource(R.string.default_grading_scale), style = MaterialTheme.typography.bodyLarge)
                            Text(text = stringResource(R.string.grading_scale_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.scale_range_label))
                                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
        RadioButton(selected = code == current, onClick = onClick)
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
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
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
