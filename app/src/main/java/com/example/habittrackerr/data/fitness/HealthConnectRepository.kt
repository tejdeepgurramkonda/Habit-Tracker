package com.example.habittrackerr.data.fitness

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import com.example.habittrackerr.data.entities.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Health Connect integration (primary fitness data source)
 */
@Singleton
class HealthConnectRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    // Required permissions for Health Connect
    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    /**
     * Check if Health Connect is available on this device
     */
    suspend fun isAvailable(): Boolean {
        return try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> true
                HealthConnectClient.SDK_UNAVAILABLE -> false
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> false
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if all required permissions are granted
     */
    suspend fun hasAllPermissions(): Boolean {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            permissions.all { it in grantedPermissions }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get the permissions that need to be requested
     */
    suspend fun getPermissionsToRequest(): Set<String> {
        return try {
            healthConnectClient.permissionController.getGrantedPermissions()
                .let { granted ->
                    permissions.filterNot { it in granted }.toSet()
                }
        } catch (e: Exception) {
            permissions
        }.map { it.toString() }.toSet()
    }

    /**
     * Read aggregated steps data for a date range
     */
    suspend fun getStepsData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val request = AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    startDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            )

            val response = healthConnectClient.aggregate(request)

            listOf(
                FitnessDataEntity(
                    userId = getCurrentUserId(),
                    date = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    dataType = FitnessDataType.STEPS,
                    value = response[StepsRecord.COUNT_TOTAL]?.toDouble() ?: 0.0,
                    unit = "steps",
                    source = FitnessDataSource.HEALTH_CONNECT
                )
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Read distance data for a date range
     */
    suspend fun getDistanceData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val request = ReadRecordsRequest(
                recordType = DistanceRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            )

            val response = healthConnectClient.readRecords(request)

            response.records.map { record ->
                FitnessDataEntity(
                    userId = getCurrentUserId(),
                    date = record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE),
                    dataType = FitnessDataType.DISTANCE,
                    value = record.distance.inMeters,
                    unit = "meters",
                    source = FitnessDataSource.HEALTH_CONNECT,
                    timestamp = record.startTime.toEpochMilli()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Read calories burned data for a date range
     */
    suspend fun getCaloriesData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val request = AggregateRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    startDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            )

            val response = healthConnectClient.aggregate(request)

            listOf(
                FitnessDataEntity(
                    userId = getCurrentUserId(),
                    date = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    dataType = FitnessDataType.CALORIES_BURNED,
                    value = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inCalories ?: 0.0,
                    unit = "calories",
                    source = FitnessDataSource.HEALTH_CONNECT
                )
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Read exercise sessions data for a date range
     */
    suspend fun getExerciseData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            )

            val response = healthConnectClient.readRecords(request)

            response.records.map { record ->
                val durationMinutes = java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                FitnessDataEntity(
                    userId = getCurrentUserId(),
                    date = record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE),
                    dataType = FitnessDataType.ACTIVE_MINUTES,
                    value = durationMinutes.toDouble(),
                    unit = "minutes",
                    source = FitnessDataSource.HEALTH_CONNECT,
                    timestamp = record.startTime.toEpochMilli()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Read sleep data for a date range
     */
    suspend fun getSleepData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            )

            val response = healthConnectClient.readRecords(request)

            response.records.map { record ->
                val durationHours = java.time.Duration.between(record.startTime, record.endTime).toMinutes() / 60.0
                FitnessDataEntity(
                    userId = getCurrentUserId(),
                    date = record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE),
                    dataType = FitnessDataType.SLEEP_DURATION,
                    value = durationHours,
                    unit = "hours",
                    source = FitnessDataSource.HEALTH_CONNECT,
                    timestamp = record.startTime.toEpochMilli()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get comprehensive fitness data for a date range
     */
    suspend fun getFitnessDataForRange(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        val allData = mutableListOf<FitnessDataEntity>()

        try {
            allData.addAll(getStepsData(startDate, endDate))
            allData.addAll(getDistanceData(startDate, endDate))
            allData.addAll(getCaloriesData(startDate, endDate))
            allData.addAll(getExerciseData(startDate, endDate))
            allData.addAll(getSleepData(startDate, endDate))
        } catch (e: Exception) {
            // Log error but return partial data if some succeeded
        }

        return allData
    }

    /**
     * Get daily fitness summary
     */
    suspend fun getDailySummary(date: LocalDate): Map<FitnessDataType, Double> {
        val data = getFitnessDataForRange(date, date)
        return data.groupBy { it.dataType }
            .mapValues { (_, values) -> values.sumOf { it.value } }
    }

    /**
     * Stream real-time fitness data updates
     */
    fun getFitnessDataStream(startDate: LocalDate, endDate: LocalDate): Flow<List<FitnessDataEntity>> = flow {
        while (true) {
            emit(getFitnessDataForRange(startDate, endDate))
            kotlinx.coroutines.delay(60000) // Update every minute
        }
    }

    // Helper function to get current user ID (implement based on your auth system)
    private fun getCurrentUserId(): String {
        // TODO: Implement based on your authentication system
        return "current_user_id"
    }
}
