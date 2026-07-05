package pt.ipt.dama.TeachersLog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.data.local.AppDatabase
import pt.ipt.dama.TeachersLog.data.local.TeacherDao
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
import pt.ipt.dama.TeachersLog.data.model.UserProfile
import pt.ipt.dama.TeachersLog.data.workers.ImageUploadWorker
import pt.ipt.dama.TeachersLog.data.workers.ImageDeleteWorker

/**
 * SettingsViewModel manages global UI state such as theme preferences and application language.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val syncRepository: SyncRepository
    private val authRepository: AuthRepository
    private val teacherDao: TeacherDao

    /** Observable state for the dark mode theme. */
    val isDarkMode: LiveData<Boolean?> = sessionManager.isDarkMode.asLiveData()

    /** Observable state for the current language code (e.g., "en", "pt"). */
    val currentLanguage: LiveData<String> = sessionManager.language.asLiveData()

    /** Observable state for synchronization preference. */
    val isSyncEnabled: LiveData<Boolean> = sessionManager.isSyncEnabled.asLiveData()

    /** Observable state for the current user's profile from the database. */
    val userProfile: LiveData<UserProfile?>

    /** Observable state for guest status. */
    val isGuest: LiveData<Boolean> = sessionManager.isGuest.asLiveData()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        teacherDao = database.teacherDao()
        syncRepository = SyncRepository(database, teacherDao, NetworkClient.apiService, sessionManager)
        authRepository = AuthRepository(NetworkClient.apiService, sessionManager, teacherDao)
        userProfile = teacherDao.getUserProfile().asLiveData()
    }

    /**
     * Toggles the application's dark mode setting.
     */
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setDarkMode(enabled)
        }
    }

    /**
     * Updates the application's current language code.
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            sessionManager.setLanguage(languageCode)
            getApplication<Application>().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE).edit {
                putString("language", languageCode)
            }
        }
    }

    /**
     * Toggles the data synchronization setting and syncs it with AWS.
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
     */
    fun restoreFromCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            val result = syncRepository.downloadFromCloud()
            if (result.isSuccess) {
                _syncMessage.value = "RESTORE_SUCCESS"
            } else {
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
     * Clears the user's cloud backup and associated images from S3.
     */
    fun clearCloudBackup() {
        viewModelScope.launch {
            _isSyncing.value = true
            syncRepository.clearCloudBackup()
            _isSyncing.value = false
        }
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
            
            if (photoUri != null) {
                if (photoUri.startsWith("http")) {
                    finalPhotoUrl = photoUri
                } else if (photoUri.startsWith("content://") || photoUri.startsWith("file://")) {
                    // Upload first
                    val cloudUrl = uploadImageFromUri(photoUri)
                    if (cloudUrl != null) {
                        finalPhotoUrl = cloudUrl
                    } else {
                        // Background upload if immediate fails
                        (-1).scheduleImageUpload(photoUri, isProfile = true)
                        finalPhotoUrl = photoUri // Keep local URI for now
                    }
                }
            } else {
                // If photoUri is null, we might want to keep the old one or clear it.
                // In this app's logic, null usually means no change requested if name/email are provided.
                finalPhotoUrl = teacherDao.getUserProfile().first()?.photoUrl
            }
            
            // Save to Room (Database)
            teacherDao.insertUserProfile(UserProfile(name = name, email = email, photoUrl = finalPhotoUrl))
            
            // Save to DataStore (Session)
            sessionManager.updateProfile(name, email, finalPhotoUrl)
            
            // Trigger sync to update top-level columns in DynamoDB
            triggerSync()
        }
    }

    /**
     * Triggers a cloud backup if sync is enabled.
     */
    private fun triggerSync() {
        viewModelScope.launch {
            val enabled = sessionManager.isSyncEnabled.first()
            if (enabled) {
                syncRepository.uploadToCloud()

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = OneTimeWorkRequestBuilder<pt.ipt.dama.TeachersLog.data.workers.DataSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "full_data_sync_settings",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            }
        }
    }

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
            null
        }
    }

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
