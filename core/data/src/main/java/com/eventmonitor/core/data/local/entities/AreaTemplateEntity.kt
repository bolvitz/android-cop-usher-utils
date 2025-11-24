package com.eventmonitor.core.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "area_templates",
    foreignKeys = [
        ForeignKey(
            entity = VenueEntity::class,
            parentColumns = ["id"],
            childColumns = ["venueId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("venueId"), Index("displayOrder")]
)
data class AreaTemplateEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val venueId: String,
    val name: String, // e.g., "Bay 1", "Baby Room", "Balcony"
    val type: String, // BAY, BABY_ROOM, BALCONY, OVERFLOW, PARKING, OTHER
    val capacity: Int = 100,
    val isActive: Boolean = true,
    val displayOrder: Int = 0, // For sorting in UI
    val color: String = "#4CAF50", // Visual identification
    val icon: String = "chair", // Material icon name
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSyncedToCloud: Boolean = false
)
