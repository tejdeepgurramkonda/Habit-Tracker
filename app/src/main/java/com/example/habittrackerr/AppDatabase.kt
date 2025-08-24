package com.example.habittrackerr

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.habittrackerr.data.entities.*
import com.example.habittrackerr.data.dao.*

@Database(
    entities = [
        Habit::class,
        UserPreferences::class,
        UserProfile::class,
        FitnessDataEntity::class,
        TaskEventEntity::class,
        TaskAnalyticsEntity::class
    ],
    version = 6,
    exportSchema = false  // Changed to false to avoid schema export warnings
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun fitnessDataDao(): FitnessDataDao
    abstract fun taskEventDao(): TaskEventDao
    abstract fun taskAnalyticsDao(): TaskAnalyticsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 5 to 6 - Add soft delete and new tables
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add soft delete columns to habits table
                database.execSQL("ALTER TABLE habits ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE habits ADD COLUMN deletedAt INTEGER")

                // Create fitness_data table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS fitness_data (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        date TEXT NOT NULL,
                        dataType TEXT NOT NULL,
                        value REAL NOT NULL,
                        unit TEXT NOT NULL,
                        source TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        syncedToFirestore INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Create task_events table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS task_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        userId TEXT NOT NULL,
                        eventType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        metadata TEXT NOT NULL DEFAULT '{}',
                        syncedToFirestore INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Create task_analytics table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS task_analytics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        userId TEXT NOT NULL,
                        dateRange TEXT NOT NULL,
                        completionRate REAL NOT NULL,
                        currentStreak INTEGER NOT NULL,
                        longestStreak INTEGER NOT NULL,
                        totalCompletions INTEGER NOT NULL,
                        averageCompletionTimeMinutes REAL NOT NULL,
                        timeOfDayDistribution TEXT NOT NULL DEFAULT '{}',
                        computedAt INTEGER NOT NULL,
                        syncedToFirestore INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Create indexes for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_fitness_data_date_dataType ON fitness_data(date, dataType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_fitness_data_userId_date ON fitness_data(userId, date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_events_taskId_timestamp ON task_events(taskId, timestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_events_userId_eventType ON task_events(userId, eventType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_events_timestamp ON task_events(timestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_analytics_taskId_dateRange ON task_analytics(taskId, dateRange)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_analytics_userId_computedAt ON task_analytics(userId, computedAt)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_database"
                )
                .addMigrations(MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
