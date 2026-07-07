package pt.ipt.dama.TeachersLog.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.ui.components.AppSnackbar
import pt.ipt.dama.TeachersLog.ui.components.LocalSnackbarHostState
import pt.ipt.dama.TeachersLog.ui.components.SnackbarType
import pt.ipt.dama.TeachersLog.ui.components.showAppSnackbar
import pt.ipt.dama.TeachersLog.ui.theme.AppTheme
import pt.ipt.dama.TeachersLog.ui.viewmodel.AuthViewModel
import pt.ipt.dama.TeachersLog.utils.IconMapper

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
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    // Validation messages
    val invalidEmailMsg = stringResource(R.string.error_invalid_email)
    val shortPasswordMsg = stringResource(R.string.error_password_too_short)
    val shortNameMsg = stringResource(R.string.error_name_too_short)

    // Handle authentication errors from the backend
    LaunchedEffect(authError) {
        if (authError != null) {
            scope.launch {
                snackbarHostState.showAppSnackbar(authError!!, type = SnackbarType.ERROR)
                authViewModel.clearError()
            }
        }
    }

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

    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$".toRegex()
    
    fun validateAndSubmit(isRegister: Boolean) {
        if (isRegister && name.trim().length < 3) {
            scope.launch { snackbarHostState.showAppSnackbar(shortNameMsg, type = SnackbarType.ERROR) }
            return
        }
        if (!email.matches(emailRegex)) {
            scope.launch { snackbarHostState.showAppSnackbar(invalidEmailMsg, type = SnackbarType.ERROR) }
            return
        }
        if (password.length < 6) {
            scope.launch { snackbarHostState.showAppSnackbar(shortPasswordMsg, type = SnackbarType.ERROR) }
            return
        }
        
        if (isRegister) {
            authViewModel.register(name, email, password, syncEnabled)
        } else {
            authViewModel.login(email, password)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
        containerColor = AppTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) { AppSnackbar(it) } }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        .clip(AppTheme.circle)
                        .background(AppTheme.white),
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
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = AppTheme.shapes.medium
                        ) {
                            Text(stringResource(R.string.sign_in))
                        }
                        
                        OutlinedButton(
                            onClick = { viewState = WelcomeViewState.REGISTER; authViewModel.clearError() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = AppTheme.shapes.medium
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
                        isLoading = isLoading,
                        onSubmit = { validateAndSubmit(false) },
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
                        isLoading = isLoading,
                        onSubmit = { validateAndSubmit(true) },
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

        if (isRegister) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.first_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                shape = AppTheme.shapes.medium
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            shape = AppTheme.shapes.medium
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.password_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            shape = AppTheme.shapes.medium,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) IconMapper.Visibility else IconMapper.VisibilityOff
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
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && (!isRegister || name.isNotBlank()),
            shape = AppTheme.shapes.medium
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
