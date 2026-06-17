package pt.ipt.dama.teacherslog.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        }
    }

    /**
     * Configures the session as logged in.
     */
    suspend fun login() {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[IS_GUEST] = false
        }
    }

    /**
     * Clears all session data (Logout/Reset).
     */
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[IS_GUEST] = false
            preferences[IS_LOGGED_IN] = false
            preferences[ONBOARDING_COMPLETED] = false
        }
    }
}
