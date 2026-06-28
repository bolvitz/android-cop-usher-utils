package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.local.entities.SeatRowWithSeats
import com.eventmonitor.shared.data.local.entities.SeatStatusEntity
import kotlinx.coroutines.flow.Flow

interface SeatMapRepository {
    // --- Runtime (per-event occupancy) ---
    fun observeRowsWithSeats(areaTemplateId: String): Flow<List<SeatRowWithSeats>>
    fun observeStatusesForArea(eventId: String, areaTemplateId: String): Flow<List<SeatStatusEntity>>
    fun observeOccupiedCountsByArea(eventId: String): Flow<Map<String, Int>>
    fun observeOccupiedCountForArea(eventId: String, areaTemplateId: String): Flow<Int>
    fun observeSeatCountForArea(areaTemplateId: String): Flow<Int>
    /** Sets a seat's status; "AVAILABLE" clears the row (absence == available). */
    suspend fun setSeatStatus(eventId: String, seatId: String, status: String)

    // --- Layout editing ---
    /** Toggles the area's seat-map flag; clears rows/seats when turning off. */
    suspend fun setHasSeatMap(areaTemplateId: String, enabled: Boolean)
    /** Appends a new row with [seatCount] seats; auto-assigns next label/order. */
    suspend fun addRow(areaTemplateId: String, seatCount: Int = 10): String
    /** Adds/removes trailing seats so the row has [newSeatCount] seats. */
    suspend fun resizeRow(rowId: String, newSeatCount: Int)
    suspend fun renameRow(rowId: String, label: String)
    suspend fun deleteRow(rowId: String)
}
