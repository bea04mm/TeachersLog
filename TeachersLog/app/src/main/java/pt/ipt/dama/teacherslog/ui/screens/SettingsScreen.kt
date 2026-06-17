package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.theme.*
import pt.ipt.dama.teacherslog.utils.IconMapper

/**
 * SettingsScreen displays the main configuration options for the user.
 *
 * @param onNavigateToAccount Navigation to profile details.
 * @param onNavigateToPreferences Navigation to theme and language settings.
 */
@Composable
fun SettingsScreen(
    onNavigateToAccount: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TeacherTopBar(
                title = stringResource(R.string.nav_settings),
                titleIcon = IconMapper.Settings
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
            // Profile Summary Section
            item {
                TonalCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = AppTheme.circle,
                            color = AppTheme.colors.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(IconMapper.Account, contentDescription = null, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.jane_harrison), style = AppTheme.typography.titleLarge)
                            Text(text = stringResource(R.string.jane_email), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            TextButton(
                                onClick = onNavigateToAccount,
                                modifier = Modifier.offset(x = (-8).dp) // Align text with names while keeping internal padding
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.edit_profile))
                            }
                        }
                    }
                }
            }

            // General Configuration Section
            item {
                Text(text = stringResource(R.string.configuration), style = AppTheme.typography.labelSmall, color = AppTheme.colors.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                TonalCard {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.app_preferences)) },
                        supportingContent = { Text(stringResource(R.string.app_preferences_desc)) },
                        leadingContent = { Icon(Icons.Default.Tune, contentDescription = null) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable(onClick = onNavigateToPreferences),
                        colors = ListItemDefaults.colors(containerColor = AppTheme.transparent)
                    )
                }
            }

            // Application Metadata Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.error)
                    ) {
                        Icon(IconMapper.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sign_out))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.version_info), style = AppTheme.typography.labelSmall, color = AppTheme.colors.outline)
                }
            }
        }
    }
}
