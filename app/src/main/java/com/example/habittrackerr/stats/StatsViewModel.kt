package com.example.habittrackerr.stats

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittrackerr.data.analytics.AnalyticsRepository
import com.example.habittrackerr.data.fitness.HealthConnectRepository
import com.example.habittrackerr.data.fitness.GoogleFitFallbackRepository
import com.example.habittrackerr.data.dao.FitnessDataDao
import com.example.habittrackerr.data.entities.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val healthConnectRepository: HealthConnectRepository,
    private val googleFitRepository: GoogleFitFallbackRepository,
    private val fitnessDataDao: FitnessDataDao
) : ViewModel() {

    private val _summaryState = MutableStateFlow(FitnessSummaryState())
    val summaryState: StateFlow<FitnessSummaryState> = _summaryState.asStateFlow()

    private val _timelineState = MutableStateFlow(TimelineState())
    val timelineState: StateFlow<TimelineState> = _timelineState.asStateFlow()

    private val _taskAnalyticsState = MutableStateFlow(TaskAnalyticsState())
    val taskAnalyticsState: StateFlow<TaskAnalyticsState> = _taskAnalyticsState.asStateFlow()

    private val _historyState = MutableStateFlow(TaskHistoryState())
    val historyState: StateFlow<TaskHistoryState> = _historyState.asStateFlow()

    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEK)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    private val _selectedTaskId = MutableStateFlow<Int?>(null)
    val selectedTaskId: StateFlow<Int?> = _selectedTaskId.asStateFlow()

    init {
        checkPermissions()
        loadInitialData()
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            val healthConnectAvailable = healthConnectRepository.isAvailable()
            val healthConnectPermissions = healthConnectRepository.hasAllPermissions()
            val googleFitAvailable = googleFitRepository.isAvailable()
            val googleFitPermissions = googleFitRepository.hasPermissions()

            _permissionsState.value = PermissionsState(
                healthConnectAvailable = healthConnectAvailable,
                healthConnectPermissions = healthConnectPermissions,
                googleFitAvailable = googleFitAvailable,
                googleFitPermissions = googleFitPermissions,
                hasAnyPermissions = healthConnectPermissions || googleFitPermissions
            )
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            loadFitnessSummary()
            loadTimelineData()
            loadTaskAnalytics()
            loadTaskHistory()
        }
    }

    fun selectTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        viewModelScope.launch {
            loadTimelineData()
            loadTaskAnalytics()
        }
    }

    fun selectTask(taskId: Int?) {
        _selectedTaskId.value = taskId
        if (taskId != null) {
            loadTaskDetailAnalytics(taskId)
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            Log.d("StatsViewModel", "Starting manual refresh...")

            _summaryState.value = _summaryState.value.copy(isLoading = true, error = null)
            _timelineState.value = _timelineState.value.copy(isLoading = true, error = null)

            // Check permissions first
            checkPermissions()
            val currentPermissions = _permissionsState.value

            Log.d("StatsViewModel", "Permission status: HealthConnect=${currentPermissions.healthConnectPermissions}, GoogleFit=${currentPermissions.googleFitPermissions}")

            try {
                // Attempt to sync fitness data
                val syncResult = syncFitnessData()
                Log.d("StatsViewModel", "Sync result: $syncResult")

                // Reload all data
                loadInitialData()

                Log.d("StatsViewModel", "Refresh completed successfully")
            } catch (e: Exception) {
                Log.e("StatsViewModel", "Error during refresh", e)
                _summaryState.value = _summaryState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh data: ${e.message}"
                )
                _timelineState.value = _timelineState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh data: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadFitnessSummary() {
        try {
            val today = LocalDate.now()
            val userId = getCurrentUserId()

            val todayData = fitnessDataDao.getFitnessDataForDate(
                userId,
                today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ).first()

            val steps = todayData.filter { it.dataType == FitnessDataType.STEPS }
                .sumOf { it.value }.toInt()

            val distance = todayData.filter { it.dataType == FitnessDataType.DISTANCE }
                .sumOf { it.value } / 1000.0 // Convert to km

            val calories = todayData.filter { it.dataType == FitnessDataType.CALORIES_BURNED }
                .sumOf { it.value }.toInt()

            val activeMinutes = todayData.filter { it.dataType == FitnessDataType.ACTIVE_MINUTES }
                .sumOf { it.value }.toInt()

            val sleepHours = todayData.filter { it.dataType == FitnessDataType.SLEEP_DURATION }
                .sumOf { it.value }

            _summaryState.value = FitnessSummaryState(
                steps = steps,
                distance = distance,
                calories = calories,
                activeMinutes = activeMinutes,
                sleepHours = sleepHours,
                lastUpdated = System.currentTimeMillis(),
                isLoading = false
            )
        } catch (e: Exception) {
            _summaryState.value = _summaryState.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }

    private suspend fun loadTimelineData() {
        try {
            val range = _selectedTimeRange.value
            val (startDate, endDate) = getDateRangeForTimeRange(range)
            val userId = getCurrentUserId()

            val fitnessData = fitnessDataDao.getFitnessDataForRange(
                userId,
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ).first()

            val chartData = generateChartData(fitnessData, startDate, endDate, range)

            _timelineState.value = TimelineState(
                chartData = chartData,
                selectedRange = range,
                isLoading = false
            )
        } catch (e: Exception) {
            _timelineState.value = _timelineState.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }

    private suspend fun loadTaskAnalytics() {
        try {
            val userId = getCurrentUserId()
            val range = _selectedTimeRange.value
            val (startDate, endDate) = getDateRangeForTimeRange(range)

            val allAnalytics = analyticsRepository.computeAllTasksAnalytics(
                userId, startDate, endDate, includeDeleted = false
            )

            _taskAnalyticsState.value = TaskAnalyticsState(
                analytics = allAnalytics,
                isLoading = false
            )
        } catch (e: Exception) {
            _taskAnalyticsState.value = _taskAnalyticsState.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }

    private fun loadTaskDetailAnalytics(taskId: Int) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val range = _selectedTimeRange.value
                val (startDate, endDate) = getDateRangeForTimeRange(range)

                val analytics = analyticsRepository.getTaskAnalyticsWithFitnessCorrelation(
                    taskId, userId, startDate, endDate
                )

                _taskAnalyticsState.value = _taskAnalyticsState.value.copy(
                    selectedTaskAnalytics = analytics
                )
            } catch (e: Exception) {
                _taskAnalyticsState.value = _taskAnalyticsState.value.copy(
                    error = e.message
                )
            }
        }
    }

    private suspend fun loadTaskHistory() {
        try {
            val userId = getCurrentUserId()

            analyticsRepository.getTaskHistoryAnalytics(userId).collect { historyItems ->
                _historyState.value = TaskHistoryState(
                    items = historyItems,
                    filter = _historyState.value.filter,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _historyState.value = _historyState.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyState.value = _historyState.value.copy(filter = filter)
    }

    private suspend fun syncFitnessData(): String {
        return try {
            val today = LocalDate.now()
            val weekAgo = today.minusDays(7)

            val permissionsState = _permissionsState.value

            when {
                permissionsState.healthConnectPermissions -> {
                    Log.d("StatsViewModel", "Syncing from Health Connect...")
                    val fitnessData = healthConnectRepository.getFitnessDataForRange(weekAgo, today)
                    if (fitnessData.isNotEmpty()) {
                        fitnessDataDao.insertFitnessData(fitnessData)
                        Log.d("StatsViewModel", "Inserted ${fitnessData.size} Health Connect data points")
                        "Synced ${fitnessData.size} data points from Health Connect"
                    } else {
                        Log.w("StatsViewModel", "No Health Connect data found")
                        "No data found in Health Connect"
                    }
                }

                permissionsState.googleFitPermissions -> {
                    Log.d("StatsViewModel", "Syncing from Google Fit...")
                    val fitnessData = googleFitRepository.getFitnessDataForRange(weekAgo, today)
                    if (fitnessData.isNotEmpty()) {
                        fitnessDataDao.insertFitnessData(fitnessData)
                        Log.d("StatsViewModel", "Inserted ${fitnessData.size} Google Fit data points")
                        "Synced ${fitnessData.size} data points from Google Fit"
                    } else {
                        Log.w("StatsViewModel", "No Google Fit data found")
                        "No data found in Google Fit"
                    }
                }

                else -> {
                    Log.w("StatsViewModel", "No fitness permissions available")
                    "No fitness data sources connected"
                }
            }
        } catch (e: Exception) {
            Log.e("StatsViewModel", "Error during fitness data sync", e)
            throw e
        }
    }

    private fun generateChartData(
        fitnessData: List<FitnessDataEntity>,
        startDate: LocalDate,
        endDate: LocalDate,
        range: TimeRange
    ): List<ChartDataPoint> {
        return when (range) {
            TimeRange.DAY -> generateHourlyData(fitnessData, startDate)
            TimeRange.WEEK -> generateDailyData(fitnessData, startDate, endDate)
            TimeRange.MONTH -> generateDailyData(fitnessData, startDate, endDate)
        }
    }

    private fun generateHourlyData(data: List<FitnessDataEntity>, date: LocalDate): List<ChartDataPoint> {
        // Group by hour and sum steps
        return (0..23).map { hour ->
            val hourData = data.filter { entity ->
                val entityDate = LocalDate.parse(entity.date)
                entityDate == date && entity.dataType == FitnessDataType.STEPS
            }

            ChartDataPoint(
                label = "${hour}:00",
                value = hourData.sumOf { it.value }.toFloat(),
                timestamp = date.atTime(hour, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        }
    }

    private fun generateDailyData(
        data: List<FitnessDataEntity>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ChartDataPoint> {
        var current = startDate
        val result = mutableListOf<ChartDataPoint>()

        while (!current.isAfter(endDate)) {
            val dayData = data.filter { entity ->
                val entityDate = LocalDate.parse(entity.date)
                entityDate == current && entity.dataType == FitnessDataType.STEPS
            }

            result.add(
                ChartDataPoint(
                    label = current.format(DateTimeFormatter.ofPattern("MM/dd")),
                    value = dayData.sumOf { it.value }.toFloat(),
                    timestamp = current.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
            )

            current = current.plusDays(1)
        }

        return result
    }

    private fun getDateRangeForTimeRange(range: TimeRange): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (range) {
            TimeRange.DAY -> today to today
            TimeRange.WEEK -> today.minusDays(6) to today
            TimeRange.MONTH -> today.minusDays(29) to today
        }
    }

    private fun getCurrentUserId(): String {
        // TODO: Implement based on your auth system
        return "current_user_id"
    }
}

// State classes
data class FitnessSummaryState(
    val steps: Int = 0,
    val distance: Double = 0.0,
    val calories: Int = 0,
    val activeMinutes: Int = 0,
    val sleepHours: Double = 0.0,
    val lastUpdated: Long = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class TimelineState(
    val chartData: List<ChartDataPoint> = emptyList(),
    val selectedRange: TimeRange = TimeRange.WEEK,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class TaskAnalyticsState(
    val analytics: List<TaskAnalyticsEntity> = emptyList(),
    val selectedTaskAnalytics: com.example.habittrackerr.data.analytics.TaskAnalyticsWithFitness? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class TaskHistoryState(
    val items: List<com.example.habittrackerr.data.analytics.TaskHistoryItem> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class PermissionsState(
    val healthConnectAvailable: Boolean = false,
    val healthConnectPermissions: Boolean = false,
    val googleFitAvailable: Boolean = false,
    val googleFitPermissions: Boolean = false,
    val hasAnyPermissions: Boolean = false
)

data class ChartDataPoint(
    val label: String,
    val value: Float,
    val timestamp: Long
)

enum class TimeRange {
    DAY, WEEK, MONTH
}

enum class HistoryFilter {
    ALL, ACTIVE, DELETED
}
