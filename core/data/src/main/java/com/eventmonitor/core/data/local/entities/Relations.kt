package com.eventmonitor.core.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class EventWithDetails(
    @Embedded val event: EventEntity,
    @Relation(
        parentColumn = "venueId",
        entityColumn = "id"
    )
    val venue: VenueEntity,
    @Relation(
        parentColumn = "eventTypeId",
        entityColumn = "id"
    )
    val eventType: EventTypeEntity?
)

data class VenueWithAreas(
    @Embedded val venue: VenueEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "venueId"
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
        parentColumn = "venueId",
        entityColumn = "id"
    )
    val venue: VenueEntity,
    @Relation(
        parentColumn = "eventTypeId",
        entityColumn = "id"
    )
    val eventType: EventTypeEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "eventId",
        entity = AreaCountEntity::class
    )
    val areaCounts: List<AreaCountWithTemplate>
)
