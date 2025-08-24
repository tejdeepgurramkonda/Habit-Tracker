package com.example.habittrackerr.data.fitness

import android.content.Context
import android.util.Log
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
    private val tag = "GoogleFitRepository"

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .build()

    /**
     * Check if Google Fit is available and user has granted permissions
     */
    fun isAvailable(): Boolean {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            account != null && GoogleSignIn.hasPermissions(account, fitnessOptions)
        } catch (e: Exception) {
            Log.e(tag, "Error checking Google Fit availability", e)
            false
        }
    }

    /**
     * Check if permissions are granted
     */
    fun hasPermissions(): Boolean {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            account != null && GoogleSignIn.hasPermissions(account, fitnessOptions)
        } catch (e: Exception) {
            Log.e(tag, "Error checking Google Fit permissions", e)
            false
        }
    }

    /**
     * Get fitness options for permission request
     */
    fun getFitnessOptions(): FitnessOptions = fitnessOptions

    /**
     * Main method called by StatsViewModel - Get all fitness data for date range
     */
    suspend fun getFitnessDataForRange(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        if (!isAvailable()) {
            Log.w(tag, "Google Fit not available or permissions not granted")
            return emptyList()
        }

        return try {
            val allData = mutableListOf<FitnessDataEntity>()

            // Get steps data
            allData.addAll(getStepsData(startDate, endDate))

            // Get distance data
            allData.addAll(getDistanceData(startDate, endDate))

            // Get calories data
            allData.addAll(getCaloriesData(startDate, endDate))

            Log.d(tag, "Successfully fetched ${allData.size} fitness data points from Google Fit")
            allData
        } catch (e: Exception) {
            Log.e(tag, "Error fetching fitness data from Google Fit", e)
            emptyList()
        }
    }

    /**
     * Read steps data from Google Fit
     */
    private suspend fun getStepsData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null || !GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                Log.w(tag, "No Google account or permissions for steps data")
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

            val stepsData = response.buckets.flatMap { bucket ->
                bucket.dataSets.flatMap { dataSet ->
                    dataSet.dataPoints.map { dataPoint ->
                        val date = LocalDate.ofInstant(
                            java.time.Instant.ofEpochMilli(dataPoint.getStartTime(TimeUnit.MILLISECONDS)),
                            ZoneId.systemDefault()
                        )

                        FitnessDataEntity(
                            userId = "default_user", // Fixed: Use default user ID
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

            Log.d(tag, "Fetched ${stepsData.size} steps data points")
            stepsData
        } catch (e: Exception) {
            Log.e(tag, "Error fetching steps data from Google Fit", e)
            emptyList()
        }
    }

    /**
     * Read distance data from Google Fit
     */
    private suspend fun getDistanceData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null || !GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                Log.w(tag, "No Google account or permissions for distance data")
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

            val distanceData = response.buckets.flatMap { bucket ->
                bucket.dataSets.flatMap { dataSet ->
                    dataSet.dataPoints.map { dataPoint ->
                        val date = LocalDate.ofInstant(
                            java.time.Instant.ofEpochMilli(dataPoint.getStartTime(TimeUnit.MILLISECONDS)),
                            ZoneId.systemDefault()
                        )

                        FitnessDataEntity(
                            userId = "default_user", // Fixed: Use default user ID
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

            Log.d(tag, "Fetched ${distanceData.size} distance data points")
            distanceData
        } catch (e: Exception) {
            Log.e(tag, "Error fetching distance data from Google Fit", e)
            emptyList()
        }
    }

    /**
     * Read calories data from Google Fit
     */
    private suspend fun getCaloriesData(startDate: LocalDate, endDate: LocalDate): List<FitnessDataEntity> {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null || !GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                Log.w(tag, "No Google account or permissions for calories data")
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

            val caloriesData = response.buckets.flatMap { bucket ->
                bucket.dataSets.flatMap { dataSet ->
                    dataSet.dataPoints.map { dataPoint ->
                        val date = LocalDate.ofInstant(
                            java.time.Instant.ofEpochMilli(dataPoint.getStartTime(TimeUnit.MILLISECONDS)),
                            ZoneId.systemDefault()
                        )

                        FitnessDataEntity(
                            userId = "default_user", // Fixed: Use default user ID
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

            Log.d(tag, "Fetched ${caloriesData.size} calories data points")
            caloriesData
        } catch (e: Exception) {
            Log.e(tag, "Error fetching calories data from Google Fit", e)
            emptyList()
        }
    }

    /**
     * Request Google Fit permissions
     */
    fun requestPermissions(): Boolean {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            account != null
        } catch (e: Exception) {
            Log.e(tag, "Error requesting Google Fit permissions", e)
            false
        }
    }
}
