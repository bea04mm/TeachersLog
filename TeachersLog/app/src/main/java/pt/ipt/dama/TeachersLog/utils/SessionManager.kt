package pt.ipt.dama.TeachersLog.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

/**
 * SessionManager handles persistent user session state using Jetpack DataStore.
 * It tracks onboarding completion and authentication status.
 */
class SessionManager(private val context: Context) {

    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val IS_GUEST = booleanPreferencesKey("is_guest")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_PHOTO = stringPreferencesKey("user_photo")
        private val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        private val LANGUAGE = stringPreferencesKey("language")
    }

    /**
     * Flow emitting the dark mode preference.
     */
    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE]
    }

    /**
     * Flow emitting the language preference.
     */
    val language: Flow<String> = context.dataStore.data.map { preferences ->
        // Default to system language if not set
        preferences[LANGUAGE] ?: java.util.Locale.getDefault().language
    }

    /**
     * Flow emitting whether the user has completed the initial onboarding.
     */
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    /**
     * Flow emitting the current authentication status.
     */
    val isAuthenticated: Flow<Boolean> = context.dataStore.data.map { preferences ->
        (preferences[IS_GUEST] ?: false) || (preferences[IS_LOGGED_IN] ?: false)
    }

    /**
     * Flow emitting whether synchronization is enabled.
     */
    val isSyncEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SYNC_ENABLED] ?: false
    }

    /**
     * Flow emitting the current user's name.
     */
    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME] ?: ""
    }

    /**
     * Flow emitting the current user's email.
     */
    val userEmail: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL] ?: ""
    }

    /**
     * Flow emitting the current user's profile photo URL.
     */
    val userPhoto: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_PHOTO] ?: ""
    }

    /**
     * Flow emitting if the user is a guest.
     */
    val isGuest: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_GUEST] ?: false
    }

    /**
     * Marks the onboarding as completed.
     */
    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
        }
    }

    /**
     * Configures the session to continue as a guest.
     */
    suspend fun continueAsGuest() {
        context.dataStore.edit { preferences ->
            preferences[IS_GUEST] = true
            preferences[IS_LOGGED_IN] = false
            preferences[SYNC_ENABLED] = false // Guests cannot sync
        }
    }

    /**
     * Configures the session as logged in with user data and token.
     */
    suspend fun saveAuthData(token: String, name: String, email: String, photoUrl: String?, syncEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[IS_GUEST] = false
            preferences[AUTH_TOKEN] = token
            preferences[USER_NAME] = name
            preferences[USER_EMAIL] = email
            preferences[USER_PHOTO] = photoUrl ?: ""
            preferences[SYNC_ENABLED] = syncEnabled
        }
    }

    /**
     * Updates only the user profile data.
     */
    suspend fun updateProfile(name: String, email: String, photoUrl: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
            preferences[USER_EMAIL] = email
            photoUrl?.let { preferences[USER_PHOTO] = it }
        }
    }

    /**
     * Updates the sync preference.
     */
    suspend fun setSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_ENABLED] = enabled
        }
    }

    /**
     * Updates the dark mode preference.
     */
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = enabled
        }
    }

    /**
     * Updates the language preference.
     */
    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = language
        }
    }

    /**
     * Clears all session data (Logout/Reset) but keeps onboarding status.
     */
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[IS_GUEST] = false
            preferences[IS_LOGGED_IN] = false
            preferences[AUTH_TOKEN] = ""
            preferences[USER_NAME] = ""
            preferences[USER_EMAIL] = ""
            preferences[USER_PHOTO] = ""
            // Keep ONBOARDING_COMPLETED as true so we go to Welcome instead of Tutorial
        }
    }
}
