package pt.ipt.dama.TeachersLog.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import pt.ipt.dama.TeachersLog.data.local.AppDatabase
import pt.ipt.dama.TeachersLog.data.local.TeacherDao
import pt.ipt.dama.TeachersLog.data.remote.ApiService
import pt.ipt.dama.TeachersLog.data.remote.models.SyncRequest
import pt.ipt.dama.TeachersLog.data.remote.models.UserDataSync
import pt.ipt.dama.TeachersLog.utils.SessionManager

/**
 * SyncRepository manages the synchronization of data between the local Room database
 * and the AWS cloud backend.
 *
 * It provides methods to create a full state snapshot of the local database and upload it
 * to the cloud, as well as methods to download and restore that state.
 *
 * @property database The [AppDatabase] instance for transaction support.
 * @property teacherDao The DAO for accessing local teacher data.
 * @property apiService The Retrofit service for cloud communication.
 * @property sessionManager Manager for user session and email identification.
 */
class SyncRepository(
    private val database: AppDatabase,
    private val teacherDao: TeacherDao,
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    // Safety flag to prevent uploading while a restore (download) is in progress
    private var isRestoring = false

    /**
     * Uploads the entire local database state to the AWS cloud.
     * Gathers all entities from local tables and sends them as a single JSON snapshot.
     * 
     * @return A [Result] indicating success or containing the caught exception.
     */
    suspend fun uploadToCloud(): Result<Unit> {
        if (isRestoring) return Result.success(Unit)
        return try {
            val email = sessionManager.userEmail.first()
            if (email.isBlank()) return Result.failure(Exception("User not authenticated"))

            val snapshot = UserDataSync(
                years = teacherDao.getAllAcademicYearsSync(),
                semesters = teacherDao.getAllSemestersSync(),
                subjects = teacherDao.getAllSubjectsSync(),
                classes = teacherDao.getAllClassesSync(),
                components = teacherDao.getAllGradingComponentsSync(),
                students = teacherDao.getAllStudentsSync(),
                scores = teacherDao.getAllStudentScoresSync(),
                grades = teacherDao.getAllGradesSync()
            )

            val response = apiService.syncData(SyncRequest(email, snapshot))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads the latest data snapshot from the cloud and overwrites the local database.
     * 
     * WARNING: This operation is destructive as it deletes all current local data before
     * inserting the downloaded records.
     * 
     * @return A [Result] indicating success or containing the caught exception.
     */
    suspend fun downloadFromCloud(): Result<Unit> {
        return try {
            val email = sessionManager.userEmail.first()
            if (email.isBlank()) return Result.failure(Exception("User not authenticated"))

            isRestoring = true
            val response = apiService.downloadData(email)
            if (response.isSuccessful && response.body() != null) {
                overwriteLocalData(response.body()!!)
                isRestoring = false
                Result.success(Unit)
            } else {
                isRestoring = false
                Result.failure(Exception("Download failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            isRestoring = false
            Result.failure(e)
        }
    }
    
    /**
     * Helper to perform a full local database overwrite with cloud data within a transaction.
     * 
     * @param data The [UserDataSync] snapshot containing all academic entities.
     */
    private suspend fun overwriteLocalData(data: UserDataSync) {
        database.withTransaction {
            teacherDao.clearAllData()
            teacherDao.insertAcademicYears(data.years)
            teacherDao.insertSemesters(data.semesters)
            teacherDao.insertSubjects(data.subjects)
            teacherDao.insertStudentsClasses(data.classes)
            teacherDao.insertGradingComponents(data.components)
            teacherDao.insertStudents(data.students)
            teacherDao.insertStudentScores(data.scores)
            teacherDao.insertGrades(data.grades)
        }
    }
}
