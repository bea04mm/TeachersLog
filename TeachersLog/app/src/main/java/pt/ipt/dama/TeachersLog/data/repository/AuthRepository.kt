package pt.ipt.dama.TeachersLog.data.repository

import pt.ipt.dama.TeachersLog.data.local.TeacherDao
import pt.ipt.dama.TeachersLog.data.remote.ApiService
import pt.ipt.dama.TeachersLog.data.remote.models.*
import pt.ipt.dama.TeachersLog.data.model.UserProfile
import pt.ipt.dama.TeachersLog.utils.SessionManager

/**
 * Repository for handling authentication network calls and local session persistence.
 */
/**
 * AuthRepository manages authentication logic, communicating with the [ApiService]
 * for cloud operations and updating the [SessionManager] for local state.
 *
 * @property apiService The Retrofit service for network calls.
 * @property sessionManager Manager for local user session persistence.
 * @property teacherDao The DAO for updating the local UserProfile table.
 */
class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val teacherDao: TeacherDao
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
                sessionManager.saveAuthData(data.token, data.name, data.email, data.profileImageUrl, data.syncEnabled)
                teacherDao.insertUserProfile(UserProfile(name = data.name, email = data.email, photoUrl = data.profileImageUrl))
                Result.success(data)
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "USER_EXISTS"
                    else -> "REGISTRATION_FAILED"
                }
                Result.failure(Exception(errorMsg))
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
                sessionManager.saveAuthData(data.token, data.name, data.email, data.profileImageUrl, data.syncEnabled)
                teacherDao.insertUserProfile(UserProfile(name = data.name, email = data.email, photoUrl = data.profileImageUrl))
                Result.success(data)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "INVALID_CREDENTIALS"
                    else -> "LOGIN_FAILED"
                }
                Result.failure(Exception(errorMsg))
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
        teacherDao.deleteUserProfile()
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
            // 1. Clean up all student images from S3 first
            val students = teacherDao.getAllStudentsSync()
            students.forEach { student ->
                student.profileImageUrl?.let { url ->
                    if (url.startsWith("http")) {
                        try { 
                            apiService.deleteImage(url) 
                        } catch (_: Exception) {
                            // Non-blocking: attempt to delete but continue account deletion
                        }
                    }
                }
            }

            // 2. Delete the user account from the AWS backend
            val response = apiService.deleteAccount(email)
            if (response.isSuccessful) {
                // 3. Clear all local data
                teacherDao.clearAllData()
                teacherDao.deleteUserProfile()
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
