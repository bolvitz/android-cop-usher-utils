package com.eventmonitor.core.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "venues",
    indices = [Index(value = ["code"], unique = true)]
)
data class VenueEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String, // e.g., "Main Campus", "North Venue", "Downtown"
    val location: String, // Address or location description
    val code: String, // Short code like "MC", "NV", "DT"
    val isActive: Boolean = true,
    val logoUrl: String = "",
    val color: String = "#1976D2", // Brand color for this venue
    val contactPerson: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val timezone: String = "UTC",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSyncedToCloud: Boolean = false,
    val cloudId: String = "", // Firebase document ID
    // Feature toggles
    val isHeadCountEnabled: Boolean = true,
    val isLostAndFoundEnabled: Boolean = false,
    val isIncidentReportingEnabled: Boolean = false
)
