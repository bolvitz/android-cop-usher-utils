package com.eventmonitor.core.data.local.dao

import androidx.room.*
import com.eventmonitor.core.data.local.entities.AreaCountEntity
import com.eventmonitor.core.data.local.entities.AreaCountWithTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaCountDao {
    @Transaction
    @Query("""
        SELECT ac.* FROM area_counts ac
        INNER JOIN area_templates at ON ac.areaTemplateId = at.id
        WHERE ac.eventId = :eventId
        ORDER BY at.name ASC
    """)
    fun getAreaCountsByService(eventId: String): Flow<List<AreaCountWithTemplate>>

    @Query("SELECT * FROM area_counts WHERE id = :areaCountId")
    fun getAreaCountById(areaCountId: String): Flow<AreaCountEntity?>

    @Query("SELECT * FROM area_counts WHERE areaTemplateId = :areaTemplateId")
    fun getAreaCountsByTemplateId(areaTemplateId: String): Flow<List<AreaCountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreaCount(areaCount: AreaCountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreaCounts(areaCounts: List<AreaCountEntity>)

    @Update
    suspend fun updateAreaCount(areaCount: AreaCountEntity)

    @Query("DELETE FROM area_counts WHERE eventId = :eventId")
    suspend fun deleteAreaCountsByService(eventId: String)
}
