package com.example.habittrackerr.data.firebase

import com.example.habittrackerr.Habit
import com.google.firebase.Timestamp
import java.time.Instant

/**
 * Firebase representation of a Habit
 */
data class FirebaseHabit(
    val id: String = "",
    val localId: Long = 0,
    val name: String = "",
    val iconId: String = "",
    val colorHex: String = "",
    val frequencyType: String = "",
    val frequencyValue: List<Int> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val completedDates: List<Timestamp> = emptyList(),
    val isActive: Boolean = true,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val notes: String = "",
    val reminderTime: String? = null,
    val isReminderEnabled: Boolean = false,
    val category: String = "",
    val difficulty: String = "MEDIUM",
    val tags: List<String> = emptyList(),
    val priority: Int = 0,
    val targetValue: Float? = null,
    val unit: String? = null,
    val isArchived: Boolean = false,
    val lastModified: Timestamp = Timestamp.now(),
    val syncStatus: String = "SYNCED"
)

/**
 * Extension function to convert local Habit to FirebaseHabit
 */
fun Habit.toFirebaseHabit(userId: String, firebaseId: String = ""): FirebaseHabit {
    return FirebaseHabit(
        id = firebaseId,
        localId = this.id.toLong(),
        name = this.name,
        iconId = this.iconId,
        colorHex = this.colorHex,
        frequencyType = this.frequencyType,
        frequencyValue = this.frequencyValue,
        createdAt = Timestamp(Instant.ofEpochMilli(this.createdAt)),
        completedDates = this.completedDates.map { Timestamp(Instant.ofEpochMilli(it)) },
        // Set default values for properties not in the local Habit
        isActive = true,
        streak = 0,
        bestStreak = 0,
        totalCompletions = this.completedDates.size,
        notes = "",
        reminderTime = null,
        isReminderEnabled = false,
        category = "",
        difficulty = "MEDIUM",
        tags = emptyList(),
        priority = 0,
        targetValue = null,
        unit = null,
        isArchived = false,
        lastModified = Timestamp.now()
    )
}

/**
 * Extension function to convert FirebaseHabit to local Habit
 */
fun FirebaseHabit.toHabit(): Habit {
    return Habit(
        id = this.localId.toInt(),
        name = this.name,
        iconId = this.iconId,
        colorHex = this.colorHex,
        frequencyType = this.frequencyType,
        frequencyValue = this.frequencyValue,
        createdAt = this.createdAt.toDate().time,
        completedDates = this.completedDates.map { it.toDate().time }
    )
}

/**
 * Alias for backward compatibility
 */
fun FirebaseHabit.toLocalHabit(): Habit = this.toHabit()
