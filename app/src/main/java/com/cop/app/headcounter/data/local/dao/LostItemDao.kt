package com.cop.app.headcounter.data.local.dao

import androidx.room.*
import com.cop.app.headcounter.data.local.entities.LostItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LostItemDao {
    @Query("SELECT * FROM lost_items WHERE locationId = :locationId ORDER BY foundDate DESC")
    fun getItemsByLocation(locationId: String): Flow<List<LostItemEntity>>

    @Query("SELECT * FROM lost_items WHERE status = :status ORDER BY foundDate DESC")
    fun getItemsByStatus(status: String): Flow<List<LostItemEntity>>

    @Query("SELECT * FROM lost_items WHERE category = :category ORDER BY foundDate DESC")
    fun getItemsByCategory(category: String): Flow<List<LostItemEntity>>

    @Query("SELECT * FROM lost_items WHERE id = :itemId")
    fun getItemById(itemId: String): Flow<LostItemEntity?>

    @Query("SELECT * FROM lost_items ORDER BY foundDate DESC")
    fun getAllItems(): Flow<List<LostItemEntity>>

    @Query("""
        SELECT * FROM lost_items
        WHERE locationId = :locationId
        AND status = :status
        ORDER BY foundDate DESC
    """)
    fun getItemsByLocationAndStatus(
        locationId: String,
        status: String
    ): Flow<List<LostItemEntity>>

    @Query("""
        SELECT * FROM lost_items
        WHERE description LIKE '%' || :searchQuery || '%'
        OR color LIKE '%' || :searchQuery || '%'
        OR brand LIKE '%' || :searchQuery || '%'
        ORDER BY foundDate DESC
    """)
    fun searchItems(searchQuery: String): Flow<List<LostItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LostItemEntity): Long

    @Update
    suspend fun updateItem(item: LostItemEntity)

    @Delete
    suspend fun deleteItem(item: LostItemEntity)

    @Query("UPDATE lost_items SET status = :status, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemStatus(itemId: String, status: String, updatedAt: Long)

    @Query("""
        UPDATE lost_items
        SET status = :status,
            claimedBy = :claimedBy,
            claimedDate = :claimedDate,
            claimerContact = :claimerContact,
            verificationNotes = :verificationNotes,
            updatedAt = :updatedAt
        WHERE id = :itemId
    """)
    suspend fun claimItem(
        itemId: String,
        status: String,
        claimedBy: String,
        claimedDate: Long,
        claimerContact: String,
        verificationNotes: String,
        updatedAt: Long
    )

    @Query("SELECT COUNT(*) FROM lost_items WHERE status = :status")
    fun getItemCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM lost_items WHERE locationId = :locationId")
    fun getItemCountByLocation(locationId: String): Flow<Int>

    @Query("DELETE FROM lost_items WHERE status = 'DISPOSED' AND foundDate < :olderThan")
    suspend fun deleteOldDisposedItems(olderThan: Long)
}
