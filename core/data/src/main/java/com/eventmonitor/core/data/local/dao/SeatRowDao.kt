package com.eventmonitor.core.data.local.dao

import androidx.room.*
import com.eventmonitor.core.data.local.entities.SeatRowEntity
import com.eventmonitor.core.data.local.entities.SeatRowWithSeats
import kotlinx.coroutines.flow.Flow

@Dao
interface SeatRowDao {
    @Transaction
    @Query("SELECT * FROM seat_rows WHERE areaTemplateId = :areaTemplateId ORDER BY displayOrder ASC")
    fun observeRowsWithSeats(areaTemplateId: String): Flow<List<SeatRowWithSeats>>

    @Query("SELECT * FROM seat_rows WHERE areaTemplateId = :areaTemplateId ORDER BY displayOrder ASC")
    suspend fun getRowsForArea(areaTemplateId: String): List<SeatRowEntity>

    @Query("SELECT * FROM seat_rows WHERE id = :rowId")
    suspend fun getRowById(rowId: String): SeatRowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRow(row: SeatRowEntity)

    @Update
    suspend fun updateRow(row: SeatRowEntity)

    @Delete
    suspend fun deleteRow(row: SeatRowEntity)

    @Query("DELETE FROM seat_rows WHERE id = :rowId")
    suspend fun deleteRowById(rowId: String)

    @Query("DELETE FROM seat_rows WHERE areaTemplateId = :areaTemplateId")
    suspend fun deleteAllRowsForArea(areaTemplateId: String)
}
