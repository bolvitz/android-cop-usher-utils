package com.eventmonitor.core.data.repository.interfaces

import com.eventmonitor.core.data.local.entities.BranchEntity
import com.eventmonitor.core.data.local.entities.BranchWithAreas
import com.eventmonitor.core.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface BranchRepository {
    fun getAllActiveBranches(): Flow<List<BranchWithAreas>>
    fun getAllBranches(): Flow<List<BranchWithAreas>>
    fun getBranchById(id: String): Flow<BranchWithAreas?>
    fun getActiveBranchCount(): Flow<Int>
    suspend fun branchNameExists(name: String, excludeBranchId: String? = null): Boolean
    suspend fun branchCodeExists(code: String, excludeBranchId: String? = null): Boolean

    suspend fun createBranch(
        name: String,
        location: String,
        code: String,
        contactPerson: String = "",
        contactEmail: String = "",
        contactPhone: String = "",
        color: String = "#1976D2"
    ): Result<String>

    suspend fun updateBranch(branch: BranchEntity): Result<Unit>
    suspend fun deleteBranch(branchId: String): Result<Unit>
    suspend fun setBranchActive(branchId: String, isActive: Boolean)
    suspend fun createDefaultAreasForBranch(branchId: String, areaCount: Int = 6)
    suspend fun hasServices(branchId: String): Boolean
}
