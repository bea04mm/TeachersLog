package pt.ipt.dama.TeachersLog.data.workers

import android.content.Context
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import pt.ipt.dama.TeachersLog.data.local.AppDatabase
import pt.ipt.dama.TeachersLog.data.remote.NetworkClient
import pt.ipt.dama.TeachersLog.utils.SessionManager
import kotlinx.coroutines.flow.first

/**
 * Background worker that handles retrying image uploads when internet connectivity is restored.
 */
class ImageUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val imageUri = inputData.getString("image_uri") ?: return@withContext Result.failure()
        val studentId = inputData.getInt("student_id", -1)
        val isProfile = inputData.getBoolean("is_profile", false)

        try {
            val uri = imageUri.toUri()
            val inputStream = applicationContext.contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() }

            if (bytes != null) {
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(mediaType)

                val response = NetworkClient.apiService.uploadImage(requestBody)
                if (response.isSuccessful) {
                    val cloudUrl = response.body()?.imageUrl
                    if (cloudUrl != null) {
                        updateLocalReference(studentId, isProfile, cloudUrl)
                        return@withContext Result.success()
                    }
                }
            }
            // If we reached here without returning success, something failed (likely network)
            Result.retry()
        } catch (e: Exception) {
            android.util.Log.e("ImageUploadWorker", "Error in background upload", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun updateLocalReference(id: Int, isProfile: Boolean, url: String) {
        val database = AppDatabase.getDatabase(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        
        if (isProfile) {
            sessionManager.updateProfile(
                sessionManager.userName.first(),
                sessionManager.userEmail.first(),
                url
            )
        } else if (id != -1) {
            val dao = database.teacherDao()
            val student = dao.getStudentById(id)
            student?.let {
                dao.updateStudent(it.copy(profileImageUrl = url))
            }
        }
    }
}
