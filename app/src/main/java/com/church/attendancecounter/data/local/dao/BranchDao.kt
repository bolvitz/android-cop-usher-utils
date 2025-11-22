package com.church.attendancecounter.data.local.dao

import androidx.room.*
import com.church.attendancecounter.data.local.entities.BranchEntity
import com.church.attendancecounter.data.local.entities.BranchWithAreas
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches ORDER BY name ASC")
    fun getAllBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE id = :branchId")
    fun getBranchById(branchId: String): Flow<BranchEntity?>

    @Transaction
    @Query("SELECT * FROM branches WHERE id = :branchId")
    fun getBranchWithAreas(branchId: String): Flow<BranchWithAreas?>

    @Transaction
    @Query("SELECT * FROM branches WHERE isActive = 1 ORDER BY name ASC")
    fun getAllBranchesWithAreas(): Flow<List<BranchWithAreas>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity): Long

    @Update
    suspend fun updateBranch(branch: BranchEntity)

    @Delete
    suspend fun deleteBranch(branch: BranchEntity)

    @Query("UPDATE branches SET isActive = :isActive WHERE id = :branchId")
    suspend fun setBranchActive(branchId: String, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM branches WHERE isActive = 1")
    fun getActiveBranchCount(): Flow<Int>
}
