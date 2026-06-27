package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

@Entity(
    tableName = "event_types",
    indices = [Index(value = ["name"], unique = true)]
)
data class EventTypeEntity(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val dayType: String,
    val time: String,
    val description: String = "",
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)
