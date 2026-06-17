package pt.ipt.dama.teacherslog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.ipt.dama.teacherslog.utils.SessionManager

/**
 * AuthViewModel manages the user session state, including onboarding and authentication status.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    val onboardingCompleted = sessionManager.onboardingCompleted.asLiveData()
    val isAuthenticated = sessionManager.isAuthenticated.asLiveData()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
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
     * Persists login status and enters the app.
     */
    fun login() {
        viewModelScope.launch {
            sessionManager.login()
        }
    }

    /**
     * Resets all session data.
     */
    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
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
