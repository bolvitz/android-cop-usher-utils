package com.cop.app.headcounter.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class ServiceWithDetails(
    @Embedded val service: ServiceEntity,
    @Relation(
        parentColumn = "branchId",
        entityColumn = "id"
    )
    val branch: BranchEntity
)

data class BranchWithAreas(
    @Embedded val branch: BranchEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "branchId"
    )
    val areas: List<AreaTemplateEntity>
)

data class AreaCountWithTemplate(
    @Embedded val areaCount: AreaCountEntity,
    @Relation(
        parentColumn = "areaTemplateId",
        entityColumn = "id"
    )
    val template: AreaTemplateEntity
)

data class ServiceWithAreaCounts(
    @Embedded val service: ServiceEntity,
    @Relation(
        parentColumn = "branchId",
        entityColumn = "id"
    )
    val branch: BranchEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "serviceId",
        entity = AreaCountEntity::class
    )
    val areaCounts: List<AreaCountWithTemplate>
)
