package com.eventmonitor.core.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "seat_rows",
    foreignKeys = [
        ForeignKey(
            entity = AreaTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaTemplateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("areaTemplateId"),
        Index(value = ["areaTemplateId", "displayOrder"], name = "idx_seat_rows_area_order")
    ]
)
data class SeatRowEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val areaTemplateId: String,
    val label: String, // "A", "B", "AA", etc.
    val displayOrder: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
