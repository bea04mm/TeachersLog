package pt.ipt.dama.teacherslog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.ui.components.TeacherTopBar
import pt.ipt.dama.teacherslog.ui.components.TonalCard
import pt.ipt.dama.teacherslog.ui.theme.*
import pt.ipt.dama.teacherslog.utils.IconMapper

/**
 * AccountDetailsScreen provides a form to update user identity details.
 *
 * @param onBackClick Callback for navigating back to the settings screen.
 */
@Composable
fun AccountDetailsScreen(
    onBackClick: () -> Unit
) {
    // Local UI state for form fields
    val defaultFirstName = stringResource(R.string.default_user_firstname)
    val defaultLastName = stringResource(R.string.default_user_lastname)
    val defaultEmail = stringResource(R.string.default_user_email)

    var firstName by remember { mutableStateOf(defaultFirstName) }
    var lastName by remember { mutableStateOf(defaultLastName) }
    var email by remember { mutableStateOf(defaultEmail) }

    Scaffold(
        topBar = {
            TeacherTopBar(
                title = stringResource(R.string.account),
                titleIcon = IconMapper.Account,
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
            // --- Profile Header Section ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(128.dp)) {
                        // Profile circular background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(AppTheme.circle)
                                .background(AppTheme.colors.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = IconMapper.Account, 
                                contentDescription = null, 
                                modifier = Modifier.size(80.dp), 
                                tint = AppTheme.colors.outline
                            )
                        }
                        // Circular edit FAB overlay
                        IconButton(
                            onClick = { /* TODO: Implement actual photo picker */ },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(40.dp)
                                .clip(AppTheme.circle)
                                .background(AppTheme.colors.primary),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = AppTheme.colors.onPrimary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$firstName $lastName", 
                        style = AppTheme.typography.headlineMedium
                    )
                    Text(
                        text = email,
                        style = AppTheme.typography.bodyMedium, 
                        color = AppTheme.colors.onSurfaceVariant
                    )
                }
            }

            // --- Personal Information Form ---
            item {
                Column {
                    TonalCard {
                        // Section Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppTheme.colors.surfaceContainerLow)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.personal_information), 
                                style = AppTheme.typography.bodyLarge, 
                                color = AppTheme.colors.primary
                            )
                        }
                        // Form Fields
                        Column(
                            modifier = Modifier.padding(16.dp), 
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(), 
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = firstName,
                                    onValueChange = { firstName = it },
                                    label = { Text(stringResource(R.string.first_name)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = lastName,
                                    onValueChange = { lastName = it },
                                    label = { Text(stringResource(R.string.last_name)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text(stringResource(R.string.email)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            // Save Button
                            Button(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = AppTheme.shapes.medium
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
        }
    }
}
