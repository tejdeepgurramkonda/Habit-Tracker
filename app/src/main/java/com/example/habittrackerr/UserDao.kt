package com.example.habittrackerr

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    fun getUserPreferences(userId: String): Flow<UserPreferences?>

    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    suspend fun getUserPreferencesSync(userId: String): UserPreferences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreferences(preferences: UserPreferences)

    @Update
    suspend fun updateUserPreferences(preferences: UserPreferences)

    @Query("DELETE FROM user_preferences WHERE userId = :userId")
    suspend fun deleteUserPreferences(userId: String)

    @Query("UPDATE user_preferences SET updatedAt = :timestamp WHERE userId = :userId")
    suspend fun updateLastModified(userId: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    fun getUserProfile(userId: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    suspend fun getUserProfileSync(userId: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile WHERE userId = :userId")
    suspend fun deleteUserProfile(userId: String)

    @Query("UPDATE user_profile SET lastActiveDate = :timestamp WHERE userId = :userId")
    suspend fun updateLastActiveDate(userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET totalHabits = :count WHERE userId = :userId")
    suspend fun updateTotalHabits(userId: String, count: Int)

    @Query("UPDATE user_profile SET totalCompletions = :count WHERE userId = :userId")
    suspend fun updateTotalCompletions(userId: String, count: Int)

    @Query("UPDATE user_profile SET currentStreak = :streak WHERE userId = :userId")
    suspend fun updateCurrentStreak(userId: String, streak: Int)

    @Query("UPDATE user_profile SET longestStreak = :streak WHERE userId = :userId")
    suspend fun updateLongestStreak(userId: String, streak: Int)

    @Query("UPDATE user_profile SET experiencePoints = :xp WHERE userId = :userId")
    suspend fun updateExperiencePoints(userId: String, xp: Int)

    @Query("UPDATE user_profile SET level = :level WHERE userId = :userId")
    suspend fun updateLevel(userId: String, level: Int)
}
