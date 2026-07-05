package pt.ipt.dama.TeachersLog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.ui.components.TeacherTopBar
import pt.ipt.dama.TeachersLog.ui.components.TonalCard
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.viewmodel.SettingsViewModel
import pt.ipt.dama.TeachersLog.utils.IconMapper

/**
 * SettingsScreen displays the main configuration options for the user.
 *
 * @param onNavigateToAccount Navigation to profile details.
 * @param onNavigateToPreferences Navigation to theme and language settings.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAccount: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogout: () -> Unit
) {
    val profile by viewModel.userProfile.observeAsState()
    val isGuest by viewModel.isGuest.observeAsState(false)

    SettingsContent(
        userName = profile?.name ?: "",
        userEmail = profile?.email ?: "",
        userPhoto = profile?.photoUrl ?: "",
        isGuest = isGuest,
        onNavigateToAccount = onNavigateToAccount,
        onNavigateToPreferences = onNavigateToPreferences,
        onNavigateToAbout = onNavigateToAbout,
        onLogout = onLogout
    )
}

/**
 * Stateless content for [SettingsScreen] to enable previews and easier testing.
 */
@Composable
fun SettingsContent(
    userName: String,
    userEmail: String,
    userPhoto: String,
    isGuest: Boolean,
    onNavigateToAccount: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                            color = AppTheme.colors.surfaceContainerHigh
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (userPhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(userPhoto)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = IconMapper.Account, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(32.dp),
                                        tint = AppTheme.colors.outline
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val displayName = if (isGuest) stringResource(R.string.continue_as_guest) else userName
                            val displayEmail = if (isGuest) stringResource(R.string.guest_disclaimer) else userEmail

                            Text(text = displayName, style = AppTheme.typography.titleLarge)
                            Text(text = displayEmail, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceVariant)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (!isGuest) {
                                TextButton(
                                    onClick = onNavigateToAccount,
                                    modifier = Modifier.offset(x = (-8).dp)
                                ) {
                                    Icon(IconMapper.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.edit_profile))
                                }
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
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.app_preferences)) },
                            supportingContent = { Text(stringResource(R.string.app_preferences_desc)) },
                            leadingContent = { Icon(Icons.Default.Tune, contentDescription = null, tint = AppTheme.colors.primary) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.clickable(onClick = onNavigateToPreferences),
                            colors = ListItemDefaults.colors(containerColor = AppTheme.transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppTheme.colors.outlineVariant)
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.nav_about)) },
                            leadingContent = { Icon(Icons.Default.Info, contentDescription = null, tint = AppTheme.colors.primary) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.clickable(onClick = onNavigateToAbout),
                            colors = ListItemDefaults.colors(containerColor = AppTheme.transparent)
                        )
                    }
                }
            }

            // Application Metadata Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isGuest) {
                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                            shape = AppTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.create_account_login))
                        }
                    } else {
                        TextButton(
                            onClick = onLogout,
                            colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.error)
                        ) {
                            Icon(IconMapper.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sign_out))
                        }
                    }
                }
            }
        }
    }
}
