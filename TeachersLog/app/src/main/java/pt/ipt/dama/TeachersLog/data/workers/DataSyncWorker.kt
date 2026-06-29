package pt.ipt.dama.TeachersLog.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pt.ipt.dama.TeachersLog.data.local.AppDatabase
import pt.ipt.dama.TeachersLog.data.remote.NetworkClient
import pt.ipt.dama.TeachersLog.data.repository.SyncRepository
import pt.ipt.dama.TeachersLog.utils.SessionManager

/**
 * Background worker that handles retrying the full database sync when internet returns.
 */
class DataSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        val syncRepository = SyncRepository(database, database.teacherDao(), NetworkClient.apiService, sessionManager)

        return try {
            val result = syncRepository.uploadToCloud()
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
