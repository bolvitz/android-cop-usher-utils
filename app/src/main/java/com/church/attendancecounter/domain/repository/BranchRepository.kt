package com.church.attendancecounter.domain.repository

import com.church.attendancecounter.data.local.entities.BranchEntity
import com.church.attendancecounter.data.local.entities.BranchWithAreas
import kotlinx.coroutines.flow.Flow

interface BranchRepository {
    fun getAllActiveBranches(): Flow<List<BranchWithAreas>>
    fun getAllBranches(): Flow<List<BranchWithAreas>>
    fun getBranchById(id: String): Flow<BranchWithAreas?>
    fun getActiveBranchCount(): Flow<Int>

    suspend fun createBranch(
        name: String,
        location: String,
        code: String,
        contactPerson: String = "",
        contactEmail: String = "",
        contactPhone: String = "",
        color: String = "#1976D2"
    ): String

    suspend fun updateBranch(branch: BranchEntity)
    suspend fun deleteBranch(branchId: String)
    suspend fun setBranchActive(branchId: String, isActive: Boolean)
    suspend fun createDefaultAreasForBranch(branchId: String, areaCount: Int = 6)
}
