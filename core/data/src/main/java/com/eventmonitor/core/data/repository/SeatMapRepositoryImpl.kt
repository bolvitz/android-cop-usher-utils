package com.eventmonitor.core.data.repository

import androidx.room.withTransaction
import com.eventmonitor.core.data.local.dao.AreaTemplateDao
import com.eventmonitor.core.data.local.dao.SeatDao
import com.eventmonitor.core.data.local.dao.SeatRowDao
import com.eventmonitor.core.data.local.dao.SeatStatusDao
import com.eventmonitor.core.data.local.database.AppDatabase
import com.eventmonitor.core.data.local.entities.SeatEntity
import com.eventmonitor.core.data.local.entities.SeatRowEntity
import com.eventmonitor.core.data.local.entities.SeatRowWithSeats
import com.eventmonitor.core.data.local.entities.SeatStatusEntity
import com.eventmonitor.core.data.repository.interfaces.EventRepository
import com.eventmonitor.core.data.repository.interfaces.SeatMapRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SeatMapRepositoryImpl constructor(
    private val database: AppDatabase,
    private val seatRowDao: SeatRowDao,
    private val seatDao: SeatDao,
    private val seatStatusDao: SeatStatusDao,
    private val areaTemplateDao: AreaTemplateDao,
    private val eventRepository: EventRepository
) : SeatMapRepository {

    override fun observeRowsWithSeats(areaTemplateId: String): Flow<List<SeatRowWithSeats>> =
        seatRowDao.observeRowsWithSeats(areaTemplateId)

    override fun observeStatusesForArea(
        eventId: String,
        areaTemplateId: String
    ): Flow<List<SeatStatusEntity>> =
        seatStatusDao.observeStatusesForArea(eventId, areaTemplateId)

    override fun observeOccupiedCountForArea(eventId: String, areaTemplateId: String): Flow<Int> =
        seatStatusDao.observeOccupiedCountForArea(eventId, areaTemplateId)

    override fun observeOccupiedCountsByArea(eventId: String): Flow<Map<String, Int>> =
        seatStatusDao.observeOccupiedCountsByArea(eventId)
            .map { rows -> rows.associate { it.areaTemplateId to it.occupied } }

    override fun observeSeatCountForArea(areaTemplateId: String): Flow<Int> =
        seatDao.observeSeatCountForArea(areaTemplateId)

    override suspend fun setHasSeatMap(areaTemplateId: String, enabled: Boolean) {
        database.withTransaction {
            val area = areaTemplateDao.getAreaById(areaTemplateId).first() ?: return@withTransaction
            if (area.hasSeatMap == enabled) return@withTransaction
            areaTemplateDao.updateArea(
                area.copy(hasSeatMap = enabled, updatedAt = System.currentTimeMillis())
            )
            if (!enabled) {
                // Cascading FKs drop seats and statuses with the rows.
                seatRowDao.deleteAllRowsForArea(areaTemplateId)
            }
        }
    }

    override suspend fun addRow(areaTemplateId: String, seatCount: Int): String {
        val safeCount = seatCount.coerceIn(1, MAX_SEATS_PER_ROW)
        return database.withTransaction {
            val rows = seatRowDao.getRowsForArea(areaTemplateId)
            val nextOrder = (rows.maxOfOrNull { it.displayOrder } ?: -1) + 1
            val label = nextRowLabel(rows.map { it.label })
            val row = SeatRowEntity(
                areaTemplateId = areaTemplateId,
                label = label,
                displayOrder = nextOrder
            )
            seatRowDao.insertRow(row)
            seatDao.insertSeats(buildSeats(row.id, label, 1, safeCount))
            row.id
        }
    }

    override suspend fun resizeRow(rowId: String, newSeatCount: Int) {
        val safeCount = newSeatCount.coerceIn(0, MAX_SEATS_PER_ROW)
        database.withTransaction {
            val row = seatRowDao.getRowById(rowId) ?: return@withTransaction
            val current = seatDao.getSeatsForRow(rowId).sortedBy { it.number }
            when {
                safeCount == current.size -> Unit
                safeCount > current.size -> {
                    val startNumber = (current.maxOfOrNull { it.number } ?: 0) + 1
                    val toAdd = safeCount - current.size
                    seatDao.insertSeats(buildSeats(rowId, row.label, startNumber, toAdd))
                }

                else -> {
                    val toRemove = current.takeLast(current.size - safeCount)
                    seatDao.deleteSeats(toRemove)
                }
            }
            seatRowDao.updateRow(row.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun renameRow(rowId: String, label: String) {
        val trimmed = label.trim().ifBlank { return }.take(8)
        database.withTransaction {
            val row = seatRowDao.getRowById(rowId) ?: return@withTransaction
            if (row.label == trimmed) return@withTransaction
            seatRowDao.updateRow(row.copy(label = trimmed, updatedAt = System.currentTimeMillis()))
            // Re-label child seats so their display labels stay in sync.
            val seats = seatDao.getSeatsForRow(rowId)
            val relabelled = seats.map { it.copy(label = "$trimmed${it.number}") }
            if (relabelled.isNotEmpty()) seatDao.insertSeats(relabelled)
        }
    }

    override suspend fun deleteRow(rowId: String) {
        seatRowDao.deleteRowById(rowId)
    }

    override suspend fun reorderRows(areaTemplateId: String, rowIdsInOrder: List<String>) {
        database.withTransaction {
            val byId = seatRowDao.getRowsForArea(areaTemplateId).associateBy { it.id }
            rowIdsInOrder.forEachIndexed { index, id ->
                val row = byId[id] ?: return@forEachIndexed
                if (row.displayOrder != index) {
                    seatRowDao.updateRow(
                        row.copy(displayOrder = index, updatedAt = System.currentTimeMillis())
                    )
                }
            }
        }
    }

    override suspend fun setSeatStatus(eventId: String, seatId: String, status: String) {
        database.withTransaction {
            if (status == "AVAILABLE") {
                seatStatusDao.clearStatus(eventId, seatId)
            } else {
                val existing = seatStatusDao.getStatus(eventId, seatId)
                seatStatusDao.upsertStatus(
                    SeatStatusEntity(
                        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                        eventId = eventId,
                        seatId = seatId,
                        status = status,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
            // Keep event.totalAttendance in sync so reports/exports stay accurate.
            eventRepository.recalculateEventTotal(eventId)
        }
    }

    private fun buildSeats(
        rowId: String,
        rowLabel: String,
        startNumber: Int,
        count: Int
    ): List<SeatEntity> = (0 until count).map { i ->
        val n = startNumber + i
        SeatEntity(
            rowId = rowId,
            number = n,
            label = "$rowLabel$n"
        )
    }

    private fun nextRowLabel(existing: List<String>): String {
        // Default label scheme: A, B, ..., Z, AA, AB, ...
        val taken = existing.toSet()
        var idx = 0
        while (true) {
            val label = labelForIndex(idx)
            if (label !in taken) return label
            idx++
        }
    }

    private fun labelForIndex(index: Int): String {
        var n = index
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A' + (n % 26)))
            n = n / 26 - 1
        } while (n >= 0)
        return sb.toString()
    }

    companion object {
        private const val MAX_SEATS_PER_ROW = 60
    }
}
