package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.local.dao.LostItemDao
import com.eventmonitor.shared.data.local.entities.LostItemEntity
import com.eventmonitor.shared.data.mappers.toDto
import com.eventmonitor.shared.data.models.LostItemDto
import com.eventmonitor.shared.domain.common.AppError
import com.eventmonitor.shared.domain.common.Result
import com.eventmonitor.shared.domain.models.ItemStatus
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LostItemRepositoryImpl(
    private val lostItemDao: LostItemDao
) : LostItemRepository {

    override fun getAllItems(): Flow<List<LostItemDto>> =
        lostItemDao.getAllItems().map { list -> list.map { it.toDto() } }

    override fun getItemsByLocation(locationId: String): Flow<List<LostItemDto>> =
        lostItemDao.getItemsByLocation(locationId).map { list -> list.map { it.toDto() } }

    override fun getItemsByStatus(status: String): Flow<List<LostItemDto>> =
        lostItemDao.getItemsByStatus(status).map { list -> list.map { it.toDto() } }

    override fun getItemsByCategory(category: String): Flow<List<LostItemDto>> =
        lostItemDao.getItemsByCategory(category).map { list -> list.map { it.toDto() } }

    override fun getItemById(itemId: String): Flow<LostItemDto?> =
        lostItemDao.getItemById(itemId).map { it?.toDto() }

    override fun getItemsByLocationAndStatus(locationId: String, status: String): Flow<List<LostItemDto>> =
        lostItemDao.getItemsByLocationAndStatus(locationId, status).map { list -> list.map { it.toDto() } }

    override fun searchItems(query: String): Flow<List<LostItemDto>> =
        lostItemDao.searchItems(query).map { list -> list.map { it.toDto() } }

    override suspend fun createItem(
        locationId: String,
        description: String,
        category: String,
        foundZone: String,
        photoUri: String,
        color: String,
        brand: String,
        identifyingMarks: String,
        reportedBy: String,
        notes: String,
        eventId: String?
    ): Result<String> = try {
        val now = nowMillis()
        val entity = LostItemEntity(
            id = newId(),
            locationId = locationId,
            eventId = eventId,
            description = description,
            category = category,
            foundZone = foundZone,
            foundDate = now,
            photoUri = photoUri,
            color = color,
            brand = brand,
            identifyingMarks = identifyingMarks,
            status = ItemStatus.PENDING.name,
            reportedBy = reportedBy,
            notes = notes,
            createdAt = now,
            updatedAt = now
        )
        lostItemDao.insertItem(entity)
        Result.Success(entity.id)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to create item"))
    }

    override suspend fun updateItem(item: LostItemDto): Result<Unit> = try {
        lostItemDao.updateItem(item.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update item"))
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> = try {
        lostItemDao.deleteItemById(itemId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete item"))
    }

    override suspend fun updateItemStatus(itemId: String, status: String): Result<Unit> = try {
        lostItemDao.updateItemStatus(itemId, status, nowMillis())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update status"))
    }

    override suspend fun claimItem(
        itemId: String,
        claimedBy: String,
        claimerContact: String,
        verificationNotes: String
    ): Result<Unit> = try {
        val now = nowMillis()
        lostItemDao.claimItem(
            itemId = itemId,
            status = ItemStatus.CLAIMED.name,
            claimedBy = claimedBy,
            claimedDate = now,
            claimerContact = claimerContact,
            verificationNotes = verificationNotes,
            updatedAt = now
        )
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to claim item"))
    }
}
