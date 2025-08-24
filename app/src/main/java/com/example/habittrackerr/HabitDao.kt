package com.example.habittrackerr

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabitsIncludingDeleted(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllHabitsFlow(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllHabitsSync(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Int)

    // Soft delete methods
    @Query("UPDATE habits SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteHabit(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE habits SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreHabit(id: Int)

    @Query("SELECT * FROM habits WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedHabits(): Flow<List<Habit>>
}
