package pt.ipt.dama.TeachersLog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pt.ipt.dama.TeachersLog.data.model.*

/**
 * AppDatabase is the primary Room database configuration for the application.
 * 
 * It defines the schema, including all entities and the version number, and 
 * provides access to the [TeacherDao].
 */
@Database(
    entities = [
        AcademicYear::class, 
        Semester::class,
        Subject::class, 
        GradingComponent::class, 
        StudentsClass::class, 
        Student::class, 
        StudentScore::class,
        Grade::class,
        UserProfile::class
    ], 
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to the Data Access Object (DAO) for teacher-related operations.
     */
    abstract fun teacherDao(): TeacherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton instance of the [AppDatabase].
         * 
         * Uses a thread-safe synchronized block to ensure only one instance of the
         * database is created throughout the application lifecycle.
         *
         * @param context The application context.
         * @return The [AppDatabase] instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teachers_log_database"
                )
                // In development, destructive migration is used to simplify schema changes
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
