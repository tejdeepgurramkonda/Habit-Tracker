package com.example.habittrackerr.auth.oneplus

import android.content.Context
import android.os.Build
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnePlusCompatibilityManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "OnePlusCompat"
        private val ONEPLUS_BRANDS = listOf("OnePlus", "ONEPLUS", "oneplus")
        private val OPPO_BRANDS = listOf("OPPO", "oppo", "Oppo")
    }

    val isOnePlusDevice: Boolean by lazy {
        isOnePlusOrOppoDevice()
    }

    private fun isOnePlusOrOppoDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER
        val brand = Build.BRAND
        val model = Build.MODEL

        return ONEPLUS_BRANDS.any {
            manufacturer.contains(it, ignoreCase = true) ||
            brand.contains(it, ignoreCase = true) ||
            model.contains(it, ignoreCase = true)
        } || OPPO_BRANDS.any {
            manufacturer.contains(it, ignoreCase = true) ||
            brand.contains(it, ignoreCase = true) ||
            model.contains(it, ignoreCase = true)
        }
    }

    fun setupOnePlusCompatibility() {
        if (!isOnePlusDevice) return

        try {
            Log.d(TAG, "Setting up OnePlus compatibility mode")

            // Disable problematic Google Play Services features
            System.setProperty("com.google.android.gms.disable_auto_init", "true")
            System.setProperty("com.google.android.gms.auth.disable_broker", "true")
            System.setProperty("com.google.android.gms.safetynet.disable", "true")

            // Set OnePlus-specific configuration
            System.setProperty("firebase.auth.disable_phone_auth", "false")
            System.setProperty("firebase.auth.use_credential_manager", "true")

            Log.d(TAG, "OnePlus compatibility mode enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup OnePlus compatibility", e)
        }
    }

    fun handleGooglePlayServicesError(error: Throwable): Boolean {
        if (!isOnePlusDevice) return false

        val errorMessage = error.message ?: ""

        // Check if this is the OnePlus-specific "Unknown calling package name" error
        if (errorMessage.contains("Unknown calling package name") ||
            errorMessage.contains("com.google.android.gms") ||
            errorMessage.contains("SecurityException")) {

            Log.w(TAG, "OnePlus Google Play Services error detected: $errorMessage")

            // This is an expected error on OnePlus devices - return true to indicate it's handled
            return true
        }

        return false
    }

    fun isGooglePlayServicesWorkaround(): Boolean {
        if (!isOnePlusDevice) return false

        try {
            // Try to check Google Play Services availability, but handle gracefully if not available
            // Use reflection to avoid compile-time dependency issues
            val gmsClass = Class.forName("com.google.android.gms.common.GoogleApiAvailability")
            val getInstance = gmsClass.getMethod("getInstance")
            val instance = getInstance.invoke(null)
            val isAvailable = gmsClass.getMethod("isGooglePlayServicesAvailable", Context::class.java)
            val result = isAvailable.invoke(instance, context) as Int

            return when (result) {
                0 -> false // SUCCESS
                else -> {
                    Log.d(TAG, "Google Play Services issue detected, using workaround mode")
                    true
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Google Play Services not available or incompatible, using workaround mode")
            return true // Use workaround if we can't check availability
        }
    }

    fun logDeviceInfo() {
        Log.d(TAG, "Device Info - Manufacturer: ${Build.MANUFACTURER}, Brand: ${Build.BRAND}, Model: ${Build.MODEL}")
        Log.d(TAG, "OnePlus device detected: $isOnePlusDevice")
    }
}
