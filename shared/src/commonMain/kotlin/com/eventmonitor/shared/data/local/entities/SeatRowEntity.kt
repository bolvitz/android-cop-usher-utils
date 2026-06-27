package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

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
    val id: String = newId(),
    val areaTemplateId: String,
    val label: String,
    val displayOrder: Int,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)
