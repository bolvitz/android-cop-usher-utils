package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.local.dao.SeatRowDao
import com.eventmonitor.shared.data.local.dao.SeatStatusDao
import com.eventmonitor.shared.data.local.entities.SeatRowWithSeats
import com.eventmonitor.shared.data.local.entities.SeatStatusEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SeatMapRepositoryImpl(
    private val seatRowDao: SeatRowDao,
    private val seatStatusDao: SeatStatusDao
) : SeatMapRepository {

    override fun observeRowsWithSeats(areaTemplateId: String): Flow<List<SeatRowWithSeats>> =
        seatRowDao.observeRowsWithSeats(areaTemplateId)

    override fun observeStatusesForArea(eventId: String, areaTemplateId: String): Flow<List<SeatStatusEntity>> =
        seatStatusDao.observeStatusesForArea(eventId, areaTemplateId)

    override fun observeOccupiedCountsByArea(eventId: String): Flow<Map<String, Int>> =
        seatStatusDao.observeOccupiedCountsByArea(eventId)
            .map { rows -> rows.associate { it.areaTemplateId to it.occupied } }

    override fun observeOccupiedCountForArea(eventId: String, areaTemplateId: String): Flow<Int> =
        seatStatusDao.observeOccupiedCountForArea(eventId, areaTemplateId)

    override suspend fun setSeatStatus(eventId: String, seatId: String, status: String) {
        if (status == "AVAILABLE") {
            seatStatusDao.clearStatus(eventId, seatId)
        } else {
            seatStatusDao.upsertStatus(
                SeatStatusEntity(eventId = eventId, seatId = seatId, status = status)
            )
        }
    }
}
