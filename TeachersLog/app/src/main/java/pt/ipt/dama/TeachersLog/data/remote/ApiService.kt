package pt.ipt.dama.TeachersLog.data.remote

import pt.ipt.dama.TeachersLog.data.remote.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface defining the AWS Lambda endpoints for authentication and data synchronization.
 */
interface ApiService {

    /**
     * Registers a new user account.
     * @param request Contains email, password, name, and sync preference.
     * @return [Response] with [AuthResponse] containing the JWT token and user profile.
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    /**
     * Authenticates an existing user.
     * @param request Contains email and password.
     * @return [Response] with [AuthResponse] containing the JWT token and user profile.
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /**
     * Updates the user's synchronization preference on the backend.
     * @param request Contains the user email and new sync preference.
     * @return [Response] with Unit.
     */
    @POST("auth/update-sync")
    suspend fun updateSyncPreference(@Body request: UpdateSyncRequest): Response<Unit>

    /**
     * Permanently deletes the user account and all associated cloud data.
     * @param email The identifier of the account to delete.
     * @return [Response] with Unit (200 OK if successful).
     */
    @DELETE("auth/delete")
    suspend fun deleteAccount(@Query("email") email: String): Response<Unit>

    /**
     * Uploads a full snapshot of the user's local database.
     * @param request Contains the user's email and the [UserDataSync] snapshot.
     * @return [Response] with Unit (200 OK if successful).
     */
    @POST("sync")
    suspend fun syncData(@Body request: SyncRequest): Response<Unit>

    /**
     * Downloads the latest data snapshot for the given user.
     * @param email The user identifier for whom to retrieve the backup.
     * @return [Response] containing the full [UserDataSync] snapshot.
     */
    @GET("sync")
    suspend fun downloadData(@Query("email") email: String): Response<UserDataSync>
}
