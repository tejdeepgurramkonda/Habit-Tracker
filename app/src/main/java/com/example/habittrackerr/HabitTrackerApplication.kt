package com.example.habittrackerr

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HabitTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // OnePlus-specific Google Play Services initialization
        initializeForOnePlus()

        // Initialize Firebase with OnePlus compatibility
        initializeFirebaseForOnePlus()
    }

    private fun initializeForOnePlus() {
        try {
            // Check if this is a OnePlus device
            val isOnePlus = Build.MANUFACTURER.equals("OnePlus", ignoreCase = true) ||
                    Build.BRAND.equals("OnePlus", ignoreCase = true) ||
                    Build.MODEL.contains("OnePlus", ignoreCase = true)

            if (isOnePlus) {
                Log.d("HabitTracker", "OnePlus device detected, applying compatibility fixes")

                // Disable Google Play Services automatic initialization
                System.setProperty("com.google.android.gms.disable_auto_init", "true")

                // Set OnePlus-specific flags
                System.setProperty("com.google.android.gms.oneplus_compat", "true")
                System.setProperty("com.google.android.gms.auth.disable_broker", "true")

                // Try to check Google Play Services availability using reflection to avoid compile-time dependencies
                try {
                    val gmsClass = Class.forName("com.google.android.gms.common.GoogleApiAvailability")
                    val getInstance = gmsClass.getMethod("getInstance")
                    val instance = getInstance.invoke(null)
                    val isAvailable = gmsClass.getMethod("isGooglePlayServicesAvailable", android.content.Context::class.java)
                    val result = isAvailable.invoke(instance, this) as Int

                    if (result != 0) {
                        Log.w("HabitTracker", "Google Play Services not fully available: $result")
                        // Continue anyway - we'll use Credential Manager API
                    }
                } catch (e: Exception) {
                    Log.w("HabitTracker", "Could not check Google Play Services availability", e)
                    // Continue with initialization anyway
                }
            }
        } catch (e: Exception) {
            Log.e("HabitTracker", "Error during OnePlus initialization", e)
            // Continue with normal initialization
        }
    }

    private fun initializeFirebaseForOnePlus() {
        try {
            // Initialize Firebase with error handling for OnePlus
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("HabitTracker", "Firebase initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("HabitTracker", "Firebase initialization failed", e)
            // App can still function with local data only
        }
    }
}
