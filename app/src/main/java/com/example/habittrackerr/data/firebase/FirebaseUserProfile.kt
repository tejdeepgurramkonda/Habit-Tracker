package com.example.habittrackerr.data.firebase

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firebase user profile model for storing user data in Firestore
 */
data class FirebaseUserProfile(
    @DocumentId
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val lastLoginAt: Date? = null,
    val totalHabits: Int = 0,
    val streakCount: Int = 0,
    val longestStreak: Int = 0,
    val preferences: UserPreferences = UserPreferences(),
    val isActive: Boolean = true,
    @ServerTimestamp
    val updatedAt: Date? = null
) {
    constructor() : this(
        userId = "",
        email = "",
        displayName = "",
        photoUrl = null,
        phoneNumber = null,
        createdAt = null,
        lastLoginAt = null,
        totalHabits = 0,
        streakCount = 0,
        longestStreak = 0,
        preferences = UserPreferences(),
        isActive = true,
        updatedAt = null
    )
}

/**
 * User preferences sub-document
 */
data class UserPreferences(
    val theme: String = "system", // light, dark, system
    val notificationsEnabled: Boolean = true,
    val reminderTime: String = "09:00",
    val weekStartsOn: String = "monday", // monday, sunday
    val language: String = "en",
    val biometricEnabled: Boolean = false,
    val syncEnabled: Boolean = true
) {
    constructor() : this(
        theme = "system",
        notificationsEnabled = true,
        reminderTime = "09:00",
        weekStartsOn = "monday",
        language = "en",
        biometricEnabled = false,
        syncEnabled = true
    )
}
