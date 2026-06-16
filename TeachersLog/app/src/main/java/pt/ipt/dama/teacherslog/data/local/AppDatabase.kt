package pt.ipt.dama.teacherslog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.ipt.dama.teacherslog.data.model.*

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
        Grade::class
    ], 
    version = 10,
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
         * @param scope A [CoroutineScope] for background operations (like database seeding).
         * @return The [AppDatabase] instance.
         */
        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teachers_log_database"
                )
                // In development, destructive migration is used to simplify schema changes
                .fallbackToDestructiveMigration(true)
                // Add a callback to populate the database with initial data
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Internal callback used to trigger database seeding when the database is created or opened.
         */
        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            /**
             * Triggered when the database is first created.
             */
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedDatabase()
            }

            /**
             * Triggered every time the database is opened.
             */
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                seedDatabase()
            }

            /**
             * Launches a coroutine to populate the database with mock data via [DatabaseSeeder].
             */
            private fun seedDatabase() {
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        DatabaseSeeder.seed(database.teacherDao())
                    }
                }
            }
        }
    }
}
