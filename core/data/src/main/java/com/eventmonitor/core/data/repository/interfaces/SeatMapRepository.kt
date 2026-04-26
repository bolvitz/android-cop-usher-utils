package com.eventmonitor.core.data.repository.interfaces

import com.eventmonitor.core.data.local.entities.SeatRowWithSeats
import com.eventmonitor.core.data.local.entities.SeatStatusEntity
import kotlinx.coroutines.flow.Flow

/**
 * Per-area seat map management. A row owns ordered seats; a per-event status
 * record represents that seat's occupancy for one event. Absence of a status
 * record means AVAILABLE for that (event, seat).
 */
interface SeatMapRepository {

    fun observeRowsWithSeats(areaTemplateId: String): Flow<List<SeatRowWithSeats>>

    fun observeStatusesForArea(
        eventId: String,
        areaTemplateId: String
    ): Flow<List<SeatStatusEntity>>

    fun observeOccupiedCountForArea(eventId: String, areaTemplateId: String): Flow<Int>

    /** Map of areaTemplateId → occupied seat count for every seat-mapped area in this event. */
    fun observeOccupiedCountsByArea(eventId: String): Flow<Map<String, Int>>

    fun observeSeatCountForArea(areaTemplateId: String): Flow<Int>

    /** Toggles the seat map flag and clears rows/seats if turning off. */
    suspend fun setHasSeatMap(areaTemplateId: String, enabled: Boolean)

    /** Appends a new row with `seatCount` seats; auto-assigns next label/order. */
    suspend fun addRow(areaTemplateId: String, seatCount: Int = 10): String

    /** Resizes an existing row by adding/removing trailing seats. Statuses for removed seats cascade. */
    suspend fun resizeRow(rowId: String, newSeatCount: Int)

    suspend fun renameRow(rowId: String, label: String)

    suspend fun deleteRow(rowId: String)

    suspend fun reorderRows(areaTemplateId: String, rowIdsInOrder: List<String>)

    suspend fun setSeatStatus(eventId: String, seatId: String, status: String)
}
