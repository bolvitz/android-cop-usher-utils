package com.church.attendancecounter.data.local.database

import androidx.room.TypeConverter
import com.church.attendancecounter.data.models.CountHistoryItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList()
        else json.decodeFromString(value)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromCountHistoryList(value: String): List<CountHistoryItem> {
        return if (value.isEmpty()) emptyList()
        else json.decodeFromString(value)
    }

    @TypeConverter
    fun toCountHistoryList(list: List<CountHistoryItem>): String {
        return json.encodeToString(list)
    }
}
