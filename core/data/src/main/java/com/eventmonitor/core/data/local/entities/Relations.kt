package com.eventmonitor.core.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class EventWithDetails(
    @Embedded val event: EventEntity,
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

data class EventWithAreaCounts(
    @Embedded val event: EventEntity,
    @Relation(
        parentColumn = "branchId",
        entityColumn = "id"
    )
    val branch: BranchEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "eventId",
        entity = AreaCountEntity::class
    )
    val areaCounts: List<AreaCountWithTemplate>
)
