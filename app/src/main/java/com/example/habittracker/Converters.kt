package com.example.habittracker

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

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
                    // If all else fails, return empty list
                    emptyList()
                }
            } catch (e: NumberFormatException) {
                // Return empty list as fallback
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
            // Try to parse as a JSON array of longs
            val listType = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            try {
                // If that fails, try to parse as a single long and convert to list
                val singleValue = value.toLongOrNull()
                if (singleValue != null) {
                    listOf(singleValue)
                } else {
                    // If all else fails, return empty list
                    emptyList()
                }
            } catch (e: NumberFormatException) {
                // Return empty list as fallback
                emptyList()
            }
        }
    }
}
