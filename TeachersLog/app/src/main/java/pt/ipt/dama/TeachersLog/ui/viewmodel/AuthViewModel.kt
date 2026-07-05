package pt.ipt.dama.TeachersLog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.data.local.AppDatabase
import pt.ipt.dama.TeachersLog.data.remote.NetworkClient
import pt.ipt.dama.TeachersLog.data.remote.models.LoginRequest
import pt.ipt.dama.TeachersLog.data.remote.models.RegisterRequest
import pt.ipt.dama.TeachersLog.data.repository.AuthRepository
import pt.ipt.dama.TeachersLog.utils.SessionManager

/**
 * AuthViewModel manages the user session state, including onboarding and authentication status.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val authRepository: AuthRepository

    // Expose flows directly for more reliable Compose observation
    val onboardingCompletedFlow = sessionManager.onboardingCompleted
    val isAuthenticatedFlow = sessionManager.isAuthenticated

    val isGuest = sessionManager.isGuest.asLiveData()
    val isAuthenticated = sessionManager.isAuthenticated.asLiveData()
    val isSyncEnabled = sessionManager.isSyncEnabled.asLiveData()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _justLoggedIn = MutableStateFlow(false)
    /** Transient state to trigger post-login logic like data conflict checks. */
    val justLoggedIn: StateFlow<Boolean> = _justLoggedIn.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        authRepository = AuthRepository(NetworkClient.apiService, sessionManager, database.teacherDao())

        // Simple delay to ensure splash screen shows and state is loaded
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _isLoading.value = false
        }
    }

    /**
     * Completes onboarding and persists state.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            sessionManager.setOnboardingCompleted()
        }
    }

    /**
     * Persists guest status and enters the app.
     */
    fun continueAsGuest() {
        viewModelScope.launch {
            sessionManager.continueAsGuest()
        }
    }

    /**
     * Registers a new user via AWS API.
     */
    fun register(name: String, email: String, pass: String, sync: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.register(RegisterRequest(email, pass, name, sync))
            if (result.isSuccess) {
                _justLoggedIn.value = true
            } else {
                val error = result.exceptionOrNull()?.message
                _authError.value = when (error) {
                    "USER_EXISTS" -> getApplication<Application>().getString(pt.ipt.dama.TeachersLog.R.string.error_user_exists)
                    "REGISTRATION_FAILED" -> getApplication<Application>().getString(pt.ipt.dama.TeachersLog.R.string.error_registration_failed)
                    else -> error ?: getApplication<Application>().getString(pt.ipt.dama.TeachersLog.R.string.error_unknown)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Authenticates the user via AWS API.
     */
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.login(LoginRequest(email, pass))
            if (result.isSuccess) {
                _justLoggedIn.value = true
            } else {
                val error = result.exceptionOrNull()?.message
                _authError.value = when (error) {
                    "INVALID_CREDENTIALS" -> getApplication<Application>().getString(pt.ipt.dama.TeachersLog.R.string.error_invalid_credentials)
                    "LOGIN_FAILED" -> getApplication<Application>().getString(pt.ipt.dama.TeachersLog.R.string.error_login_failed)
                    else -> error ?: getApplication<Application>().getString(pt.ipt.dama.TeachersLog.R.string.error_unknown)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Resets the just logged in flag after logic is handled.
     */
    fun clearJustLoggedIn() {
        _justLoggedIn.value = false
    }

    /**
     * Resets all session data.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun clearError() {
        _authError.value = null
    }
}

class AuthViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
