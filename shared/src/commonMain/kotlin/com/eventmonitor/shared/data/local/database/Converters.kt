package com.eventmonitor.shared.data.local.database

import androidx.room.TypeConverter
import com.eventmonitor.shared.data.models.CountHistoryItem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else json.decodeFromString(value)

    @TypeConverter
    fun toStringList(list: List<String>): String = json.encodeToString(list)

    @TypeConverter
    fun fromCountHistoryList(value: String): List<CountHistoryItem> =
        if (value.isEmpty()) emptyList() else json.decodeFromString(value)

    @TypeConverter
    fun toCountHistoryList(list: List<CountHistoryItem>): String = json.encodeToString(list)
}
