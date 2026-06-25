package pt.ipt.dama.teacherslog.data.repository

import pt.ipt.dama.teacherslog.data.remote.ApiService
import pt.ipt.dama.teacherslog.data.remote.models.*
import pt.ipt.dama.teacherslog.utils.SessionManager

/**
 * Repository for handling authentication network calls and local session persistence.
 */
/**
 * AuthRepository manages authentication logic, communicating with the [ApiService]
 * for cloud operations and updating the [SessionManager] for local state.
 *
 * @property apiService The Retrofit service for network calls.
 * @property sessionManager Manager for local user session persistence.
 */
class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {

    /**
     * Registers a new user on AWS and saves data locally if successful.
     * 
     * @param request Registration details.
     * @return [Result] containing the [AuthResponse].
     */
    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                sessionManager.saveAuthData(data.token, data.name, data.email, data.syncEnabled)
                Result.success(data)
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates a user on AWS and saves the JWT token locally.
     * 
     * @param request Login credentials.
     * @return [Result] containing the [AuthResponse].
     */
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                sessionManager.saveAuthData(data.token, data.name, data.email, data.syncEnabled)
                Result.success(data)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logs out the current user and clears the local session.
     */
    suspend fun logout() {
        sessionManager.clearSession()
    }

    /**
     * Updates the synchronization preference on AWS.
     * 
     * @param email The account identifier.
     * @param enabled The new sync status.
     * @return [Result] indicating network success or failure.
     */
    suspend fun updateSyncPreference(email: String, enabled: Boolean): Result<Unit> {
        return try {
            val response = apiService.updateSyncPreference(UpdateSyncRequest(email, enabled))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update sync preference: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Permanently deletes the user account from the AWS backend.
     * 
     * @param email The identifier of the account to delete.
     * @return [Result] indicating success or failure.
     */
    suspend fun deleteAccount(email: String): Result<Unit> {
        return try {
            val response = apiService.deleteAccount(email)
            if (response.isSuccessful) {
                sessionManager.clearSession()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Deletion failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
