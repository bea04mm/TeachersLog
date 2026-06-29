package pt.ipt.dama.TeachersLog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.data.local.AppDatabase
import pt.ipt.dama.TeachersLog.data.remote.NetworkClient
import pt.ipt.dama.TeachersLog.data.repository.AuthRepository
import pt.ipt.dama.TeachersLog.data.repository.SyncRepository
import pt.ipt.dama.TeachersLog.utils.SessionManager
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ipt.dama.TeachersLog.data.workers.ImageUploadWorker
import pt.ipt.dama.TeachersLog.data.workers.ImageDeleteWorker

/**
 * SettingsViewModel manages global UI state such as theme preferences and application language.
 *
 * @param application The application context, used for locale initialization.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val syncRepository: SyncRepository
    private val authRepository = AuthRepository(NetworkClient.apiService, sessionManager)

    /** Observable state for the dark mode theme. */
    val isDarkMode: LiveData<Boolean?> = sessionManager.isDarkMode.asLiveData()

    /** Observable state for the current language code (e.g., "en", "pt"). */
    val currentLanguage: LiveData<String> = sessionManager.language.asLiveData()

    /** Observable state for synchronization preference. */
    val isSyncEnabled: LiveData<Boolean> = sessionManager.isSyncEnabled.asLiveData()

    /** Observable state for the current user's name. */
    val userName: LiveData<String> = sessionManager.userName.asLiveData()

    /** Observable state for the current user's email. */
    val userEmail: LiveData<String> = sessionManager.userEmail.asLiveData()

    /** Observable state for the current user's photo. */
    val userPhoto: LiveData<String> = sessionManager.userPhoto.asLiveData()

    /** Observable state for guest status. */
    val isGuest: LiveData<Boolean> = sessionManager.isGuest.asLiveData()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        syncRepository = SyncRepository(database, database.teacherDao(), NetworkClient.apiService, sessionManager)
    }

    /**
     * Toggles the application's dark mode setting.
     * @param enabled True to enable dark mode, false for light mode.
     */
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setDarkMode(enabled)
            // Also save to SharedPreferences for sync initialization if needed, 
            // but DataStore is preferred for Compose.
        }
    }

    /**
     * Updates the application's current language code.
     * @param languageCode The new language code (e.g., "en" or "pt").
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            sessionManager.setLanguage(languageCode)
            // Persist to SharedPreferences for attachBaseContext on restart
            getApplication<Application>().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE).edit {
                putString("language", languageCode)
            }
        }
    }

    /**
     * Toggles the data synchronization setting and syncs it with AWS.
     * @param enabled True to enable sync, false to disable.
     */
    fun toggleSync(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setSyncEnabled(enabled)
            val email = sessionManager.userEmail.first()
            if (email.isNotBlank()) {
                authRepository.updateSyncPreference(email, enabled)
            }
        }
    }

    /**
     * Downloads the latest backup from AWS and overwrites the local database.
     * Updates [isSyncing] and [syncMessage] during the process.
     */
    fun restoreFromCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            val result = syncRepository.downloadFromCloud()
            if (result.isSuccess) {
                // Return success signal (UI will map to string resource)
                _syncMessage.value = "RESTORE_SUCCESS"
            } else {
                // Return error message
                _syncMessage.value = "RESTORE_FAILED:" + (result.exceptionOrNull()?.message ?: "Unknown Error")
            }
            _isSyncing.value = false
        }
    }

    /**
     * Clears the current synchronization result message.
     */
    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /**
     * Updates the user's profile information in the local session manager.
     * @param name The new display name.
     * @param email The new email address.
     * @param photoUri Optional local URI to upload.
     */
    fun updateProfile(name: String, email: String, photoUri: String? = null) {
        viewModelScope.launch {
            var finalPhotoUrl: String? = null
            
            // If it's a local URI, upload it first
            if (photoUri != null && (photoUri.startsWith("content://") || photoUri.startsWith("file://"))) {
                // Delete old image if it exists and is a cloud URL
                val oldPhoto = sessionManager.userPhoto.first()
                android.util.Log.d("SettingsViewModel", "Old photo URL: $oldPhoto")
                if (oldPhoto.startsWith("http")) {
                    android.util.Log.d("SettingsViewModel", "Deleting old cloud photo: $oldPhoto")
                    try { 
                        val response = NetworkClient.apiService.deleteImage(oldPhoto)
                        if (!response.isSuccessful) {
                            scheduleImageDeletion(oldPhoto)
                        }
                    } catch (_: Exception) {
                        scheduleImageDeletion(oldPhoto)
                    }
                }

                val cloudUrl = uploadImageFromUri(photoUri)
                if (cloudUrl != null) {
                    finalPhotoUrl = cloudUrl
                } else {
                    // Upload failed, schedule background task
                    (-1).scheduleImageUpload(photoUri, isProfile = true)
                }
            }
            
            sessionManager.updateProfile(name, email, finalPhotoUrl)
        }
    }

    /**
     * Schedules a background task to upload an image when internet is available.
     */
    private fun Int.scheduleImageUpload(uri: String, isProfile: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("image_uri", uri)
            .putInt("student_id", this)
            .putBoolean("is_profile", isProfile)
            .build()

        val uploadRequest = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "upload_${if (isProfile) "profile" else this}",
            ExistingWorkPolicy.REPLACE,
            uploadRequest
        )
    }

    /**
     * Schedules a background task to delete an image when internet is available.
     */
    private fun scheduleImageDeletion(url: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("image_url", url)
            .build()

        val deleteRequest = OneTimeWorkRequestBuilder<ImageDeleteWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "delete_${url.hashCode()}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            deleteRequest
        )
    }

    /**
     * Helper to handle the image upload process from a URI.
     */
    private suspend fun uploadImageFromUri(uriString: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val context = getApplication<Application>()
            val uri = uriString.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() }

            if (bytes != null) {
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(mediaType)

                val response = NetworkClient.apiService.uploadImage(requestBody)
                if (response.isSuccessful) response.body()?.imageUrl else null
            } else null
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Error uploading profile image", e)
            null
        }
    }

    /**
     * Permanently deletes the user account from AWS and the local device.
     * Redirects to the welcome/onboarding screen upon success via state change.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            val email = sessionManager.userEmail.first()
            if (email.isNotBlank()) {
                _isSyncing.value = true
                authRepository.deleteAccount(email)
                _isSyncing.value = false
            }
        }
    }
}

/**
 * Factory for [SettingsViewModel].
 */
class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
