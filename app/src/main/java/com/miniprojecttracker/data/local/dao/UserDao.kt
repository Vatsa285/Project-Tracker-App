package com.miniprojecttracker.data.local.dao

import androidx.room.*
import com.miniprojecttracker.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user operations.
 * All queries return Flow for reactive UI updates.
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserByIdOnce(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role ORDER BY name ASC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE teamId = :teamId ORDER BY name ASC")
    fun getUsersByTeam(teamId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY points DESC")
    fun getUsersByPoints(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("UPDATE users SET points = points + :points WHERE id = :userId")
    suspend fun addPoints(userId: String, points: Int)

    @Query("UPDATE users SET points = 0")
    suspend fun resetAllPoints()

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
