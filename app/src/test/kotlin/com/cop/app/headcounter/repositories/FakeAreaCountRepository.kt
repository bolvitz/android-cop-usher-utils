package com.cop.app.headcounter.repositories

import com.cop.app.headcounter.data.local.entities.AreaCountEntity
import com.cop.app.headcounter.data.local.entities.AreaCountWithTemplate
import com.cop.app.headcounter.domain.repository.AreaCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAreaCountRepository : AreaCountRepository {
    private val areaCounts = MutableStateFlow<Map<String, AreaCountEntity>>(emptyMap())
    private val areaCountsWithTemplates = MutableStateFlow<List<AreaCountWithTemplate>>(emptyList())

    fun setAreaCountsWithTemplates(counts: List<AreaCountWithTemplate>) {
        areaCountsWithTemplates.value = counts
    }

    override fun getAreaCountsByService(serviceId: String): Flow<List<AreaCountWithTemplate>> {
        return areaCountsWithTemplates.map { list ->
            list.filter { it.areaCount.serviceId == serviceId }
        }
    }

    override suspend fun getAreaCountById(areaCountId: String): AreaCountEntity? {
        return areaCounts.value[areaCountId]
    }

    override fun getAreaCountByIdFlow(areaCountId: String): Flow<AreaCountEntity?> {
        return areaCounts.map { it[areaCountId] }
    }

    override fun getAreaCountsByTemplateId(areaTemplateId: String): Flow<List<AreaCountEntity>> {
        return areaCounts.map { counts ->
            counts.values.filter { it.areaTemplateId == areaTemplateId }
        }
    }

    override suspend fun insertAreaCount(areaCount: AreaCountEntity) {
        areaCounts.value = areaCounts.value + (areaCount.id to areaCount)
    }

    override suspend fun insertAreaCounts(areaCounts: List<AreaCountEntity>) {
        this.areaCounts.value = this.areaCounts.value + areaCounts.associateBy { it.id }
    }

    override suspend fun updateAreaCount(areaCount: AreaCountEntity) {
        areaCounts.value = areaCounts.value + (areaCount.id to areaCount)
    }

    override suspend fun deleteAreaCountsByService(serviceId: String) {
        areaCounts.value = areaCounts.value.filterValues { it.serviceId != serviceId }
    }
}
