package pt.ipt.dama.teacherslog.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.ipt.dama.teacherslog.R
import pt.ipt.dama.teacherslog.ui.theme.AppTheme
import pt.ipt.dama.teacherslog.ui.viewmodel.AuthViewModel

enum class WelcomeViewState {
    LANDING, LOGIN, REGISTER
}

@Composable
fun WelcomeScreen(
    authViewModel: AuthViewModel,
    onGuestClick: () -> Unit
) {
    var viewState by remember { mutableStateOf(WelcomeViewState.LANDING) }
    val isLoading by authViewModel.isLoading.collectAsState()
    val authError by authViewModel.authError.collectAsState()

    // Handle back button/gesture when not in landing page
    BackHandler(enabled = viewState != WelcomeViewState.LANDING) {
        viewState = WelcomeViewState.LANDING
        authViewModel.clearError()
    }

    // Form states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var syncEnabled by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (viewState == WelcomeViewState.LANDING) Arrangement.SpaceBetween else Arrangement.Top
        ) {
            if (viewState == WelcomeViewState.LANDING) {
                Spacer(modifier = Modifier.height(64.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(if (viewState == WelcomeViewState.LANDING) 160.dp else 100.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .size(if (viewState == WelcomeViewState.LANDING) 160.dp else 100.dp)
                            .scale(1.6f)
                    )
                }
                Spacer(modifier = Modifier.height(if (viewState == WelcomeViewState.LANDING) 24.dp else 16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = if (viewState == WelcomeViewState.LANDING) AppTheme.typography.headlineLarge else AppTheme.typography.headlineSmall,
                    color = AppTheme.colors.primary
                )
                if (viewState == WelcomeViewState.LANDING) {
                    Text(
                        text = stringResource(R.string.choose_how_to_continue),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Content Area
            when (viewState) {
                WelcomeViewState.LANDING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { viewState = WelcomeViewState.LOGIN; authViewModel.clearError() },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(stringResource(R.string.sign_in))
                        }
                        
                        OutlinedButton(
                            onClick = { viewState = WelcomeViewState.REGISTER; authViewModel.clearError() },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(stringResource(R.string.create_account))
                        }

                        TextButton(
                            onClick = onGuestClick,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(stringResource(R.string.continue_as_guest))
                        }
                        
                        Text(
                            text = stringResource(R.string.guest_disclaimer),
                            style = AppTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = AppTheme.colors.outline,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                WelcomeViewState.LOGIN -> {
                    AuthForm(
                        isRegister = false,
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        name = name,
                        onNameChange = { name = it },
                        syncEnabled = syncEnabled,
                        onSyncChange = { syncEnabled = it },
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        error = authError,
                        isLoading = isLoading,
                        onSubmit = { authViewModel.login(email, password) },
                        onBack = { viewState = WelcomeViewState.LANDING }
                    )
                }
                WelcomeViewState.REGISTER -> {
                    AuthForm(
                        isRegister = true,
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        name = name,
                        onNameChange = { name = it },
                        syncEnabled = syncEnabled,
                        onSyncChange = { syncEnabled = it },
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        error = authError,
                        isLoading = isLoading,
                        onSubmit = { authViewModel.register(name, email, password, syncEnabled) },
                        onBack = { viewState = WelcomeViewState.LANDING }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthForm(
    isRegister: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    syncEnabled: Boolean,
    onSyncChange: (Boolean) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    error: String?,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isRegister) stringResource(R.string.create_account) else stringResource(R.string.sign_in),
            style = AppTheme.typography.titleLarge,
            color = AppTheme.colors.onBackground
        )

        if (error != null) {
            Text(
                text = error,
                color = AppTheme.colors.error,
                style = AppTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        if (isRegister) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.first_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = onTogglePassword) {
                    Icon(imageVector = image, contentDescription = null)
                }
            }
        )

        if (isRegister) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = syncEnabled,
                    onCheckedChange = onSyncChange,
                    enabled = !isLoading
                )
                Text(
                    text = stringResource(R.string.sync_label),
                    style = AppTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && (!isRegister || name.isNotBlank())
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AppTheme.colors.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isRegister) stringResource(R.string.create_account) else stringResource(R.string.sign_in))
            }
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(stringResource(R.string.back))
        }
    }
}
