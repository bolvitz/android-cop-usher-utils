package com.eventmonitor.shared.data.local.dao

import androidx.room.*
import com.eventmonitor.shared.data.local.entities.SeatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeatDao {
    @Query("SELECT * FROM seats WHERE rowId = :rowId ORDER BY number ASC")
    suspend fun getSeatsForRow(rowId: String): List<SeatEntity>

    @Query("""
        SELECT s.* FROM seats s
        INNER JOIN seat_rows r ON s.rowId = r.id
        WHERE r.areaTemplateId = :areaTemplateId
        ORDER BY r.displayOrder ASC, s.number ASC
    """)
    fun observeSeatsForArea(areaTemplateId: String): Flow<List<SeatEntity>>

    @Query("""
        SELECT COUNT(*) FROM seats s
        INNER JOIN seat_rows r ON s.rowId = r.id
        WHERE r.areaTemplateId = :areaTemplateId
    """)
    fun observeSeatCountForArea(areaTemplateId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeats(seats: List<SeatEntity>)

    @Delete
    suspend fun deleteSeats(seats: List<SeatEntity>)

    @Query("DELETE FROM seats WHERE rowId = :rowId")
    suspend fun deleteAllSeatsForRow(rowId: String)
}
