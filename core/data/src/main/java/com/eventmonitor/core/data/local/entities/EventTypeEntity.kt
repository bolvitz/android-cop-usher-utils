package com.eventmonitor.core.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "event_types",
    indices = [Index(value = ["name"], unique = true)]
)
data class EventTypeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String, // e.g., "Sunday Morning Service", "Wednesday Bible Study"
    val dayType: String, // e.g., "Sunday", "Wednesday", "Saturday"
    val time: String, // e.g., "9:00 AM", "7:00 PM"
    val description: String = "",
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
