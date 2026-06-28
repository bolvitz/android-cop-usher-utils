package com.eventmonitor.shared.data.local.dao

import androidx.room.*
import com.eventmonitor.shared.data.local.entities.SeatStatusEntity
import kotlinx.coroutines.flow.Flow

data class AreaOccupancyRow(
    val areaTemplateId: String,
    val occupied: Int
)

@Dao
interface SeatStatusDao {
    @Query("SELECT * FROM seat_statuses WHERE eventId = :eventId AND seatId = :seatId LIMIT 1")
    suspend fun getStatus(eventId: String, seatId: String): SeatStatusEntity?

    @Query("""
        SELECT ss.* FROM seat_statuses ss
        INNER JOIN seats s ON ss.seatId = s.id
        INNER JOIN seat_rows r ON s.rowId = r.id
        WHERE ss.eventId = :eventId AND r.areaTemplateId = :areaTemplateId
    """)
    fun observeStatusesForArea(eventId: String, areaTemplateId: String): Flow<List<SeatStatusEntity>>

    @Query("""
        SELECT COUNT(*) FROM seat_statuses ss
        INNER JOIN seats s ON ss.seatId = s.id
        INNER JOIN seat_rows r ON s.rowId = r.id
        WHERE ss.eventId = :eventId
          AND r.areaTemplateId = :areaTemplateId
          AND ss.status = 'OCCUPIED'
    """)
    fun observeOccupiedCountForArea(eventId: String, areaTemplateId: String): Flow<Int>

    @Query("""
        SELECT r.areaTemplateId AS areaTemplateId, COUNT(*) AS occupied
        FROM seat_statuses ss
        INNER JOIN seats s ON ss.seatId = s.id
        INNER JOIN seat_rows r ON s.rowId = r.id
        WHERE ss.eventId = :eventId AND ss.status = 'OCCUPIED'
        GROUP BY r.areaTemplateId
    """)
    fun observeOccupiedCountsByArea(eventId: String): Flow<List<AreaOccupancyRow>>

    @Query("""
        SELECT r.areaTemplateId AS areaTemplateId, COUNT(*) AS occupied
        FROM seat_statuses ss
        INNER JOIN seats s ON ss.seatId = s.id
        INNER JOIN seat_rows r ON s.rowId = r.id
        WHERE ss.eventId = :eventId AND ss.status = 'OCCUPIED'
        GROUP BY r.areaTemplateId
    """)
    suspend fun getOccupiedCountsByArea(eventId: String): List<AreaOccupancyRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatus(status: SeatStatusEntity)

    @Query("DELETE FROM seat_statuses WHERE eventId = :eventId AND seatId = :seatId")
    suspend fun clearStatus(eventId: String, seatId: String)
}
