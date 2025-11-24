package com.eventmonitor.core.data.local.dao

import androidx.room.*
import com.eventmonitor.core.data.local.entities.EventEntity
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.local.entities.EventWithAreaCounts
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Transaction
    @Query("SELECT * FROM events ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): Flow<List<EventWithDetails>>

    @Transaction
    @Query("SELECT * FROM events WHERE venueId = :venueId ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentEventsByVenue(venueId: String, limit: Int = 50): Flow<List<EventWithDetails>>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :eventId")
    fun getEventById(eventId: String): Flow<EventWithDetails?>

    @Transaction
    @Query("""
        SELECT * FROM events
        WHERE venueId = :venueId
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, createdAt ASC
    """)
    fun getEventsByVenueAndDateRange(
        venueId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM events
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, venueId ASC
    """)
    fun getEventsAcrossVenuesByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>

    @Query("SELECT * FROM events WHERE venueId = :venueId AND eventType = :eventType ORDER BY date DESC LIMIT 1")
    fun getLastEventOfType(venueId: String, eventType: String): Flow<EventEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("""
        SELECT AVG(totalAttendance)
        FROM events
        WHERE venueId = :venueId
        AND date BETWEEN :startDate AND :endDate
    """)
    fun getAverageAttendance(
        venueId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    @Transaction
    @Query("SELECT * FROM events ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentEventsWithAreaCounts(limit: Int = 50): Flow<List<EventWithAreaCounts>>

    @Transaction
    @Query("""
        SELECT * FROM events
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, createdAt DESC
    """)
    fun getEventsWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithAreaCounts>>

    @Query("SELECT EXISTS(SELECT 1 FROM events WHERE eventTypeId = :eventTypeId LIMIT 1)")
    suspend fun hasEventsWithType(eventTypeId: String): Boolean
}
