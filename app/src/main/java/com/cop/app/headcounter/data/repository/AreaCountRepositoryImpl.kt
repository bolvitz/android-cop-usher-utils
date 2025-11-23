package com.cop.app.headcounter.data.repository

import com.cop.app.headcounter.data.local.dao.AreaCountDao
import com.cop.app.headcounter.data.local.entities.AreaCountEntity
import com.cop.app.headcounter.data.local.entities.AreaCountWithTemplate
import com.cop.app.headcounter.domain.repository.AreaCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AreaCountRepositoryImpl @Inject constructor(
    private val areaCountDao: AreaCountDao
) : AreaCountRepository {

    override fun getAreaCountsByService(serviceId: String): Flow<List<AreaCountWithTemplate>> {
        return areaCountDao.getAreaCountsByService(serviceId)
    }

    override suspend fun getAreaCountById(areaCountId: String): AreaCountEntity? {
        return areaCountDao.getAreaCountById(areaCountId).first()
    }

    override fun getAreaCountByIdFlow(areaCountId: String): Flow<AreaCountEntity?> {
        return areaCountDao.getAreaCountById(areaCountId)
    }

    override fun getAreaCountsByTemplateId(areaTemplateId: String): Flow<List<AreaCountEntity>> {
        return areaCountDao.getAreaCountsByTemplateId(areaTemplateId)
    }

    override suspend fun insertAreaCount(areaCount: AreaCountEntity) {
        areaCountDao.insertAreaCount(areaCount)
    }

    override suspend fun insertAreaCounts(areaCounts: List<AreaCountEntity>) {
        areaCountDao.insertAreaCounts(areaCounts)
    }

    override suspend fun updateAreaCount(areaCount: AreaCountEntity) {
        areaCountDao.updateAreaCount(areaCount)
    }

    override suspend fun deleteAreaCountsByService(serviceId: String) {
        areaCountDao.deleteAreaCountsByService(serviceId)
    }
}
