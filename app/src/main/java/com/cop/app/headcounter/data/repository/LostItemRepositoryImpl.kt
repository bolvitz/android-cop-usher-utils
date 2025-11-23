package com.cop.app.headcounter.data.repository

import com.cop.app.headcounter.data.local.dao.LostItemDao
import com.cop.app.headcounter.data.local.entities.LostItemEntity
import com.cop.app.headcounter.domain.common.AppError
import com.cop.app.headcounter.domain.common.Result
import com.cop.app.headcounter.domain.common.resultOf
import com.cop.app.headcounter.domain.models.ItemStatus
import com.cop.app.headcounter.domain.repository.LostItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class LostItemRepositoryImpl @Inject constructor(
    private val lostItemDao: LostItemDao
) : LostItemRepository {

    override fun getAllItems(): Flow<List<LostItemEntity>> =
        lostItemDao.getAllItems()

    override fun getItemsByLocation(locationId: String): Flow<List<LostItemEntity>> =
        lostItemDao.getItemsByLocation(locationId)

    override fun getItemsByStatus(status: String): Flow<List<LostItemEntity>> =
        lostItemDao.getItemsByStatus(status)

    override fun getItemsByCategory(category: String): Flow<List<LostItemEntity>> =
        lostItemDao.getItemsByCategory(category)

    override fun getItemById(itemId: String): Flow<LostItemEntity?> =
        lostItemDao.getItemById(itemId)

    override fun getItemsByLocationAndStatus(
        locationId: String,
        status: String
    ): Flow<List<LostItemEntity>> =
        lostItemDao.getItemsByLocationAndStatus(locationId, status)

    override fun searchItems(query: String): Flow<List<LostItemEntity>> =
        lostItemDao.searchItems(query)

    override fun getItemCountByStatus(status: String): Flow<Int> =
        lostItemDao.getItemCountByStatus(status)

    override fun getItemCountByLocation(locationId: String): Flow<Int> =
        lostItemDao.getItemCountByLocation(locationId)

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
        notes: String
    ): Result<String> {
        // Validate input
        if (description.isBlank()) {
            return Result.Error(
                AppError.ValidationError("Description cannot be empty")
            )
        }

        if (foundZone.isBlank()) {
            return Result.Error(
                AppError.ValidationError("Found zone/location is required")
            )
        }

        return resultOf {
            val itemId = UUID.randomUUID().toString()
            val item = LostItemEntity(
                id = itemId,
                locationId = locationId,
                description = description,
                category = category,
                foundZone = foundZone,
                foundDate = System.currentTimeMillis(),
                photoUri = photoUri,
                color = color,
                brand = brand,
                identifyingMarks = identifyingMarks,
                status = ItemStatus.PENDING.name,
                reportedBy = reportedBy,
                notes = notes
            )

            lostItemDao.insertItem(item)
            itemId
        }
    }

    override suspend fun updateItem(item: LostItemEntity): Result<Unit> {
        if (item.description.isBlank()) {
            return Result.Error(
                AppError.ValidationError("Description cannot be empty")
            )
        }

        return resultOf {
            lostItemDao.updateItem(item.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> {
        val item = lostItemDao.getItemById(itemId).first()
            ?: return Result.Error(AppError.NotFound("Lost Item", itemId))

        return resultOf {
            lostItemDao.deleteItem(item)
        }
    }

    override suspend fun updateItemStatus(itemId: String, status: String): Result<Unit> {
        val item = lostItemDao.getItemById(itemId).first()
            ?: return Result.Error(AppError.NotFound("Lost Item", itemId))

        return resultOf {
            lostItemDao.updateItemStatus(
                itemId = itemId,
                status = status,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun claimItem(
        itemId: String,
        claimedBy: String,
        claimerContact: String,
        verificationNotes: String
    ): Result<Unit> {
        val item = lostItemDao.getItemById(itemId).first()
            ?: return Result.Error(AppError.NotFound("Lost Item", itemId))

        if (claimedBy.isBlank()) {
            return Result.Error(
                AppError.ValidationError("Claimer name is required")
            )
        }

        return resultOf {
            lostItemDao.claimItem(
                itemId = itemId,
                status = ItemStatus.CLAIMED.name,
                claimedBy = claimedBy,
                claimedDate = System.currentTimeMillis(),
                claimerContact = claimerContact,
                verificationNotes = verificationNotes,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun deleteOldDisposedItems(daysOld: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        lostItemDao.deleteOldDisposedItems(cutoffTime)
    }
}
