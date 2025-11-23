package com.cop.app.headcounter.data.local.dao

import androidx.room.*
import com.cop.app.headcounter.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE firebaseUid = :firebaseUid")
    fun getUserByFirebaseUid(firebaseUid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET lastSyncTime = :syncTime WHERE id = :userId")
    suspend fun updateLastSyncTime(userId: String, syncTime: Long)
}
