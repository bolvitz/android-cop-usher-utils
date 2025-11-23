package com.eventmonitor.core.data.repository

import com.eventmonitor.core.data.local.dao.AreaCountDao
import com.eventmonitor.core.data.local.dao.AreaTemplateDao
import com.eventmonitor.core.data.local.entities.AreaTemplateEntity
import com.eventmonitor.core.domain.models.AreaType
import com.eventmonitor.core.data.repository.interfaces.AreaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class AreaRepositoryImpl @Inject constructor(
    private val areaTemplateDao: AreaTemplateDao,
    private val areaCountDao: AreaCountDao
) : AreaRepository {

    override fun getAreasByBranch(branchId: String): Flow<List<AreaTemplateEntity>> =
        areaTemplateDao.getAreasByBranch(branchId)

    override fun getAreaById(areaId: String): Flow<AreaTemplateEntity?> =
        areaTemplateDao.getAreaById(areaId)

    override fun getTotalCapacityForBranch(branchId: String): Flow<Int?> =
        areaTemplateDao.getTotalCapacityForBranch(branchId)

    override suspend fun createArea(
        branchId: String,
        name: String,
        type: AreaType,
        capacity: Int,
        displayOrder: Int
    ): String {
        val areaId = UUID.randomUUID().toString()
        val area = AreaTemplateEntity(
            id = areaId,
            branchId = branchId,
            name = name,
            type = type.name,
            capacity = capacity,
            displayOrder = displayOrder,
            icon = type.defaultIcon
        )

        areaTemplateDao.insertArea(area)
        return areaId
    }

    override suspend fun updateArea(area: AreaTemplateEntity) {
        areaTemplateDao.updateArea(area.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteArea(areaId: String) {
        val area = areaTemplateDao.getAreaById(areaId).first()
        area?.let {
            areaTemplateDao.deleteArea(it)
        }
    }

    override suspend fun setAreaActive(areaId: String, isActive: Boolean) {
        areaTemplateDao.setAreaActive(areaId, isActive)
    }

    override suspend fun reorderAreas(areaIds: List<String>) {
        areaIds.forEachIndexed { index, areaId ->
            areaTemplateDao.updateDisplayOrder(areaId, index)
        }
    }

    override suspend fun duplicateAreaToBranches(areaId: String, targetBranchIds: List<String>) {
        val sourceArea = areaTemplateDao.getAreaById(areaId).first() ?: return

        val newAreas = targetBranchIds.map { branchId ->
            sourceArea.copy(
                id = UUID.randomUUID().toString(),
                branchId = branchId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }

        areaTemplateDao.insertAreas(newAreas)
    }

    override suspend fun hasAreaCounts(areaId: String): Boolean {
        val counts = areaCountDao.getAreaCountsByTemplateId(areaId).first()
        return counts.isNotEmpty()
    }
}
