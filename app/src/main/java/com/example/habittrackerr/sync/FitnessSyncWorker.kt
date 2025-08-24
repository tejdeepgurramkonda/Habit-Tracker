package com.example.habittrackerr.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.habittrackerr.data.fitness.HealthConnectRepository
import com.example.habittrackerr.data.fitness.GoogleFitFallbackRepository
import com.example.habittrackerr.data.dao.FitnessDataDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Background worker to sync fitness data from Health Connect or Google Fit
 */
@HiltWorker
class FitnessSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthConnectRepository: HealthConnectRepository,
    private val googleFitRepository: GoogleFitFallbackRepository,
    private val fitnessDataDao: FitnessDataDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val syncResult = syncFitnessData()
            if (syncResult) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun syncFitnessData(): Boolean {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)

        // Try Health Connect first, then Google Fit as fallback
        val fitnessData = when {
            healthConnectRepository.isAvailable() && healthConnectRepository.hasAllPermissions() -> {
                healthConnectRepository.getFitnessDataForRange(weekAgo, today)
            }
            googleFitRepository.isAvailable() && googleFitRepository.hasPermissions() -> {
                googleFitRepository.getFitnessDataForRange(weekAgo, today)
            }
            else -> {
                emptyList()
            }
        }

        if (fitnessData.isNotEmpty()) {
            fitnessDataDao.insertFitnessData(fitnessData)
        }

        // Clean up old data (older than 30 days)
        val cutoffDate = today.minusDays(30).toString()
        fitnessDataDao.deleteOldData(cutoffDate)

        return true
    }

    companion object {
        const val WORK_NAME = "fitness_sync_work"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<FitnessSyncWorker>(
                repeatInterval = 6, // Every 6 hours
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 1, // Flex time of 1 hour
                flexTimeIntervalUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
        }

        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
