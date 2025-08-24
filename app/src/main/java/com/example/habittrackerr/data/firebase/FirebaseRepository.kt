package com.example.habittrackerr.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val habitsCollection = firestore.collection("habits")
    private val usersCollection = firestore.collection("users")

    /**
     * Get current user ID
     */
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Sync local habit to Firebase
     */
    suspend fun syncHabitToFirebase(habit: com.example.habittrackerr.Habit): Result<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val firebaseHabit = habit.toFirebaseHabit(userId)
            val document = habitsCollection.add(firebaseHabit).await()
            Result.success(document.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update existing Firebase habit
     */
    suspend fun updateFirebaseHabit(firebaseId: String, habit: com.example.habittrackerr.Habit): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val firebaseHabit = habit.toFirebaseHabit(userId, firebaseId)
            habitsCollection.document(firebaseId).set(firebaseHabit).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete habit from Firebase (soft delete)
     */
    suspend fun deleteFirebaseHabit(firebaseId: String): Result<Unit> {
        return try {
            habitsCollection.document(firebaseId)
                .update("isDeleted", true, "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all user habits from Firebase
     */
    suspend fun getUserHabits(): Result<List<FirebaseHabit>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val snapshot = habitsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val habits = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseHabit::class.java)
            }
            Result.success(habits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Listen to real-time habit updates
     */
    fun listenToUserHabits(): Flow<List<FirebaseHabit>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }

        val listener = habitsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isDeleted", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val habits = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseHabit::class.java)
                } ?: emptyList()

                trySend(habits)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Create or update user profile
     */
    suspend fun createOrUpdateUserProfile(profile: FirebaseUserProfile): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            usersCollection.document(userId).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user profile
     */
    suspend fun getUserProfile(): Result<FirebaseUserProfile?> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val snapshot = usersCollection.document(userId).get().await()
            val profile = snapshot.toObject(FirebaseUserProfile::class.java)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user last login time
     */
    suspend fun updateLastLogin(): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            usersCollection.document(userId)
                .update("lastLoginAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Batch sync multiple habits
     */
    suspend fun batchSyncHabits(habits: List<com.example.habittrackerr.Habit>): Result<List<String>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val batch = firestore.batch()
            val documentIds = mutableListOf<String>()

            habits.forEach { habit ->
                val firebaseHabit = habit.toFirebaseHabit(userId)
                val docRef = habitsCollection.document()
                batch.set(docRef, firebaseHabit)
                documentIds.add(docRef.id)
            }

            batch.commit().await()
            Result.success(documentIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
