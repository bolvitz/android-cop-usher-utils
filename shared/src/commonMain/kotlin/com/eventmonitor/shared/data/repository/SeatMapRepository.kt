package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.local.entities.SeatRowWithSeats
import com.eventmonitor.shared.data.local.entities.SeatStatusEntity
import kotlinx.coroutines.flow.Flow

interface SeatMapRepository {
    fun observeRowsWithSeats(areaTemplateId: String): Flow<List<SeatRowWithSeats>>
    fun observeStatusesForArea(eventId: String, areaTemplateId: String): Flow<List<SeatStatusEntity>>
    /** Occupied seat counts per areaTemplateId for the given event. */
    fun observeOccupiedCountsByArea(eventId: String): Flow<Map<String, Int>>
    fun observeOccupiedCountForArea(eventId: String, areaTemplateId: String): Flow<Int>
    /** Sets a seat's status; "AVAILABLE" clears the row (absence == available). */
    suspend fun setSeatStatus(eventId: String, seatId: String, status: String)
}
