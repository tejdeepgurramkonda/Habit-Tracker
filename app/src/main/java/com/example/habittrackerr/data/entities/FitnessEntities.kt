package com.example.habittrackerr.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity for storing fitness data from Health Connect or Google Fit
 */
@Entity(
    tableName = "fitness_data",
    indices = [
        Index(value = ["date", "dataType"]),
        Index(value = ["userId", "date"])
    ]
)
data class FitnessDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: String, // YYYY-MM-DD format
    val dataType: FitnessDataType,
    val value: Double,
    val unit: String,
    val source: FitnessDataSource,
    val timestamp: Long = System.currentTimeMillis(),
    val syncedToFirestore: Boolean = false
)

/**
 * Entity for storing task/habit events for analytics
 */
@Entity(
    tableName = "task_events",
    indices = [
        Index(value = ["taskId", "timestamp"]),
        Index(value = ["userId", "eventType"]),
        Index(value = ["timestamp"])
    ]
)
data class TaskEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Int,
    val userId: String,
    val eventType: TaskEventType,
    val timestamp: Long,
    val metadata: Map<String, String> = emptyMap(), // Additional event data
    val syncedToFirestore: Boolean = false
)

/**
 * Entity for storing computed analytics data
 */
@Entity(
    tableName = "task_analytics",
    indices = [
        Index(value = ["taskId", "dateRange"]),
        Index(value = ["userId", "computedAt"])
    ]
)
data class TaskAnalyticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Int,
    val userId: String,
    val dateRange: String, // e.g., "2024-08-01_2024-08-31"
    val completionRate: Float,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val averageCompletionTimeMinutes: Float,
    val timeOfDayDistribution: Map<Int, Int>, // Hour -> Count
    val computedAt: Long = System.currentTimeMillis(),
    val syncedToFirestore: Boolean = false
)

/**
 * Types of fitness data we track
 */
enum class FitnessDataType {
    STEPS,
    DISTANCE,
    CALORIES_BURNED,
    ACTIVE_MINUTES,
    SLEEP_DURATION,
    HEART_RATE,
    EXERCISE_SESSION
}

/**
 * Source of fitness data
 */
enum class FitnessDataSource {
    HEALTH_CONNECT,
    GOOGLE_FIT,
    MANUAL_ENTRY
}

/**
 * Types of task events
 */
enum class TaskEventType {
    CREATED,
    COMPLETED,
    UPDATED,
    DELETED,
    RESTORED,
    VIEWED
}
