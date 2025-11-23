package com.eventmonitor.core.data.repository.interfaces

import com.eventmonitor.core.data.local.entities.AreaCountEntity
import com.eventmonitor.core.data.local.entities.AreaCountWithTemplate
import kotlinx.coroutines.flow.Flow

interface AreaCountRepository {
    fun getAreaCountsByService(eventId: String): Flow<List<AreaCountWithTemplate>>
    suspend fun getAreaCountById(areaCountId: String): AreaCountEntity?
    fun getAreaCountByIdFlow(areaCountId: String): Flow<AreaCountEntity?>
    fun getAreaCountsByTemplateId(areaTemplateId: String): Flow<List<AreaCountEntity>>
    suspend fun insertAreaCount(areaCount: AreaCountEntity)
    suspend fun insertAreaCounts(areaCounts: List<AreaCountEntity>)
    suspend fun updateAreaCount(areaCount: AreaCountEntity)
    suspend fun deleteAreaCountsByService(eventId: String)
}
