package com.eventmonitor.core.data.local.dao

import androidx.room.*
import com.eventmonitor.core.data.local.entities.AreaTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaTemplateDao {
    @Query("SELECT * FROM area_templates WHERE branchId = :branchId AND isActive = 1 ORDER BY name ASC")
    fun getAreasByBranch(branchId: String): Flow<List<AreaTemplateEntity>>

    @Query("SELECT * FROM area_templates WHERE id = :areaId")
    fun getAreaById(areaId: String): Flow<AreaTemplateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: AreaTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreas(areas: List<AreaTemplateEntity>)

    @Update
    suspend fun updateArea(area: AreaTemplateEntity)

    @Delete
    suspend fun deleteArea(area: AreaTemplateEntity)

    @Query("UPDATE area_templates SET isActive = :isActive WHERE id = :areaId")
    suspend fun setAreaActive(areaId: String, isActive: Boolean)

    @Query("UPDATE area_templates SET displayOrder = :order WHERE id = :areaId")
    suspend fun updateDisplayOrder(areaId: String, order: Int)

    @Query("SELECT SUM(capacity) FROM area_templates WHERE branchId = :branchId AND isActive = 1")
    fun getTotalCapacityForBranch(branchId: String): Flow<Int?>

    @Query("DELETE FROM area_templates WHERE branchId = :branchId")
    suspend fun deleteAllAreasForBranch(branchId: String)
}
