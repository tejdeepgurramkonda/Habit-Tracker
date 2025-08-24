package com.example.habittrackerr

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val iconId: String,
    val colorHex: String,
    val frequencyType: String,
    val frequencyValue: List<Int>,
    val createdAt: Long = System.currentTimeMillis(),
    val completedDates: List<Long> = emptyList(), // Completion timestamps
    val isDeleted: Boolean = false, // Soft delete support
    val deletedAt: Long? = null // Deletion timestamp
)
