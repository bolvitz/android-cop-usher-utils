package com.cop.app.headcounter.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "area_counts",
    foreignKeys = [
        ForeignKey(
            entity = ServiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AreaTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaTemplateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("serviceId"), Index("areaTemplateId")]
)
data class AreaCountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val serviceId: String,
    val areaTemplateId: String,
    val count: Int = 0,
    val capacity: Int, // Snapshot from template at time of service
    val notes: String = "", // Area-specific notes like "Setup extra chairs"
    val countHistory: String = "", // JSON array of count changes for undo/redo
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSyncedToCloud: Boolean = false
)
