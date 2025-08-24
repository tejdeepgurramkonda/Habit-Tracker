package com.example.habittrackerr.data.analytics

import com.example.habittrackerr.data.dao.*
import com.example.habittrackerr.data.entities.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsRepositoryTest {

    @Mock
    private lateinit var taskEventDao: TaskEventDao

    @Mock
    private lateinit var taskAnalyticsDao: TaskAnalyticsDao

    @Mock
    private lateinit var fitnessDataDao: FitnessDataDao

    @Mock
    private lateinit var habitDao: HabitDao

    private lateinit var analyticsRepository: AnalyticsRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        analyticsRepository = AnalyticsRepository(
            taskEventDao = taskEventDao,
            taskAnalyticsDao = taskAnalyticsDao,
            fitnessDataDao = fitnessDataDao,
            habitDao = habitDao
        )
    }

    @Test
    fun `computeTaskAnalytics calculates correct completion rate`() = runTest {
        // Arrange
        val taskId = 1
        val userId = "user123"
        val startDate = LocalDate.of(2024, 8, 1)
        val endDate = LocalDate.of(2024, 8, 7) // 7 days

        val completionEvents = listOf(
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722470400000L), // Aug 1
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722556800000L), // Aug 2
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722729600000L)  // Aug 4
        )

        whenever(taskEventDao.getEventsForRange(any(), any(), any()))
            .thenReturn(flowOf(completionEvents))
        whenever(taskEventDao.getCurrentStreak(any(), any()))
            .thenReturn(2)

        // Act
        val result = analyticsRepository.computeTaskAnalytics(taskId, userId, startDate, endDate)

        // Assert
        assertEquals(3, result.totalCompletions)
        assertEquals(3f / 7f, result.completionRate, 0.01f) // 3 completions in 7 days
        assertEquals(2, result.currentStreak)
    }

    @Test
    fun `computeTaskAnalytics handles deleted tasks correctly`() = runTest {
        // Arrange
        val taskId = 1
        val userId = "user123"
        val startDate = LocalDate.of(2024, 8, 1)
        val endDate = LocalDate.of(2024, 8, 7)

        val events = listOf(
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722470400000L),
            createTaskEvent(taskId, TaskEventType.DELETED, 1722643200000L), // Aug 3
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722729600000L)  // Aug 4 (after deletion)
        )

        whenever(taskEventDao.getEventsForRange(any(), any(), any()))
            .thenReturn(flowOf(events))
        whenever(taskEventDao.getCurrentStreak(any(), any()))
            .thenReturn(0)

        // Act
        val result = analyticsRepository.computeTaskAnalytics(taskId, userId, startDate, endDate)

        // Assert
        assertEquals(2, result.totalCompletions) // Should count completions including after deletion
        assertTrue(result.completionRate > 0) // Should still have completion rate
    }

    @Test
    fun `calculateLongestStreak works with irregular completion pattern`() = runTest {
        // Arrange
        val taskId = 1
        val userId = "user123"
        val startDate = LocalDate.of(2024, 8, 1)
        val endDate = LocalDate.of(2024, 8, 15)

        // Pattern: 3 days streak, 2 days gap, 5 days streak
        val events = listOf(
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722470400000L), // Aug 1
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722556800000L), // Aug 2
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722643200000L), // Aug 3
            // Gap: Aug 4-5
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722816000000L), // Aug 6
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722902400000L), // Aug 7
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722988800000L), // Aug 8
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1723075200000L), // Aug 9
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1723161600000L)  // Aug 10
        )

        whenever(taskEventDao.getEventsForRange(any(), any(), any()))
            .thenReturn(flowOf(events))
        whenever(taskEventDao.getCurrentStreak(any(), any()))
            .thenReturn(0)

        // Act
        val result = analyticsRepository.computeTaskAnalytics(taskId, userId, startDate, endDate)

        // Assert
        assertEquals(5, result.longestStreak) // Longest consecutive streak should be 5 days
    }

    @Test
    fun `getTaskAnalyticsWithFitnessCorrelation includes fitness data`() = runTest {
        // Arrange
        val taskId = 1
        val userId = "user123"
        val startDate = LocalDate.of(2024, 8, 1)
        val endDate = LocalDate.of(2024, 8, 7)

        val events = listOf(
            createTaskEvent(taskId, TaskEventType.COMPLETED, 1722470400000L)
        )

        val fitnessData = listOf(
            FitnessDataEntity(
                userId = userId,
                date = "2024-08-01",
                dataType = FitnessDataType.STEPS,
                value = 10000.0,
                unit = "steps",
                source = FitnessDataSource.HEALTH_CONNECT
            ),
            FitnessDataEntity(
                userId = userId,
                date = "2024-08-01",
                dataType = FitnessDataType.CALORIES_BURNED,
                value = 500.0,
                unit = "calories",
                source = FitnessDataSource.HEALTH_CONNECT
            )
        )

        whenever(taskEventDao.getEventsForRange(any(), any(), any()))
            .thenReturn(flowOf(events))
        whenever(taskEventDao.getCurrentStreak(any(), any()))
            .thenReturn(1)
        whenever(fitnessDataDao.getFitnessDataForRange(any(), any(), any()))
            .thenReturn(flowOf(fitnessData))

        // Act
        val result = analyticsRepository.getTaskAnalyticsWithFitnessCorrelation(
            taskId, userId, startDate, endDate
        )

        // Assert
        assertEquals(10000.0, result.averageSteps)
        assertEquals(500.0, result.averageCalories)
        assertTrue(result.fitnessDataAvailable)
    }

    private fun createTaskEvent(
        taskId: Int,
        eventType: TaskEventType,
        timestamp: Long
    ): TaskEventEntity {
        return TaskEventEntity(
            id = 0,
            taskId = taskId,
            userId = "user123",
            eventType = eventType,
            timestamp = timestamp,
            metadata = emptyMap(),
            syncedToFirestore = false
        )
    }
}
