package pt.ipt.dama.TeachersLog.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pt.ipt.dama.TeachersLog.data.remote.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker that handles retrying image deletions from S3 when internet returns.
 */
class ImageDeleteWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val imageUrl = inputData.getString("image_url") ?: return@withContext Result.failure()

        try {
            val response = NetworkClient.apiService.deleteImage(imageUrl)
            if (response.isSuccessful) {
                Result.success()
            } else {
                // If it's 404, the image is already gone, so we're done
                if (response.code() == 404) {
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageDeleteWorker", "Error deleting image in background", e)
            Result.retry()
        }
    }
}
