package com.eventmonitor.core.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "seats",
    foreignKeys = [
        ForeignKey(
            entity = SeatRowEntity::class,
            parentColumns = ["id"],
            childColumns = ["rowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("rowId"),
        Index(value = ["rowId", "number"], unique = true, name = "idx_seats_row_number")
    ]
)
data class SeatEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val rowId: String,
    val number: Int, // 1-based within row
    val seatType: String = "STANDARD", // STANDARD, VIP, WHEELCHAIR, COMPANION
    val label: String, // "$rowLabel$number", e.g. "A12"
    val createdAt: Long = System.currentTimeMillis()
)
