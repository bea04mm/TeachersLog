package pt.ipt.dama.teacherslog.utils

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
        private val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
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
    suspend fun saveAuthData(token: String, name: String, email: String, syncEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[IS_GUEST] = false
            preferences[AUTH_TOKEN] = token
            preferences[USER_NAME] = name
            preferences[USER_EMAIL] = email
            preferences[SYNC_ENABLED] = syncEnabled
        }
    }

    /**
     * Updates only the user profile data.
     */
    suspend fun updateProfile(name: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
            preferences[USER_EMAIL] = email
        }
    }

    /**
     * Updates only the sync preference.
     */
    suspend fun setSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_ENABLED] = enabled
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
            // Keep ONBOARDING_COMPLETED as true so we go to Welcome instead of Tutorial
        }
    }
}
