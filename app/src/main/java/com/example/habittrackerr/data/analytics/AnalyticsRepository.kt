package com.example.habittrackerr.data.analytics

import com.example.habittrackerr.HabitDao
import com.example.habittrackerr.data.dao.*
import com.example.habittrackerr.data.entities.*
import com.example.habittrackerr.Habit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for computing and managing task-level analytics
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val taskEventDao: TaskEventDao,
    private val taskAnalyticsDao: TaskAnalyticsDao,
    private val fitnessDataDao: FitnessDataDao,
    private val habitDao: HabitDao
) {

    /**
     * Compute comprehensive analytics for a specific task
     */
    suspend fun computeTaskAnalytics(
        taskId: Int,
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): TaskAnalyticsEntity {

        val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Get all events for this task in the date range
        val events = taskEventDao.getEventsForRange(userId, startTime, endTime).first()
        val taskEvents = events.filter { it.taskId == taskId }

        // Calculate completion metrics
        val completionEvents = taskEvents.filter { it.eventType == TaskEventType.COMPLETED }
        val totalCompletions = completionEvents.size

        // Calculate streaks
        val currentStreak = calculateCurrentStreak(taskId, System.currentTimeMillis())
        val longestStreak = calculateLongestStreak(taskEvents)

        // Calculate completion rate
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
        val completionRate = if (totalDays > 0) totalCompletions.toFloat() / totalDays else 0f

        // Calculate average completion time
        val avgCompletionTime = calculateAverageCompletionTime(completionEvents)

        // Calculate time-of-day distribution
        val timeDistribution = calculateTimeOfDayDistribution(completionEvents)

        val dateRange = "${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}"

        return TaskAnalyticsEntity(
            taskId = taskId,
            userId = userId,
            dateRange = dateRange,
            completionRate = completionRate,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCompletions = totalCompletions,
            averageCompletionTimeMinutes = avgCompletionTime,
            timeOfDayDistribution = timeDistribution
        )
    }

    /**
     * Get analytics for all tasks including deleted ones
     */
    suspend fun computeAllTasksAnalytics(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        includeDeleted: Boolean = true
    ): List<TaskAnalyticsEntity> {

        val habits = if (includeDeleted) {
            habitDao.getAllHabitsIncludingDeleted().first()
        } else {
            habitDao.getAllHabits().first()
        }

        return habits.map { habit ->
            computeTaskAnalytics(habit.id, userId, startDate, endDate)
        }
    }

    /**
     * Calculate current streak for a task
     */
    private suspend fun calculateCurrentStreak(taskId: Int, currentTime: Long): Int {
        return try {
            taskEventDao.getCurrentStreak(taskId, currentTime)
        } catch (_: Exception) {
            // Fallback calculation if SQL query fails
            val events = taskEventDao.getEventsForTask(taskId).first()
            val completionEvents = events.filter { it.eventType == TaskEventType.COMPLETED }
                .sortedByDescending { it.timestamp }

            calculateStreakFromEvents(completionEvents, currentTime)
        }
    }

    /**
     * Calculate longest streak from events
     */
    private fun calculateLongestStreak(events: List<TaskEventEntity>): Int {
        val completionEvents = events.filter { it.eventType == TaskEventType.COMPLETED }
            .sortedBy { it.timestamp }

        if (completionEvents.isEmpty()) return 0

        val completionDates = completionEvents.map { event ->
            timestampToLocalDate(event.timestamp)
        }.distinct().sorted()

        var maxStreak = 1
        var currentStreak = 1

        for (i in 1 until completionDates.size) {
            val prevDate = completionDates[i - 1]
            val currentDate = completionDates[i]

            if (java.time.temporal.ChronoUnit.DAYS.between(prevDate, currentDate) == 1L) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }

        return maxStreak
    }

    /**
     * Calculate current streak from events (fallback method)
     */
    private fun calculateStreakFromEvents(completionEvents: List<TaskEventEntity>, currentTime: Long): Int {
        if (completionEvents.isEmpty()) return 0

        val today = timestampToLocalDate(currentTime)

        val completionDates = completionEvents.map { event ->
            timestampToLocalDate(event.timestamp)
        }.distinct().sortedDescending()

        // Check if task was completed today or yesterday to start streak
        val streakStart = if (completionDates.contains(today)) {
            today
        } else if (completionDates.contains(today.minusDays(1))) {
            today.minusDays(1)
        } else {
            return 0
        }

        var streak = 0
        var checkDate = streakStart

        while (completionDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }

    /**
     * Calculate average completion time in minutes
     */
    private fun calculateAverageCompletionTime(completionEvents: List<TaskEventEntity>): Float {
        if (completionEvents.isEmpty()) return 0f

        // Group completions by date and calculate session duration
        val dailyCompletions = completionEvents.groupBy { event ->
            timestampToLocalDate(event.timestamp)
        }

        val sessionDurations = dailyCompletions.values.mapNotNull { dayEvents ->
            if (dayEvents.size < 2) return@mapNotNull null

            val sortedEvents = dayEvents.sortedBy { it.timestamp }
            val sessionStart = sortedEvents.first().timestamp
            val sessionEnd = sortedEvents.last().timestamp

            ((sessionEnd - sessionStart) / (1000 * 60)).toFloat() // Convert to minutes
        }

        return if (sessionDurations.isNotEmpty()) {
            sessionDurations.average().toFloat()
        } else {
            15f // Default assumption: 15 minutes per completion
        }
    }

    /**
     * Calculate time-of-day distribution (hour buckets)
     */
    private fun calculateTimeOfDayDistribution(completionEvents: List<TaskEventEntity>): Map<Int, Int> {
        val distribution = mutableMapOf<Int, Int>()

        completionEvents.forEach { event ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = event.timestamp
            }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            distribution[hour] = distribution.getOrDefault(hour, 0) + 1
        }

        return distribution
    }

    /**
     * Helper function to convert timestamp to LocalDate for API compatibility
     */
    private fun timestampToLocalDate(timestamp: Long): LocalDate {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        return LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Get task analytics with fitness correlation
     */
    suspend fun getTaskAnalyticsWithFitnessCorrelation(
        taskId: Int,
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): TaskAnalyticsWithFitness {

        val taskAnalytics = computeTaskAnalytics(taskId, userId, startDate, endDate)

        // Get fitness data for the same period
        val fitnessData = fitnessDataDao.getFitnessDataForRange(
            userId,
            startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        ).first()

        val averageSteps = fitnessData
            .filter { it.dataType == FitnessDataType.STEPS }
            .map { it.value }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        val averageCalories = fitnessData
            .filter { it.dataType == FitnessDataType.CALORIES_BURNED }
            .map { it.value }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        return TaskAnalyticsWithFitness(
            analytics = taskAnalytics,
            averageSteps = averageSteps,
            averageCalories = averageCalories,
            fitnessDataAvailable = fitnessData.isNotEmpty()
        )
    }

    /**
     * Save computed analytics to database
     */
    suspend fun saveAnalytics(analytics: TaskAnalyticsEntity) {
        taskAnalyticsDao.insertAnalytics(analytics)
    }

    /**
     * Get cached analytics or compute if not available
     */
    suspend fun getOrComputeAnalytics(
        taskId: Int,
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        forceRecompute: Boolean = false
    ): TaskAnalyticsEntity {

        val dateRange = "${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}_${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}"

        if (!forceRecompute) {
            val cached = taskAnalyticsDao.getAnalyticsForRange(taskId, dateRange)
            if (cached != null) {
                return cached
            }
        }

        val computed = computeTaskAnalytics(taskId, userId, startDate, endDate)
        saveAnalytics(computed)
        return computed
    }

    /**
     * Get analytics for task history view (including deleted tasks)
     */
    fun getTaskHistoryAnalytics(userId: String): Flow<List<TaskHistoryItem>> {
        return combine(
            habitDao.getAllHabitsIncludingDeleted(),
            taskAnalyticsDao.getAllAnalytics(userId)
        ) { habits, analytics ->

            habits.map { habit ->
                val latestAnalytics = analytics
                    .filter { it.taskId == habit.id }
                    .maxByOrNull { it.computedAt }

                TaskHistoryItem(
                    habit = habit,
                    analytics = latestAnalytics,
                    isActive = !habit.isDeleted,
                    lastActivity = latestAnalytics?.computedAt ?: habit.createdAt
                )
            }.sortedByDescending { it.lastActivity }
        }
    }
}

/**
 * Data class combining task analytics with fitness correlation
 */
data class TaskAnalyticsWithFitness(
    val analytics: TaskAnalyticsEntity,
    val averageSteps: Double,
    val averageCalories: Double,
    val fitnessDataAvailable: Boolean
)

/**
 * Data class for task history view
 */
data class TaskHistoryItem(
    val habit: Habit,
    val analytics: TaskAnalyticsEntity?,
    val isActive: Boolean,
    val lastActivity: Long
)
