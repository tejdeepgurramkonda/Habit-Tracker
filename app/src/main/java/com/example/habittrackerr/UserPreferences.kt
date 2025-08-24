package com.example.habittrackerr

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val userId: String,
    val displayName: String = "",
    val profileImageUrl: String? = null,
    val notificationsEnabled: Boolean = true,
    val reminderSound: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val dailyReminderTime: String = "09:00", // HH:mm format
    val weeklyReportEnabled: Boolean = true,
    val shareDataWithGoogleFit: Boolean = false,
    val privacyMode: Boolean = false,
    val autoBackup: Boolean = true,
    val theme: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val language: String = "en",
    val timeFormat24h: Boolean = true,
    val firstDayOfWeek: Int = 1, // 1 = Monday, 0 = Sunday
    val streakResetTime: String = "00:00",
    val completionGoal: Int = 100, // percentage
    val motivationalQuotes: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val userId: String,
    val email: String,
    val displayName: String,
    val profileImageUrl: String? = null,
    val joinedDate: Long = System.currentTimeMillis(),
    val lastActiveDate: Long = System.currentTimeMillis(),
    val totalHabits: Int = 0,
    val totalCompletions: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val perfectDays: Int = 0,
    val averageCompletionRate: Float = 0f,
    val favoriteHabitCategory: String = "",
    val timeZone: String = "",
    val isPublicProfile: Boolean = false,
    val bio: String = "",
    val achievements: List<String> = emptyList(),
    val level: Int = 1,
    val experiencePoints: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconResId: String,
    val unlockedDate: Long? = null,
    val isUnlocked: Boolean = false,
    val category: AchievementCategory,
    val requiredValue: Int,
    val currentValue: Int = 0
)

enum class AchievementCategory {
    STREAK,
    COMPLETION,
    VARIETY,
    CONSISTENCY,
    MILESTONE
}

// Extension functions for UserProfile calculations
fun UserProfile.getCompletionRate(): Float {
    return if (totalHabits > 0) {
        (totalCompletions.toFloat() / (totalHabits * getDaysSinceJoined())) * 100f
    } else 0f
}

fun UserProfile.getDaysSinceJoined(): Int {
    val currentTime = System.currentTimeMillis()
    val daysDiff = (currentTime - joinedDate) / (24 * 60 * 60 * 1000)
    return maxOf(1, daysDiff.toInt()) // At least 1 day
}

fun UserProfile.getNextLevelExperience(): Int {
    return level * 100 // 100 XP per level
}

fun UserProfile.getExperienceProgress(): Float {
    val nextLevelXP = getNextLevelExperience()
    val currentLevelXP = (level - 1) * 100
    val progressXP = experiencePoints - currentLevelXP
    val neededXP = nextLevelXP - currentLevelXP
    return if (neededXP > 0) progressXP.toFloat() / neededXP else 0f
}
