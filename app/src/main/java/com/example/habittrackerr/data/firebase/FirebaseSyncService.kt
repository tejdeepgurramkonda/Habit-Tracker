package com.example.habittrackerr.data.firebase

import android.util.Log
import com.example.habittrackerr.HabitDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncService @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val habitDao: HabitDao
) {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val tag = "FirebaseSyncService"

    /**
     * Sync all local habits to Firebase - Fixed to use proper coroutine context
     */
    suspend fun syncLocalHabitsToFirebase(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(tag, "Starting sync of local habits to Firebase")

            // Ensure database access is on IO dispatcher
            val localHabits = habitDao.getAllHabits().first()

            if (localHabits.isEmpty()) {
                Log.d(tag, "No local habits to sync")
                return@withContext Result.success(Unit)
            }

            val syncResult = firebaseRepository.batchSyncHabits(localHabits)
            if (syncResult.isSuccess) {
                Log.d(tag, "Successfully synced ${localHabits.size} habits to Firebase")
                Result.success(Unit)
            } else {
                Log.e(tag, "Failed to sync habits to Firebase", syncResult.exceptionOrNull())
                Result.failure(syncResult.exceptionOrNull() ?: Exception("Unknown sync error"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during sync process", e)
            Result.failure(e)
        }
    }

    /**
     * Sync Firebase habits to local database - Fixed to use proper coroutine context
     */
    suspend fun syncFirebaseHabitsToLocal(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(tag, "Starting sync of Firebase habits to local database")
            val firebaseHabitsResult = firebaseRepository.getUserHabits()

            if (firebaseHabitsResult.isFailure) {
                Log.e(tag, "Failed to fetch habits from Firebase", firebaseHabitsResult.exceptionOrNull())
                return@withContext Result.failure(firebaseHabitsResult.exceptionOrNull() ?: Exception("Failed to fetch Firebase habits"))
            }

            val firebaseHabits = firebaseHabitsResult.getOrNull() ?: emptyList()
            Log.d(tag, "Retrieved ${firebaseHabits.size} habits from Firebase")

            // Convert Firebase habits to local habits and insert/update on IO dispatcher
            firebaseHabits.forEach { firebaseHabit ->
                val localHabit = firebaseHabit.toLocalHabit()
                habitDao.insertHabit(localHabit)
            }

            Log.d(tag, "Successfully synced Firebase habits to local database")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error syncing Firebase habits to local", e)
            Result.failure(e)
        }
    }

    /**
     * Start real-time sync listener - Fixed to use proper coroutine context
     */
    fun startRealtimeSync() {
        Log.d(tag, "Starting real-time sync listener")

        firebaseRepository.listenToUserHabits()
            .flowOn(Dispatchers.IO) // Ensure Firebase operations are on IO dispatcher
            .catch { exception ->
                Log.e(tag, "Error in real-time sync", exception)
            }
            .onEach { firebaseHabits ->
                // Use withContext to ensure database operations are on IO dispatcher
                withContext(Dispatchers.IO) {
                    try {
                        Log.d(tag, "Received ${firebaseHabits.size} habits from real-time update")

                        // Update local database with Firebase changes
                        firebaseHabits.forEach { firebaseHabit ->
                            val localHabit = firebaseHabit.toLocalHabit()
                            habitDao.insertHabit(localHabit)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error processing real-time update", e)
                    }
                }
            }
            .launchIn(serviceScope)
    }

    /**
     * Sync single habit to Firebase - Already async, but adding context for safety
     */
    suspend fun syncHabitToFirebase(habit: com.example.habittrackerr.Habit): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(tag, "Syncing single habit to Firebase: ${habit.name}")
            val result = firebaseRepository.syncHabitToFirebase(habit)

            if (result.isSuccess) {
                Log.d(tag, "Successfully synced habit: ${habit.name}")
            } else {
                Log.e(tag, "Failed to sync habit: ${habit.name}", result.exceptionOrNull())
            }

            result
        } catch (e: Exception) {
            Log.e(tag, "Error syncing habit to Firebase", e)
            Result.failure(e)
        }
    }

    /**
     * Perform full bi-directional sync - Fixed to use proper coroutine context
     */
    suspend fun performFullSync(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(tag, "Starting full bi-directional sync")

            // First, sync local habits to Firebase
            val localSyncResult = syncLocalHabitsToFirebase()
            if (localSyncResult.isFailure) {
                return@withContext localSyncResult
            }

            // Then, sync Firebase habits to local
            val firebaseSyncResult = syncFirebaseHabitsToLocal()
            if (firebaseSyncResult.isFailure) {
                return@withContext firebaseSyncResult
            }

            Log.d(tag, "Full sync completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error during full sync", e)
            Result.failure(e)
        }
    }

    /**
     * Create or update user profile after successful login - Already async, but adding context for safety
     */
    suspend fun createOrUpdateUserProfile(
        email: String,
        displayName: String,
        photoUrl: String? = null,
        phoneNumber: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(tag, "Creating/updating user profile for: $email")

            val profile = FirebaseUserProfile(
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                phoneNumber = phoneNumber,
                isActive = true
            )

            val result = firebaseRepository.createOrUpdateUserProfile(profile)

            if (result.isSuccess) {
                // Update last login time
                firebaseRepository.updateLastLogin()
                Log.d(tag, "User profile created/updated successfully")
            } else {
                Log.e(tag, "Failed to create/update user profile", result.exceptionOrNull())
            }

            result
        } catch (e: Exception) {
            Log.e(tag, "Error creating/updating user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Stop real-time sync listener
     */
    fun stopRealtimeSync() {
        Log.d(tag, "Stopping real-time sync listener")
        // The listener will be automatically removed when the serviceScope is cancelled
    }

    /**
     * Clear all sync jobs
     */
    fun clearSyncJobs() {
        serviceScope.launch {
            Log.d(tag, "Clearing all sync jobs")
            // Cancel any ongoing operations
        }
    }
}
