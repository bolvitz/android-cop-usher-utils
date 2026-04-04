package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.models.AreaCountDto
import com.eventmonitor.shared.data.models.AreaCountWithTemplate
import com.eventmonitor.shared.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface AreaCountRepository {
    fun getAreaCountsByEvent(eventId: String): Flow<List<AreaCountWithTemplate>>
    fun getAreaCountById(areaCountId: String): Flow<AreaCountDto?>

    suspend fun createAreaCount(areaCount: AreaCountDto): Result<String>
    suspend fun updateAreaCount(areaCount: AreaCountDto): Result<Unit>

    suspend fun incrementCount(
        eventId: String,
        areaCountId: String,
        amount: Int = 1,
        action: String = "INCREMENT"
    ): Result<Unit>

    suspend fun decrementCount(
        eventId: String,
        areaCountId: String,
        amount: Int = 1,
        action: String = "DECREMENT"
    ): Result<Unit>

    suspend fun updateCount(
        eventId: String,
        areaCountId: String,
        newCount: Int,
        action: String = "MANUAL_EDIT"
    ): Result<Unit>

    suspend fun resetCount(eventId: String, areaCountId: String): Result<Unit>
    suspend fun deleteAreaCount(areaCountId: String): Result<Unit>
}
