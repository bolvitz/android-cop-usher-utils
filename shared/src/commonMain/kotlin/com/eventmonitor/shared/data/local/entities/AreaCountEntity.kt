package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

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
    val id: String = newId(),
    val eventId: String,
    val areaTemplateId: String,
    val count: Int = 0,
    val capacity: Int,
    val notes: String = "",
    val countHistory: String = "",
    val lastUpdated: Long = nowMillis(),
    val isSyncedToCloud: Boolean = false
)
