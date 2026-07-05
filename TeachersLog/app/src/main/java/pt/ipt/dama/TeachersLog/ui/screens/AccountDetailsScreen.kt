package pt.ipt.dama.TeachersLog.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.ui.components.TeacherTopBar
import pt.ipt.dama.TeachersLog.ui.components.TonalCard
import pt.ipt.dama.TeachersLog.ui.components.LocalSnackbarHostState
import pt.ipt.dama.TeachersLog.ui.components.SnackbarType
import pt.ipt.dama.TeachersLog.ui.components.showAppSnackbar
import pt.ipt.dama.TeachersLog.ui.theme.*
import pt.ipt.dama.TeachersLog.ui.viewmodel.SettingsViewModel
import pt.ipt.dama.TeachersLog.utils.IconMapper

/**
 * AccountDetailsScreen provides a form to update user identity details.
 */
@Composable
fun AccountDetailsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val profile by viewModel.userProfile.observeAsState()
    val isDeleting by viewModel.isSyncing.collectAsState()

    AccountDetailsContent(
        userName = profile?.name ?: "",
        userEmail = profile?.email ?: "",
        userPhoto = profile?.photoUrl ?: "",
        isDeleting = isDeleting,
        onSave = { name, email, photo -> viewModel.updateProfile(name, email, photo) },
        onDeleteAccount = { viewModel.deleteAccount() },
        onBackClick = onBackClick
    )
}

/**
 * Stateless content for [AccountDetailsScreen] to enable previews and easier testing.
 */
@Composable
fun AccountDetailsContent(
    userName: String,
    userEmail: String,
    userPhoto: String,
    isDeleting: Boolean,
    onSave: (String, String, String?) -> Unit,
    onDeleteAccount: () -> Unit,
    onBackClick: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }
    val showDeleteDialog = remember { mutableStateOf(false) }
    
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val successMsg = stringResource(R.string.save_success)
    val shortNameMsg = stringResource(R.string.error_name_too_short)
    val invalidEmailMsg = stringResource(R.string.error_invalid_email)
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$".toRegex()

    // Sync local state when inputs change
    LaunchedEffect(userName, userEmail, userPhoto) {
        val parts = userName.split(" ")
        firstName = parts.firstOrNull() ?: ""
        email = userEmail
        
        if (photoUri.isEmpty() || photoUri.startsWith("http")) {
            photoUri = userPhoto
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { photoUri = it.toString() } }
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = AppTheme.colors.background,
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
            if (showDeleteDialog.value) {
                item {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog.value = false },
                        title = { Text(stringResource(R.string.delete_account_confirm_title), style = AppTheme.typography.titleLarge) },
                        text = { Text(stringResource(R.string.delete_account_confirm_message), style = AppTheme.typography.bodyMedium) },
                        confirmButton = {
                            Button(
                                onClick = { onDeleteAccount(); showDeleteDialog.value = false },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.error),
                                shape = AppTheme.shapes.medium
                            ) { Text(stringResource(R.string.delete_label)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog.value = false }) { Text(stringResource(R.string.cancel)) }
                        },
                        shape = AppTheme.shapes.large,
                        containerColor = AppTheme.colors.surfaceContainerHigh
                    )
                }
            }

            // --- Profile Header Section ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(128.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(AppTheme.circle)
                                .background(AppTheme.colors.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photoUri)
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
                                    modifier = Modifier.size(80.dp), 
                                    tint = AppTheme.colors.outline
                                )
                            }
                        }
                        IconButton(
                            onClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(40.dp)
                                .clip(AppTheme.circle)
                                .background(AppTheme.colors.primary),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = AppTheme.colors.onPrimary)
                        ) {
                            Icon(IconMapper.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = firstName,
                        style = AppTheme.typography.headlineMedium,
                        color = AppTheme.colors.onSurface
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
                TonalCard {
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
                    Column(
                        modifier = Modifier.padding(16.dp), 
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text(stringResource(R.string.first_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = AppTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(stringResource(R.string.email)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = AppTheme.shapes.medium
                        )
                        Button(
                            onClick = { 
                                if (firstName.trim().length < 2) {
                                    scope.launch { snackbarHostState.showAppSnackbar(shortNameMsg, type = SnackbarType.ERROR) }
                                    return@Button
                                }
                                if (!email.matches(emailRegex)) {
                                    scope.launch { snackbarHostState.showAppSnackbar(invalidEmailMsg, type = SnackbarType.ERROR) }
                                    return@Button
                                }
                                onSave(firstName, email, photoUri.ifBlank { null })
                                scope.launch { snackbarHostState.showAppSnackbar(successMsg, type = SnackbarType.SUCCESS) }
                                onBackClick() 
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = AppTheme.shapes.medium,
                            enabled = firstName.isNotBlank() && email.isNotBlank()
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }

            // --- Danger Zone Section ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(IconMapper.Warning, contentDescription = null, tint = AppTheme.colors.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.danger_zone), style = AppTheme.typography.labelMedium, color = AppTheme.colors.error)
                    }
                    TonalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(R.string.delete_account), style = AppTheme.typography.titleMedium, color = AppTheme.colors.error)
                                Text(text = stringResource(R.string.delete_account_desc), style = AppTheme.typography.bodySmall, color = AppTheme.colors.outline)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            if (isDeleting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = AppTheme.colors.error)
                            } else {
                                Button(
                                    onClick = { showDeleteDialog.value = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.error),
                                    shape = AppTheme.shapes.medium
                                ) {
                                    Text(stringResource(R.string.delete_label))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
