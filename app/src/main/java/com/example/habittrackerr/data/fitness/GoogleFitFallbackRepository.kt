package com.example.habittrackerr.data.fitness

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.example.habittrackerr.data.entities.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Google Fit integration (fallback when Health Connect unavailable)
 */
@Singleton
class GoogleFitFallbackRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .build()

    /**
     * Check if Google Fit is available and user has granted permissions
     */
    fun isAvailable(): Boolean {
        return try {
            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            GoogleSignIn.hasPermissions(account, fitnessOptions)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if permissions are granted
     */
    fun hasPermissions(): Boolean {
        return try {
            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            GoogleSignIn.hasPermissions(account, fitnessOptions)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get fitness options for permission request
     */
    fun getFitnessOptions(): FitnessOptions = fitnessOptions

    /**
     * Read steps data from Google Fit
     */
    suspend fun getStepsData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                return emptyList()
            }

            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val request = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(request)
                .await()

            response.buckets.flatMap { bucket ->
                bucket.dataSets.flatMap { dataSet ->
                    dataSet.dataPoints.map { dataPoint ->
                        val date = LocalDate.ofInstant(
                            java.time.Instant.ofEpochMilli(dataPoint.getStartTime(TimeUnit.MILLISECONDS)),
                            ZoneId.systemDefault()
                        )

                        FitnessDataEntity(
                            userId = getCurrentUserId(),
                            date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            dataType = FitnessDataType.STEPS,
                            value = dataPoint.getValue(Field.FIELD_STEPS).asInt().toDouble(),
                            unit = "steps",
                            source = FitnessDataSource.GOOGLE_FIT,
                            timestamp = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Read distance data from Google Fit
     */
    suspend fun getDistanceData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                return emptyList()
            }

            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val request = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(request)
                .await()

            response.buckets.flatMap { bucket ->
                bucket.dataSets.flatMap { dataSet ->
                    dataSet.dataPoints.map { dataPoint ->
                        val date = LocalDate.ofInstant(
                            java.time.Instant.ofEpochMilli(dataPoint.getStartTime(TimeUnit.MILLISECONDS)),
                            ZoneId.systemDefault()
                        )

                        FitnessDataEntity(
                            userId = getCurrentUserId(),
                            date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            dataType = FitnessDataType.DISTANCE,
                            value = dataPoint.getValue(Field.FIELD_DISTANCE).asFloat().toDouble(),
                            unit = "meters",
                            source = FitnessDataSource.GOOGLE_FIT,
                            timestamp = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Read calories data from Google Fit
     */
    suspend fun getCaloriesData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                return emptyList()
            }

            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val request = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(request)
                .await()

            response.buckets.flatMap { bucket ->
                bucket.dataSets.flatMap { dataSet ->
                    dataSet.dataPoints.map { dataPoint ->
                        val date = LocalDate.ofInstant(
                            java.time.Instant.ofEpochMilli(dataPoint.getStartTime(TimeUnit.MILLISECONDS)),
                            ZoneId.systemDefault()
                        )

                        FitnessDataEntity(
                            userId = getCurrentUserId(),
                            date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            dataType = FitnessDataType.CALORIES_BURNED,
                            value = dataPoint.getValue(Field.FIELD_CALORIES).asFloat().toDouble(),
                            unit = "calories",
                            source = FitnessDataSource.GOOGLE_FIT,
                            timestamp = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                        )
                    }
                }
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

    // Helper function to get current user ID
    private fun getCurrentUserId(): String {
        // TODO: Implement based on your authentication system
        return "current_user_id"
    }
}
