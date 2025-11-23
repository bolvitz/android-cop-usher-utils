package com.cop.app.headcounter.domain.repository

import com.cop.app.headcounter.data.local.entities.LostItemEntity
import com.cop.app.headcounter.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface LostItemRepository {
    fun getAllItems(): Flow<List<LostItemEntity>>
    fun getItemsByLocation(locationId: String): Flow<List<LostItemEntity>>
    fun getItemsByStatus(status: String): Flow<List<LostItemEntity>>
    fun getItemsByCategory(category: String): Flow<List<LostItemEntity>>
    fun getItemById(itemId: String): Flow<LostItemEntity?>
    fun getItemsByLocationAndStatus(locationId: String, status: String): Flow<List<LostItemEntity>>
    fun searchItems(query: String): Flow<List<LostItemEntity>>
    fun getItemCountByStatus(status: String): Flow<Int>
    fun getItemCountByLocation(locationId: String): Flow<Int>

    suspend fun createItem(
        locationId: String,
        description: String,
        category: String,
        foundZone: String,
        photoUri: String = "",
        color: String = "",
        brand: String = "",
        identifyingMarks: String = "",
        reportedBy: String = "",
        notes: String = ""
    ): Result<String>

    suspend fun updateItem(item: LostItemEntity): Result<Unit>
    suspend fun deleteItem(itemId: String): Result<Unit>
    suspend fun updateItemStatus(itemId: String, status: String): Result<Unit>

    suspend fun claimItem(
        itemId: String,
        claimedBy: String,
        claimerContact: String,
        verificationNotes: String = ""
    ): Result<Unit>

    suspend fun deleteOldDisposedItems(daysOld: Int = 90)
}
