package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.models.LostItemDto
import com.eventmonitor.shared.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface LostItemRepository {
    fun getAllItems(): Flow<List<LostItemDto>>
    fun getItemsByLocation(locationId: String): Flow<List<LostItemDto>>
    fun getItemsByStatus(status: String): Flow<List<LostItemDto>>
    fun getItemsByCategory(category: String): Flow<List<LostItemDto>>
    fun getItemById(itemId: String): Flow<LostItemDto?>
    fun getItemsByLocationAndStatus(locationId: String, status: String): Flow<List<LostItemDto>>
    fun searchItems(query: String): Flow<List<LostItemDto>>

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
        notes: String = "",
        eventId: String? = null
    ): Result<String>

    suspend fun updateItem(item: LostItemDto): Result<Unit>
    suspend fun deleteItem(itemId: String): Result<Unit>
    suspend fun updateItemStatus(itemId: String, status: String): Result<Unit>
    suspend fun claimItem(
        itemId: String,
        claimedBy: String,
        claimerContact: String,
        verificationNotes: String = ""
    ): Result<Unit>
}
