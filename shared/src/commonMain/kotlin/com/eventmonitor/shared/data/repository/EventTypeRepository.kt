package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.models.EventTypeDto
import com.eventmonitor.shared.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface EventTypeRepository {
    fun getAllEventTypes(): Flow<List<EventTypeDto>>
    fun getActiveEventTypes(): Flow<List<EventTypeDto>>
    fun getEventTypeById(id: String): Flow<EventTypeDto?>

    suspend fun createEventType(eventType: EventTypeDto): Result<String>
    suspend fun updateEventType(eventType: EventTypeDto): Result<Unit>
    suspend fun deleteEventType(eventTypeId: String): Result<Unit>
}
