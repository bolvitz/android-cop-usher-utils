package com.cop.app.headcounter.domain.repository

import com.cop.app.headcounter.data.local.entities.AreaCountEntity
import com.cop.app.headcounter.data.local.entities.AreaCountWithTemplate
import kotlinx.coroutines.flow.Flow

interface AreaCountRepository {
    fun getAreaCountsByService(serviceId: String): Flow<List<AreaCountWithTemplate>>
    suspend fun getAreaCountById(areaCountId: String): AreaCountEntity?
    fun getAreaCountByIdFlow(areaCountId: String): Flow<AreaCountEntity?>
    fun getAreaCountsByTemplateId(areaTemplateId: String): Flow<List<AreaCountEntity>>
    suspend fun insertAreaCount(areaCount: AreaCountEntity)
    suspend fun insertAreaCounts(areaCounts: List<AreaCountEntity>)
    suspend fun updateAreaCount(areaCount: AreaCountEntity)
    suspend fun deleteAreaCountsByService(serviceId: String)
}
