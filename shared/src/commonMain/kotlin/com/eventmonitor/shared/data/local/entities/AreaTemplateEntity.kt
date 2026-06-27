package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

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
    val id: String = newId(),
    val venueId: String,
    val name: String,
    val type: String,
    val capacity: Int = 100,
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val color: String = "#4CAF50",
    val icon: String = "chair",
    val notes: String = "",
    val hasSeatMap: Boolean = false,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
    val isSyncedToCloud: Boolean = false
)
