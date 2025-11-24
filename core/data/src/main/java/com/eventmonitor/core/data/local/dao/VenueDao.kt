package com.eventmonitor.core.data.local.dao

import androidx.room.*
import com.eventmonitor.core.data.local.entities.VenueEntity
import com.eventmonitor.core.data.local.entities.VenueWithAreas
import kotlinx.coroutines.flow.Flow

@Dao
interface VenueDao {
    @Query("SELECT * FROM venues WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveVenues(): Flow<List<VenueEntity>>

    @Query("SELECT * FROM venues ORDER BY name ASC")
    fun getAllVenues(): Flow<List<VenueEntity>>

    @Query("SELECT * FROM venues WHERE id = :venueId")
    fun getVenueById(venueId: String): Flow<VenueEntity?>

    @Transaction
    @Query("SELECT * FROM venues WHERE id = :venueId")
    fun getVenueWithAreas(venueId: String): Flow<VenueWithAreas?>

    @Transaction
    @Query("SELECT * FROM venues WHERE isActive = 1 ORDER BY name ASC")
    fun getAllVenuesWithAreas(): Flow<List<VenueWithAreas>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVenue(venue: VenueEntity): Long

    @Update
    suspend fun updateVenue(venue: VenueEntity)

    @Delete
    suspend fun deleteVenue(venue: VenueEntity)

    @Query("UPDATE venues SET isActive = :isActive WHERE id = :venueId")
    suspend fun setVenueActive(venueId: String, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM venues WHERE isActive = 1")
    fun getActiveVenueCount(): Flow<Int>
}
