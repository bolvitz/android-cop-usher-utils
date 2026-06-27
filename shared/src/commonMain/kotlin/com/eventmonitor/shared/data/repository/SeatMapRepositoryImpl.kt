package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.local.dao.AreaTemplateDao
import com.eventmonitor.shared.data.local.dao.SeatDao
import com.eventmonitor.shared.data.local.dao.SeatRowDao
import com.eventmonitor.shared.data.local.dao.SeatStatusDao
import com.eventmonitor.shared.data.local.entities.SeatEntity
import com.eventmonitor.shared.data.local.entities.SeatRowEntity
import com.eventmonitor.shared.data.local.entities.SeatRowWithSeats
import com.eventmonitor.shared.data.local.entities.SeatStatusEntity
import com.eventmonitor.shared.util.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SeatMapRepositoryImpl(
    private val seatRowDao: SeatRowDao,
    private val seatStatusDao: SeatStatusDao,
    private val seatDao: SeatDao,
    private val areaTemplateDao: AreaTemplateDao
) : SeatMapRepository {

    // --- Runtime ---
    override fun observeRowsWithSeats(areaTemplateId: String): Flow<List<SeatRowWithSeats>> =
        seatRowDao.observeRowsWithSeats(areaTemplateId)

    override fun observeStatusesForArea(eventId: String, areaTemplateId: String): Flow<List<SeatStatusEntity>> =
        seatStatusDao.observeStatusesForArea(eventId, areaTemplateId)

    override fun observeOccupiedCountsByArea(eventId: String): Flow<Map<String, Int>> =
        seatStatusDao.observeOccupiedCountsByArea(eventId)
            .map { rows -> rows.associate { it.areaTemplateId to it.occupied } }

    override fun observeOccupiedCountForArea(eventId: String, areaTemplateId: String): Flow<Int> =
        seatStatusDao.observeOccupiedCountForArea(eventId, areaTemplateId)

    override fun observeSeatCountForArea(areaTemplateId: String): Flow<Int> =
        seatDao.observeSeatCountForArea(areaTemplateId)

    override suspend fun setSeatStatus(eventId: String, seatId: String, status: String) {
        if (status == "AVAILABLE") {
            seatStatusDao.clearStatus(eventId, seatId)
        } else {
            seatStatusDao.upsertStatus(SeatStatusEntity(eventId = eventId, seatId = seatId, status = status))
        }
    }

    // --- Layout editing ---
    override suspend fun setHasSeatMap(areaTemplateId: String, enabled: Boolean) {
        val area = areaTemplateDao.getAreaById(areaTemplateId).first() ?: return
        if (area.hasSeatMap == enabled) return
        if (!enabled) {
            seatRowDao.deleteAllRowsForArea(areaTemplateId) // cascades to seats + statuses
        }
        areaTemplateDao.updateArea(area.copy(hasSeatMap = enabled))
    }

    override suspend fun addRow(areaTemplateId: String, seatCount: Int): String {
        val existing = seatRowDao.getRowsForArea(areaTemplateId)
        val order = existing.size
        val label = rowLabel(order)
        val row = SeatRowEntity(id = newId(), areaTemplateId = areaTemplateId, label = label, displayOrder = order)
        seatRowDao.insertRow(row)
        seatDao.insertSeats(generateSeats(row.id, label, 1..seatCount))
        return row.id
    }

    override suspend fun resizeRow(rowId: String, newSeatCount: Int) {
        val row = seatRowDao.getRowById(rowId) ?: return
        val current = seatDao.getSeatsForRow(rowId).sortedBy { it.number }
        when {
            newSeatCount > current.size ->
                seatDao.insertSeats(generateSeats(rowId, row.label, (current.size + 1)..newSeatCount))
            newSeatCount < current.size ->
                seatDao.deleteSeats(current.drop(newSeatCount))
        }
    }

    override suspend fun renameRow(rowId: String, label: String) {
        val row = seatRowDao.getRowById(rowId) ?: return
        seatRowDao.updateRow(row.copy(label = label))
    }

    override suspend fun deleteRow(rowId: String) {
        seatRowDao.deleteRowById(rowId) // cascades to seats + statuses
    }

    private fun generateSeats(rowId: String, rowLabel: String, numbers: IntRange): List<SeatEntity> =
        numbers.map { n ->
            SeatEntity(id = newId(), rowId = rowId, number = n, label = "$rowLabel$n")
        }

    /** 0 -> "A", 25 -> "Z", 26 -> "AA", ... */
    private fun rowLabel(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (true) {
            sb.append(('A' + (i % 26)))
            i = i / 26 - 1
            if (i < 0) break
        }
        return sb.reverse().toString()
    }
}
