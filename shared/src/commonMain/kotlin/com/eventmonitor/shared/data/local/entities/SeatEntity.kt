package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

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
    val id: String = newId(),
    val rowId: String,
    val number: Int,
    val seatType: String = "STANDARD",
    val label: String,
    val createdAt: Long = nowMillis()
)
