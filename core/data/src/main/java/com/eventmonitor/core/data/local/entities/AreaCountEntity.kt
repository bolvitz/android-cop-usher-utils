package com.eventmonitor.core.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "area_counts",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AreaTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaTemplateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("eventId"),
        Index("areaTemplateId"),
        Index(value = ["eventId", "areaTemplateId"], name = "idx_area_counts_service_template")
    ]
)
data class AreaCountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val areaTemplateId: String,
    val count: Int = 0,
    val capacity: Int, // Snapshot from template at time of service
    val notes: String = "", // Area-specific notes like "Setup extra chairs"
    val countHistory: String = "", // JSON array of count changes for undo/redo
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSyncedToCloud: Boolean = false
)
