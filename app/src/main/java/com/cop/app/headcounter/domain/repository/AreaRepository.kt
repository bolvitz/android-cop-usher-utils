package com.cop.app.headcounter.domain.repository

import com.cop.app.headcounter.data.local.entities.AreaTemplateEntity
import com.cop.app.headcounter.domain.models.AreaType
import kotlinx.coroutines.flow.Flow

interface AreaRepository {
    fun getAreasByBranch(branchId: String): Flow<List<AreaTemplateEntity>>
    fun getAreaById(areaId: String): Flow<AreaTemplateEntity?>
    fun getTotalCapacityForBranch(branchId: String): Flow<Int?>

    suspend fun createArea(
        branchId: String,
        name: String,
        type: AreaType,
        capacity: Int,
        displayOrder: Int
    ): String

    suspend fun updateArea(area: AreaTemplateEntity)
    suspend fun deleteArea(areaId: String)
    suspend fun setAreaActive(areaId: String, isActive: Boolean)
    suspend fun reorderAreas(areaIds: List<String>)
    suspend fun duplicateAreaToBranches(areaId: String, targetBranchIds: List<String>)
}
