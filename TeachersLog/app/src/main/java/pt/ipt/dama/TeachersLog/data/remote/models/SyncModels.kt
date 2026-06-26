package pt.ipt.dama.TeachersLog.data.remote.models

import com.google.gson.annotations.SerializedName
import pt.ipt.dama.TeachersLog.data.model.*

/**
 * A complete snapshot of the user's local database for cloud backup.
 */
data class UserDataSync(
    @SerializedName("academicYears") val years: List<AcademicYear>,
    @SerializedName("semesters") val semesters: List<Semester>,
    @SerializedName("subjects") val subjects: List<Subject>,
    @SerializedName("classes") val classes: List<StudentsClass>,
    @SerializedName("gradingComponents") val components: List<GradingComponent>,
    @SerializedName("students") val students: List<Student>,
    @SerializedName("studentScores") val scores: List<StudentScore>,
    @SerializedName("grades") val grades: List<Grade>,
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Request object for the sync endpoint.
 */
data class SyncRequest(
    @SerializedName("email") val email: String,
    @SerializedName("data") val data: UserDataSync
)
