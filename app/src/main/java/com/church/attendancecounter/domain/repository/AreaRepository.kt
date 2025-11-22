package com.church.attendancecounter.domain.repository

import com.church.attendancecounter.data.local.entities.AreaTemplateEntity
import com.church.attendancecounter.domain.models.AreaType
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
