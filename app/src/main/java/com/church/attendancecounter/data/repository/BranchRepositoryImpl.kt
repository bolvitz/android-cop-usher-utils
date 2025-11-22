package com.church.attendancecounter.data.repository

import com.church.attendancecounter.data.local.dao.AreaTemplateDao
import com.church.attendancecounter.data.local.dao.BranchDao
import com.church.attendancecounter.data.local.entities.AreaTemplateEntity
import com.church.attendancecounter.data.local.entities.BranchEntity
import com.church.attendancecounter.data.local.entities.BranchWithAreas
import com.church.attendancecounter.domain.models.AreaType
import com.church.attendancecounter.domain.repository.BranchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class BranchRepositoryImpl @Inject constructor(
    private val branchDao: BranchDao,
    private val areaTemplateDao: AreaTemplateDao
) : BranchRepository {

    override fun getAllActiveBranches(): Flow<List<BranchWithAreas>> =
        branchDao.getAllBranchesWithAreas()
            .map { list -> list.filter { it.branch.isActive } }

    override fun getAllBranches(): Flow<List<BranchWithAreas>> =
        branchDao.getAllBranchesWithAreas()

    override fun getBranchById(id: String): Flow<BranchWithAreas?> =
        branchDao.getBranchWithAreas(id)

    override fun getActiveBranchCount(): Flow<Int> =
        branchDao.getActiveBranchCount()

    override suspend fun createBranch(
        name: String,
        location: String,
        code: String,
        contactPerson: String,
        contactEmail: String,
        contactPhone: String,
        color: String
    ): String {
        val branchId = UUID.randomUUID().toString()
        val branch = BranchEntity(
            id = branchId,
            name = name,
            location = location,
            code = code,
            contactPerson = contactPerson,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            color = color
        )

        branchDao.insertBranch(branch)
        return branchId
    }

    override suspend fun updateBranch(branch: BranchEntity) {
        branchDao.updateBranch(branch.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteBranch(branchId: String) {
        val branch = branchDao.getBranchById(branchId).first()
        branch?.let {
            branchDao.deleteBranch(it)
        }
    }

    override suspend fun setBranchActive(branchId: String, isActive: Boolean) {
        branchDao.setBranchActive(branchId, isActive)
    }

    override suspend fun createDefaultAreasForBranch(branchId: String, areaCount: Int) {
        val defaultAreas = mutableListOf<AreaTemplateEntity>()

        // Create bays
        repeat(areaCount) { index ->
            defaultAreas.add(
                AreaTemplateEntity(
                    branchId = branchId,
                    name = "Bay ${index + 1}",
                    type = AreaType.BAY.name,
                    capacity = 100,
                    displayOrder = index,
                    icon = AreaType.BAY.defaultIcon,
                    color = "#4CAF50"
                )
            )
        }

        // Add baby rooms
        defaultAreas.add(
            AreaTemplateEntity(
                branchId = branchId,
                name = "Baby Room 1",
                type = AreaType.BABY_ROOM.name,
                capacity = 100,
                displayOrder = areaCount,
                icon = AreaType.BABY_ROOM.defaultIcon,
                color = "#FFC107"
            )
        )

        defaultAreas.add(
            AreaTemplateEntity(
                branchId = branchId,
                name = "Baby Room 2",
                type = AreaType.BABY_ROOM.name,
                capacity = 100,
                displayOrder = areaCount + 1,
                icon = AreaType.BABY_ROOM.defaultIcon,
                color = "#FFC107"
            )
        )

        // Add balcony
        defaultAreas.add(
            AreaTemplateEntity(
                branchId = branchId,
                name = "Balcony",
                type = AreaType.BALCONY.name,
                capacity = 100,
                displayOrder = areaCount + 2,
                icon = AreaType.BALCONY.defaultIcon,
                color = "#2196F3"
            )
        )

        areaTemplateDao.insertAreas(defaultAreas)
    }
}
