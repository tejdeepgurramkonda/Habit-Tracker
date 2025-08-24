package com.example.habittrackerr.permissions

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import com.example.habittrackerr.data.fitness.GoogleFitFallbackRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Health Connect permissions and provides unified permission interface
 */
@Singleton
class HealthConnectPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleFitRepository: GoogleFitFallbackRepository
) {

    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // Health Connect permissions we need
    private val healthConnectPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    /**
     * Check current permission status for both Health Connect and Google Fit
     */
    suspend fun checkPermissions() {
        val healthConnectAvailable = isHealthConnectAvailable()
        val healthConnectGranted = if (healthConnectAvailable) {
            checkHealthConnectPermissions()
        } else false

        val googleFitAvailable = googleFitRepository.isAvailable()
        val googleFitGranted = googleFitRepository.hasPermissions()

        _permissionState.value = PermissionState(
            healthConnectAvailable = healthConnectAvailable,
            healthConnectGranted = healthConnectGranted,
            googleFitAvailable = googleFitAvailable,
            googleFitGranted = googleFitGranted,
            hasAnyPermissions = healthConnectGranted || googleFitGranted
        )
    }

    /**
     * Check if Health Connect is available on this device
     */
    private suspend fun isHealthConnectAvailable(): Boolean {
        return try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if Health Connect permissions are granted
     */
    private suspend fun checkHealthConnectPermissions(): Boolean {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            healthConnectPermissions.all { it in grantedPermissions }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get permissions that need to be requested
     */
    suspend fun getPermissionsToRequest(): Set<String> {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            healthConnectPermissions.filterNot { it in grantedPermissions }
                .map { it.toString() }
                .toSet()
        } catch (e: Exception) {
            healthConnectPermissions.map { it.toString() }.toSet()
        }
    }

    /**
     * Request Health Connect permissions
     */
    fun requestHealthConnectPermissions(
        activity: Activity,
        launcher: ActivityResultLauncher<Set<String>>
    ) {
        try {
            launcher.launch(healthConnectPermissions.map { it.toString() }.toSet())
        } catch (e: Exception) {
            // Handle permission request error
        }
    }

    /**
     * Handle permission result and update state
     */
    suspend fun handlePermissionResult() {
        checkPermissions()
    }

    /**
     * Get user-friendly explanation for permissions
     */
    fun getPermissionRationale(): String {
        return """
            To provide comprehensive health and fitness insights, we need access to your activity data:
            
            • Steps and distance for daily activity tracking
            • Calories burned for energy expenditure analysis
            • Exercise sessions for workout pattern insights
            • Sleep data for recovery and wellness tracking
            • Heart rate data for intensity measurements
            
            Your data is stored securely on your device and used only for personal analytics.
        """.trimIndent()
    }

    /**
     * Check if we should show rationale for permissions
     */
    fun shouldShowRationale(): Boolean {
        val state = _permissionState.value
        return (state.healthConnectAvailable || state.googleFitAvailable) &&
               !state.hasAnyPermissions
    }
}

/**
 * Data class representing current permission state
 */
data class PermissionState(
    val healthConnectAvailable: Boolean = false,
    val healthConnectGranted: Boolean = false,
    val googleFitAvailable: Boolean = false,
    val googleFitGranted: Boolean = false,
    val hasAnyPermissions: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
