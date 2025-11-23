package com.cop.app.headcounter.data.repository

import com.cop.app.headcounter.data.local.dao.AreaTemplateDao
import com.cop.app.headcounter.data.local.dao.BranchDao
import com.cop.app.headcounter.data.local.dao.ServiceDao
import com.cop.app.headcounter.data.local.entities.AreaTemplateEntity
import com.cop.app.headcounter.data.local.entities.BranchEntity
import com.cop.app.headcounter.data.local.entities.BranchWithAreas
import com.cop.app.headcounter.domain.common.AppError
import com.cop.app.headcounter.domain.common.Result
import com.cop.app.headcounter.domain.common.resultOf
import com.cop.app.headcounter.domain.models.AreaType
import com.cop.app.headcounter.domain.repository.BranchRepository
import com.cop.app.headcounter.domain.validation.DomainValidators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class BranchRepositoryImpl @Inject constructor(
    private val branchDao: BranchDao,
    private val areaTemplateDao: AreaTemplateDao,
    private val serviceDao: ServiceDao
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

    override suspend fun branchNameExists(name: String, excludeBranchId: String?): Boolean {
        val allBranches = branchDao.getAllBranchesWithAreas().first()
        return allBranches.any { branchWithAreas ->
            branchWithAreas.branch.name.equals(name, ignoreCase = true) &&
            branchWithAreas.branch.id != excludeBranchId
        }
    }

    override suspend fun branchCodeExists(code: String, excludeBranchId: String?): Boolean {
        val allBranches = branchDao.getAllBranchesWithAreas().first()
        return allBranches.any { branchWithAreas ->
            branchWithAreas.branch.code.equals(code, ignoreCase = true) &&
            branchWithAreas.branch.id != excludeBranchId
        }
    }

    override suspend fun createBranch(
        name: String,
        location: String,
        code: String,
        contactPerson: String,
        contactEmail: String,
        contactPhone: String,
        color: String
    ): Result<String> {
        // Validate input
        val validationResult = DomainValidators.validateBranchInput(
            name = name,
            location = location,
            code = code,
            contactEmail = contactEmail.takeIf { it.isNotBlank() },
            contactPhone = contactPhone.takeIf { it.isNotBlank() }
        )

        if (validationResult is Result.Error) {
            return validationResult
        }

        // Check for duplicate name
        if (branchNameExists(name)) {
            return Result.Error(
                AppError.AlreadyExists("Branch", "name", name)
            )
        }

        // Check for duplicate code
        if (branchCodeExists(code)) {
            return Result.Error(
                AppError.AlreadyExists("Branch", "code", code)
            )
        }

        // Create branch
        return resultOf {
            val branchId = UUID.randomUUID().toString()
            val branch = BranchEntity(
                id = branchId,
                name = name,
                location = location,
                code = code.uppercase(),
                contactPerson = contactPerson,
                contactEmail = contactEmail,
                contactPhone = contactPhone,
                color = color
            )

            branchDao.insertBranch(branch)
            branchId
        }
    }

    override suspend fun updateBranch(branch: BranchEntity): Result<Unit> {
        // Validate input
        val validationResult = DomainValidators.validateBranchInput(
            name = branch.name,
            location = branch.location,
            code = branch.code,
            contactEmail = branch.contactEmail.takeIf { it.isNotBlank() },
            contactPhone = branch.contactPhone.takeIf { it.isNotBlank() }
        )

        if (validationResult is Result.Error) {
            return validationResult
        }

        // Check for duplicate name
        if (branchNameExists(branch.name, excludeBranchId = branch.id)) {
            return Result.Error(
                AppError.AlreadyExists("Branch", "name", branch.name)
            )
        }

        // Check for duplicate code
        if (branchCodeExists(branch.code, excludeBranchId = branch.id)) {
            return Result.Error(
                AppError.AlreadyExists("Branch", "code", branch.code)
            )
        }

        return resultOf {
            branchDao.updateBranch(branch.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun deleteBranch(branchId: String): Result<Unit> {
        // Check if branch has services
        if (hasServices(branchId)) {
            return Result.Error(
                AppError.HasDependencies(
                    resource = "Branch",
                    dependencyCount = serviceDao.getRecentServicesByBranch(branchId, 999).first().size,
                    dependencyType = "services"
                )
            )
        }

        val branch = branchDao.getBranchById(branchId).first()
            ?: return Result.Error(AppError.NotFound("Branch", branchId))

        return resultOf {
            branchDao.deleteBranch(branch)
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

    override suspend fun hasServices(branchId: String): Boolean {
        val services = serviceDao.getRecentServicesByBranch(branchId, 1).first()
        return services.isNotEmpty()
    }
}
