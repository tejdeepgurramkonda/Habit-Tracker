package com.example.habittrackerr.data.dao

import androidx.room.*
import com.example.habittrackerr.data.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for fitness data operations
 */
@Dao
interface FitnessDataDao {

    @Query("SELECT * FROM fitness_data WHERE userId = :userId AND date = :date ORDER BY timestamp DESC")
    fun getFitnessDataForDate(userId: String, date: String): Flow<List<FitnessDataEntity>>

    @Query("SELECT * FROM fitness_data WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date, timestamp")
    fun getFitnessDataForRange(userId: String, startDate: String, endDate: String): Flow<List<FitnessDataEntity>>

    @Query("SELECT * FROM fitness_data WHERE userId = :userId AND dataType = :type AND date BETWEEN :startDate AND :endDate ORDER BY date")
    fun getFitnessDataByType(userId: String, type: FitnessDataType, startDate: String, endDate: String): Flow<List<FitnessDataEntity>>

    @Query("SELECT SUM(value) FROM fitness_data WHERE userId = :userId AND dataType = :type AND date = :date")
    suspend fun getDailyTotal(userId: String, type: FitnessDataType, date: String): Double?

    @Query("SELECT AVG(value) FROM fitness_data WHERE userId = :userId AND dataType = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageForRange(userId: String, type: FitnessDataType, startDate: String, endDate: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFitnessData(data: List<FitnessDataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFitnessData(data: FitnessDataEntity)

    @Query("UPDATE fitness_data SET syncedToFirestore = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("SELECT * FROM fitness_data WHERE syncedToFirestore = 0 LIMIT 100")
    suspend fun getUnsyncedData(): List<FitnessDataEntity>

    @Query("DELETE FROM fitness_data WHERE date < :cutoffDate")
    suspend fun deleteOldData(cutoffDate: String)
}

/**
 * DAO for task events operations
 */
@Dao
interface TaskEventDao {

    @Query("SELECT * FROM task_events WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getEventsForTask(taskId: Int): Flow<List<TaskEventEntity>>

    @Query("SELECT * FROM task_events WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getEventsForRange(userId: String, startTime: Long, endTime: Long): Flow<List<TaskEventEntity>>

    @Query("SELECT * FROM task_events WHERE taskId = :taskId AND eventType = :type ORDER BY timestamp DESC")
    fun getEventsByType(taskId: Int, type: TaskEventType): Flow<List<TaskEventEntity>>

    @Query("SELECT COUNT(*) FROM task_events WHERE taskId = :taskId AND eventType = 'COMPLETED' AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getCompletionCount(taskId: Int, startTime: Long, endTime: Long): Int

    @Query("""
        SELECT COUNT(*) as streak FROM (
            SELECT DATE(timestamp/1000, 'unixepoch') as date 
            FROM task_events 
            WHERE taskId = :taskId AND eventType = 'COMPLETED' 
            AND timestamp <= :currentTime
            GROUP BY date 
            ORDER BY date DESC
        ) WHERE date >= DATE(:currentTime/1000, 'unixepoch', '-' || (SELECT COUNT(*) FROM (
            SELECT DATE(timestamp/1000, 'unixepoch') as date 
            FROM task_events 
            WHERE taskId = :taskId AND eventType = 'COMPLETED' 
            AND timestamp <= :currentTime
            GROUP BY date 
            ORDER BY date DESC
        )) || ' days')
    """)
    suspend fun getCurrentStreak(taskId: Int, currentTime: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TaskEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<TaskEventEntity>)

    @Query("UPDATE task_events SET syncedToFirestore = 1 WHERE id = :id")
    suspend fun markEventAsSynced(id: Long)

    @Query("SELECT * FROM task_events WHERE syncedToFirestore = 0 LIMIT 100")
    suspend fun getUnsyncedEvents(): List<TaskEventEntity>
}

/**
 * DAO for task analytics operations
 */
@Dao
interface TaskAnalyticsDao {

    @Query("SELECT * FROM task_analytics WHERE taskId = :taskId ORDER BY computedAt DESC LIMIT 1")
    suspend fun getLatestAnalytics(taskId: Int): TaskAnalyticsEntity?

    @Query("SELECT * FROM task_analytics WHERE taskId = :taskId AND dateRange = :dateRange")
    suspend fun getAnalyticsForRange(taskId: Int, dateRange: String): TaskAnalyticsEntity?

    @Query("SELECT * FROM task_analytics WHERE userId = :userId ORDER BY computedAt DESC")
    fun getAllAnalytics(userId: String): Flow<List<TaskAnalyticsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: TaskAnalyticsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: List<TaskAnalyticsEntity>)

    @Query("UPDATE task_analytics SET syncedToFirestore = 1 WHERE id = :id")
    suspend fun markAnalyticsAsSynced(id: Long)

    @Query("SELECT * FROM task_analytics WHERE syncedToFirestore = 0 LIMIT 100")
    suspend fun getUnsyncedAnalytics(): List<TaskAnalyticsEntity>

    @Query("DELETE FROM task_analytics WHERE computedAt < :cutoffTime")
    suspend fun deleteOldAnalytics(cutoffTime: Long)
}
