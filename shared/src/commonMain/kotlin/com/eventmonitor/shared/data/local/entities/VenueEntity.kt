package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

@Entity(
    tableName = "venues",
    indices = [Index(value = ["code"], unique = true)]
)
data class VenueEntity(
    @PrimaryKey
    val id: String = newId(),
    val name: String,
    val location: String,
    val code: String,
    val isActive: Boolean = true,
    val logoUrl: String = "",
    val color: String = "#1976D2",
    val contactPerson: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val timezone: String = "UTC",
    val notes: String = "",
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
    val isSyncedToCloud: Boolean = false,
    val cloudId: String = "",
    val isHeadCountEnabled: Boolean = true,
    val isLostAndFoundEnabled: Boolean = false,
    val isIncidentReportingEnabled: Boolean = false
)
