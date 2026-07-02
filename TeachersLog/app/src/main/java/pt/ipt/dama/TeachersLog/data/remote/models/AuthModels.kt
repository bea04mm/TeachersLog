package pt.ipt.dama.TeachersLog.data.remote.models

import com.google.gson.annotations.SerializedName

/**
 * Data model for registration request.
 */
data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("name") val name: String,
    @SerializedName("syncEnabled") val syncEnabled: Boolean
)

/**
 * Data model for login request.
 */
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/**
 * Data model for authentication response from AWS.
 */
data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("profileImageUrl") val profileImageUrl: String?,
    @SerializedName("syncEnabled") val syncEnabled: Boolean
)

/**
 * Data model for updating synchronization preference.
 */
data class UpdateSyncRequest(
    @SerializedName("email") val email: String,
    @SerializedName("syncEnabled") val syncEnabled: Boolean
)

/**
 * Response for image upload operations.
 */
data class ImageResponse(
    @SerializedName("imageUrl") val imageUrl: String
)
