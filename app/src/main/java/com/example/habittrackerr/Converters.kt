package com.example.habittrackerr

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.example.habittrackerr.data.entities.*

class Converters {
    private val gson = Gson()

    // Converters for List<Int> (frequencyValue)
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return try {
            // Handle empty or null strings
            if (value.isBlank()) return emptyList()

            // First try to parse as a JSON array
            val listType = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            try {
                // If that fails, try to parse as a single number and convert to list
                val singleValue = value.toIntOrNull()
                if (singleValue != null) {
                    listOf(singleValue)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Converters for List<Long> (completedDates)
    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        return try {
            // Handle empty or null strings
            if (value.isBlank()) return emptyList()

            // First try to parse as a JSON array
            val listType = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            try {
                // If that fails, try to parse as a single number and convert to list
                val singleValue = value.toLongOrNull()
                if (singleValue != null) {
                    listOf(singleValue)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Converters for List<String> (achievements)
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            // Handle empty or null strings
            if (value.isBlank()) return emptyList()

            // Try to parse as a JSON array
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            try {
                // If that fails, try to parse as a single string and convert to list
                if (value.isNotBlank()) {
                    listOf(value)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // New converters for Map<String, String> (metadata)
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return try {
            if (value.isBlank()) return emptyMap()
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // New converters for Map<Int, Int> (timeOfDayDistribution)
    @TypeConverter
    fun fromIntMap(value: Map<Int, Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toIntMap(value: String): Map<Int, Int> {
        return try {
            if (value.isBlank()) return emptyMap()
            val mapType = object : TypeToken<Map<Int, Int>>() {}.type
            gson.fromJson(value, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // New converters for FitnessDataType enum
    @TypeConverter
    fun fromFitnessDataType(value: FitnessDataType): String {
        return value.name
    }

    @TypeConverter
    fun toFitnessDataType(value: String): FitnessDataType {
        return try {
            FitnessDataType.valueOf(value)
        } catch (e: Exception) {
            FitnessDataType.STEPS // Default fallback
        }
    }

    // New converters for FitnessDataSource enum
    @TypeConverter
    fun fromFitnessDataSource(value: FitnessDataSource): String {
        return value.name
    }

    @TypeConverter
    fun toFitnessDataSource(value: String): FitnessDataSource {
        return try {
            FitnessDataSource.valueOf(value)
        } catch (e: Exception) {
            FitnessDataSource.MANUAL_ENTRY // Default fallback
        }
    }

    // New converters for TaskEventType enum
    @TypeConverter
    fun fromTaskEventType(value: TaskEventType): String {
        return value.name
    }

    @TypeConverter
    fun toTaskEventType(value: String): TaskEventType {
        return try {
            TaskEventType.valueOf(value)
        } catch (e: Exception) {
            TaskEventType.VIEWED // Default fallback
        }
    }
}
