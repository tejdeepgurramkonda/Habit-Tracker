package com.example.habittrackerr

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userPreferencesDao: UserPreferencesDao,
    private val userProfileDao: UserProfileDao,
    private val habitDao: HabitDao
) {

    fun getUserPreferences(userId: String): Flow<UserPreferences?> {
        return userPreferencesDao.getUserPreferences(userId)
    }

    fun getUserProfile(userId: String): Flow<UserProfile?> {
        return userProfileDao.getUserProfile(userId)
    }

    suspend fun createUserProfile(userId: String, email: String, displayName: String): UserProfile {
        val profile = UserProfile(
            userId = userId,
            email = email,
            displayName = displayName,
            joinedDate = System.currentTimeMillis()
        )
        userProfileDao.insertUserProfile(profile)

        // Create default preferences
        val preferences = UserPreferences(userId = userId)
        userPreferencesDao.insertUserPreferences(preferences)

        return profile
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
        userProfileDao.updateUserProfile(updatedProfile)
    }

    suspend fun updateUserPreferences(preferences: UserPreferences) {
        val updatedPreferences = preferences.copy(updatedAt = System.currentTimeMillis())
        userPreferencesDao.updateUserPreferences(updatedPreferences)
    }

    suspend fun syncUserStatistics(userId: String) {
        val habits = habitDao.getAllHabits().first()
        val stats = calculateUserStatistics(habits)

        userProfileDao.updateTotalHabits(userId, stats.totalHabits)
        userProfileDao.updateTotalCompletions(userId, stats.totalCompletions)
        userProfileDao.updateCurrentStreak(userId, stats.currentStreak)
        userProfileDao.updateLongestStreak(userId, stats.longestStreak)

        // Calculate experience points based on completions
        val experiencePoints = calculateExperiencePoints(stats.totalCompletions, stats.currentStreak)
        val level = calculateLevel(experiencePoints)

        userProfileDao.updateExperiencePoints(userId, experiencePoints)
        userProfileDao.updateLevel(userId, level)
        userProfileDao.updateLastActiveDate(userId)
    }

    suspend fun updateLastActiveDate(userId: String) {
        userProfileDao.updateLastActiveDate(userId)
    }

    suspend fun deleteUserData(userId: String) {
        userPreferencesDao.deleteUserPreferences(userId)
        userProfileDao.deleteUserProfile(userId)
    }

    // Get combined user data
    fun getUserData(userId: String): Flow<UserData?> {
        return combine(
            getUserProfile(userId),
            getUserPreferences(userId)
        ) { profile, preferences ->
            if (profile != null && preferences != null) {
                UserData(profile, preferences)
            } else null
        }
    }

    private fun calculateUserStatistics(habits: List<Habit>): UserStatistics {
        val totalHabits = habits.size
        val totalCompletions = habits.sumOf { it.completedDates.size }
        val currentStreak = calculateCurrentStreak(habits)
        val longestStreak = calculateLongestStreak(habits)

        return UserStatistics(
            totalHabits = totalHabits,
            totalCompletions = totalCompletions,
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }

    private fun calculateExperiencePoints(completions: Int, streak: Int): Int {
        // Base XP: 10 per completion
        // Bonus XP: streak multiplier
        val baseXP = completions * 10
        val streakBonus = when {
            streak >= 30 -> 500
            streak >= 14 -> 200
            streak >= 7 -> 100
            streak >= 3 -> 50
            else -> 0
        }
        return baseXP + streakBonus
    }

    private fun calculateLevel(experiencePoints: Int): Int {
        return (experiencePoints / 100) + 1
    }
}

data class UserData(
    val profile: UserProfile,
    val preferences: UserPreferences
)

data class UserStatistics(
    val totalHabits: Int,
    val totalCompletions: Int,
    val currentStreak: Int,
    val longestStreak: Int
)

// Helper functions for streak calculations
fun calculateCurrentStreak(habits: List<Habit>): Int {
    if (habits.isEmpty()) return 0

    val calendar = java.util.Calendar.getInstance()
    var currentStreak = 0
    var dayOffset = 0

    while (true) {
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -dayOffset)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        val dayStart = calendar.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000

        val habitsCompletedThisDay = habits.count { habit ->
            habit.completedDates.any { date ->
                date in dayStart until dayEnd
            }
        }

        if (habitsCompletedThisDay > 0) {
            currentStreak++
            dayOffset++
        } else {
            break
        }

        if (dayOffset > 365) break // Prevent infinite loop
    }

    return currentStreak
}

fun calculateLongestStreak(habits: List<Habit>): Int {
    if (habits.isEmpty()) return 0

    val allDates = habits.flatMap { it.completedDates }.sorted()
    if (allDates.isEmpty()) return 0

    var longestStreak = 1
    var currentStreak = 1

    for (i in 1 until allDates.size) {
        val prevDate = java.util.Calendar.getInstance().apply { timeInMillis = allDates[i-1] }
        val currentDate = java.util.Calendar.getInstance().apply { timeInMillis = allDates[i] }

        // Normalize to day level
        prevDate.set(java.util.Calendar.HOUR_OF_DAY, 0)
        prevDate.set(java.util.Calendar.MINUTE, 0)
        prevDate.set(java.util.Calendar.SECOND, 0)
        prevDate.set(java.util.Calendar.MILLISECOND, 0)

        currentDate.set(java.util.Calendar.HOUR_OF_DAY, 0)
        currentDate.set(java.util.Calendar.MINUTE, 0)
        currentDate.set(java.util.Calendar.SECOND, 0)
        currentDate.set(java.util.Calendar.MILLISECOND, 0)

        val daysDiff = ((currentDate.timeInMillis - prevDate.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

        if (daysDiff == 1) {
            currentStreak++
            longestStreak = maxOf(longestStreak, currentStreak)
        } else if (daysDiff > 1) {
            currentStreak = 1
        }
    }

    return longestStreak
}
