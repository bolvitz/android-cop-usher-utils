package com.cop.app.headcounter.data.local.dao

import androidx.room.*
import com.cop.app.headcounter.data.local.entities.AreaCountEntity
import com.cop.app.headcounter.data.local.entities.AreaCountWithTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaCountDao {
    @Transaction
    @Query("SELECT * FROM area_counts WHERE serviceId = :serviceId")
    fun getAreaCountsByService(serviceId: String): Flow<List<AreaCountWithTemplate>>

    @Query("SELECT * FROM area_counts WHERE id = :areaCountId")
    fun getAreaCountById(areaCountId: String): Flow<AreaCountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreaCount(areaCount: AreaCountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreaCounts(areaCounts: List<AreaCountEntity>)

    @Update
    suspend fun updateAreaCount(areaCount: AreaCountEntity)

    @Query("DELETE FROM area_counts WHERE serviceId = :serviceId")
    suspend fun deleteAreaCountsByService(serviceId: String)
}
