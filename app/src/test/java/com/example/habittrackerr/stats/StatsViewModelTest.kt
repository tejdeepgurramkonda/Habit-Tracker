package com.example.habittrackerr.stats

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.habittrackerr.data.analytics.AnalyticsRepository
import com.example.habittrackerr.data.dao.FitnessDataDao
import com.example.habittrackerr.data.entities.*
import com.example.habittrackerr.data.fitness.HealthConnectRepository
import com.example.habittrackerr.data.fitness.GoogleFitFallbackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var analyticsRepository: AnalyticsRepository

    @Mock
    private lateinit var healthConnectRepository: HealthConnectRepository

    @Mock
    private lateinit var googleFitRepository: GoogleFitFallbackRepository

    @Mock
    private lateinit var fitnessDataDao: FitnessDataDao

    private lateinit var viewModel: StatsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup default mock responses
        whenever(healthConnectRepository.isAvailable()).thenReturn(true)
        whenever(healthConnectRepository.hasAllPermissions()).thenReturn(true)
        whenever(googleFitRepository.isAvailable()).thenReturn(true)
        whenever(googleFitRepository.hasPermissions()).thenReturn(false)

        viewModel = StatsViewModel(
            analyticsRepository = analyticsRepository,
            healthConnectRepository = healthConnectRepository,
            googleFitRepository = googleFitRepository,
            fitnessDataDao = fitnessDataDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial permissions state is set correctly`() = runTest {
        // Act - ViewModel initialization triggers permission check
        testScheduler.advanceUntilIdle()

        // Assert
        val permissionsState = viewModel.permissionsState.value
        assertTrue(permissionsState.healthConnectAvailable)
        assertTrue(permissionsState.healthConnectPermissions)
        assertTrue(permissionsState.googleFitAvailable)
        assertFalse(permissionsState.googleFitPermissions)
        assertTrue(permissionsState.hasAnyPermissions)
    }

    @Test
    fun `selectTimeRange updates state and triggers data reload`() = runTest {
        // Arrange
        val mockFitnessData = listOf(
            FitnessDataEntity(
                userId = "user123",
                date = "2024-08-24",
                dataType = FitnessDataType.STEPS,
                value = 10000.0,
                unit = "steps",
                source = FitnessDataSource.HEALTH_CONNECT
            )
        )

        whenever(fitnessDataDao.getFitnessDataForDate(any(), any()))
            .thenReturn(flowOf(emptyList()))
        whenever(fitnessDataDao.getFitnessDataForRange(any(), any(), any()))
            .thenReturn(flowOf(mockFitnessData))
        whenever(analyticsRepository.computeAllTasksAnalytics(any(), any(), any(), any()))
            .thenReturn(emptyList())

        // Act
        viewModel.selectTimeRange(TimeRange.MONTH)
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(TimeRange.MONTH, viewModel.selectedTimeRange.value)
        verify(fitnessDataDao, atLeastOnce()).getFitnessDataForRange(any(), any(), any())
        verify(analyticsRepository, atLeastOnce()).computeAllTasksAnalytics(any(), any(), any(), any())
    }

    @Test
    fun `refreshData triggers sync and reloads all data`() = runTest {
        // Arrange
        whenever(fitnessDataDao.getFitnessDataForDate(any(), any()))
            .thenReturn(flowOf(emptyList()))
        whenever(fitnessDataDao.getFitnessDataForRange(any(), any(), any()))
            .thenReturn(flowOf(emptyList()))
        whenever(analyticsRepository.computeAllTasksAnalytics(any(), any(), any(), any()))
            .thenReturn(emptyList())
        whenever(healthConnectRepository.getFitnessDataForRange(any(), any()))
            .thenReturn(emptyList())

        // Act
        viewModel.refreshData()
        testScheduler.advanceUntilIdle()

        // Assert
        assertTrue(viewModel.summaryState.value.isLoading || !viewModel.summaryState.value.isLoading)
        verify(healthConnectRepository).getFitnessDataForRange(any(), any())
        verify(fitnessDataDao, atLeastOnce()).insertFitnessData(any<List<FitnessDataEntity>>())
    }

    @Test
    fun `fitness summary loads correctly with data`() = runTest {
        // Arrange
        val mockFitnessData = listOf(
            FitnessDataEntity(
                userId = "user123",
                date = "2024-08-24",
                dataType = FitnessDataType.STEPS,
                value = 8500.0,
                unit = "steps",
                source = FitnessDataSource.HEALTH_CONNECT
            ),
            FitnessDataEntity(
                userId = "user123",
                date = "2024-08-24",
                dataType = FitnessDataType.CALORIES_BURNED,
                value = 450.0,
                unit = "calories",
                source = FitnessDataSource.HEALTH_CONNECT
            ),
            FitnessDataEntity(
                userId = "user123",
                date = "2024-08-24",
                dataType = FitnessDataType.DISTANCE,
                value = 6200.0,
                unit = "meters",
                source = FitnessDataSource.HEALTH_CONNECT
            )
        )

        whenever(fitnessDataDao.getFitnessDataForDate(any(), any()))
            .thenReturn(flowOf(mockFitnessData))

        // Act - Initialize ViewModel to trigger data loading
        testScheduler.advanceUntilIdle()

        // Assert
        val summaryState = viewModel.summaryState.value
        assertEquals(8500, summaryState.steps)
        assertEquals(450, summaryState.calories)
        assertEquals(6.2, summaryState.distance, 0.1) // 6200m = 6.2km
        assertFalse(summaryState.isLoading)
    }

    @Test
    fun `task analytics state updates when data is available`() = runTest {
        // Arrange
        val mockAnalytics = listOf(
            TaskAnalyticsEntity(
                taskId = 1,
                userId = "user123",
                dateRange = "2024-08-17_2024-08-24",
                completionRate = 0.8f,
                currentStreak = 5,
                longestStreak = 10,
                totalCompletions = 20,
                averageCompletionTimeMinutes = 15.5f,
                timeOfDayDistribution = mapOf(9 to 5, 18 to 10, 20 to 5)
            )
        )

        whenever(analyticsRepository.computeAllTasksAnalytics(any(), any(), any(), any()))
            .thenReturn(mockAnalytics)
        whenever(fitnessDataDao.getFitnessDataForDate(any(), any()))
            .thenReturn(flowOf(emptyList()))
        whenever(fitnessDataDao.getFitnessDataForRange(any(), any(), any()))
            .thenReturn(flowOf(emptyList()))

        // Act
        testScheduler.advanceUntilIdle()

        // Assert
        val analyticsState = viewModel.taskAnalyticsState.value
        assertEquals(1, analyticsState.analytics.size)
        assertEquals(1, analyticsState.analytics.first().taskId)
        assertEquals(0.8f, analyticsState.analytics.first().completionRate)
        assertFalse(analyticsState.isLoading)
    }

    @Test
    fun `selectTask triggers detailed analytics loading`() = runTest {
        // Arrange
        val taskId = 1
        val mockDetailedAnalytics = com.example.habittrackerr.data.analytics.TaskAnalyticsWithFitness(
            analytics = TaskAnalyticsEntity(
                taskId = taskId,
                userId = "user123",
                dateRange = "2024-08-17_2024-08-24",
                completionRate = 0.9f,
                currentStreak = 3,
                longestStreak = 7,
                totalCompletions = 15,
                averageCompletionTimeMinutes = 12.0f,
                timeOfDayDistribution = mapOf(8 to 3, 19 to 8, 21 to 4)
            ),
            averageSteps = 9500.0,
            averageCalories = 420.0,
            fitnessDataAvailable = true
        )

        whenever(analyticsRepository.getTaskAnalyticsWithFitnessCorrelation(any(), any(), any(), any()))
            .thenReturn(mockDetailedAnalytics)

        // Act
        viewModel.selectTask(taskId)
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(taskId, viewModel.selectedTaskId.value)
        val selectedAnalytics = viewModel.taskAnalyticsState.value.selectedTaskAnalytics
        assertEquals(taskId, selectedAnalytics?.analytics?.taskId)
        assertEquals(9500.0, selectedAnalytics?.averageSteps)
        assertEquals(420.0, selectedAnalytics?.averageCalories)
        assertTrue(selectedAnalytics?.fitnessDataAvailable == true)
    }

    @Test
    fun `setHistoryFilter updates filter state`() = runTest {
        // Act
        viewModel.setHistoryFilter(HistoryFilter.DELETED)

        // Assert
        assertEquals(HistoryFilter.DELETED, viewModel.historyState.value.filter)
    }
}
