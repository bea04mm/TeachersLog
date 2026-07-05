package pt.ipt.dama.TeachersLog.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.ui.components.TeacherTopBar
import pt.ipt.dama.TeachersLog.ui.components.TonalCard
import pt.ipt.dama.TeachersLog.ui.components.LocalSnackbarHostState
import pt.ipt.dama.TeachersLog.ui.components.SnackbarType
import pt.ipt.dama.TeachersLog.ui.components.showAppSnackbar
import pt.ipt.dama.TeachersLog.ui.theme.AppTheme
import pt.ipt.dama.TeachersLog.ui.viewmodel.SettingsViewModel

/**
 * SecuritySettingsScreen provides an interface for authenticated users to change their account password.
 * 
 * It includes validation for password length, confirmation matching, and 
 * handles server-side validation for the current (old) password.
 * 
 * @param viewModel The [SettingsViewModel] to handle password change requests.
 * @param onBackClick Callback for navigating back to the previous screen.
 */
@Composable
fun SecuritySettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    // State for password fields
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    
    // Resolve string resources for feedback
    val successMsg = stringResource(R.string.password_changed_success)
    val mismatchMsg = stringResource(R.string.error_password_mismatch)
    val shortPassMsg = stringResource(R.string.error_password_too_short)
    val incorrectOldMsg = stringResource(R.string.error_incorrect_old_password)
    val genericErrorMsg = stringResource(R.string.error_unknown)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = AppTheme.colors.background,
        topBar = {
            TeacherTopBar(
                title = stringResource(R.string.security),
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
                TonalCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.change_password),
                            style = AppTheme.typography.titleMedium,
                            color = AppTheme.colors.primary
                        )

                        // --- Input Fields ---

                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = { Text(stringResource(R.string.old_password)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = AppTheme.shapes.medium,
                            enabled = !isProcessing
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text(stringResource(R.string.new_password)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = AppTheme.shapes.medium,
                            enabled = !isProcessing
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text(stringResource(R.string.confirm_new_password)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = AppTheme.shapes.medium,
                            enabled = !isProcessing
                        )

                        // --- Action Button ---

                        Button(
                            onClick = {
                                // 1. Local Validation: Mismatch
                                if (newPassword != confirmPassword) {
                                    scope.launch { snackbarHostState.showAppSnackbar(mismatchMsg, type = SnackbarType.ERROR) }
                                    return@Button
                                }
                                // 2. Local Validation: Length
                                if (newPassword.length < 6) {
                                    scope.launch { snackbarHostState.showAppSnackbar(shortPassMsg, type = SnackbarType.ERROR) }
                                    return@Button
                                }

                                isProcessing = true
                                // 3. Remote Update Request
                                viewModel.changePassword(oldPassword, newPassword) { error ->
                                    isProcessing = false
                                    if (error == null) {
                                        scope.launch { snackbarHostState.showAppSnackbar(successMsg, type = SnackbarType.SUCCESS) }
                                        onBackClick()
                                    } else {
                                        // Handle specific server-side errors
                                        val msg = if (error == "INCORRECT_PASSWORD") incorrectOldMsg else genericErrorMsg
                                        scope.launch { snackbarHostState.showAppSnackbar(msg, type = SnackbarType.ERROR) }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = AppTheme.shapes.medium,
                            enabled = oldPassword.isNotBlank() && newPassword.isNotBlank() && !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp), 
                                    strokeWidth = 2.dp, 
                                    color = AppTheme.colors.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
        }
    }
}
