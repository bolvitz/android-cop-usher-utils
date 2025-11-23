package com.eventmonitor.core.data.repository

import com.eventmonitor.core.data.local.dao.AreaCountDao
import com.eventmonitor.core.data.local.entities.AreaCountEntity
import com.eventmonitor.core.data.local.entities.AreaCountWithTemplate
import com.eventmonitor.core.data.repository.interfaces.AreaCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AreaCountRepositoryImpl @Inject constructor(
    private val areaCountDao: AreaCountDao
) : AreaCountRepository {

    override fun getAreaCountsByService(eventId: String): Flow<List<AreaCountWithTemplate>> {
        return areaCountDao.getAreaCountsByService(eventId)
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

    override suspend fun deleteAreaCountsByService(eventId: String) {
        areaCountDao.deleteAreaCountsByService(eventId)
    }
}
