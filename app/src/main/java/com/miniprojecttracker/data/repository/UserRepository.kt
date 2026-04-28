package com.miniprojecttracker.data.repository

import com.miniprojecttracker.data.local.dao.UserDao
import com.miniprojecttracker.data.local.entity.UserEntity
import com.miniprojecttracker.data.remote.FirestoreDataSource
import com.miniprojecttracker.domain.model.User
import com.miniprojecttracker.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user data operations.
 * Syncs between Firestore (source of truth) and Room (local cache).
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val firestoreDataSource: FirestoreDataSource
) {
    fun getAllUsers(): Flow<List<User>> =
        userDao.getAllUsers().map { list -> list.map { it.toDomain() } }

    fun getUserById(userId: String): Flow<User?> =
        userDao.getUserById(userId).map { it?.toDomain() }

    fun getUsersByRole(role: UserRole): Flow<List<User>> =
        userDao.getUsersByRole(role.name).map { list -> list.map { it.toDomain() } }

    fun getUsersByTeam(teamId: String): Flow<List<User>> =
        userDao.getUsersByTeam(teamId).map { list -> list.map { it.toDomain() } }

    fun getLeaderboard(): Flow<List<User>> =
        userDao.getUsersByPoints().map { list -> list.map { it.toDomain() } }

    suspend fun updateUser(user: User) {
        firestoreDataSource.updateUser(user)
        userDao.updateUser(UserEntity.fromDomain(user))
    }

    suspend fun addPoints(userId: String, points: Int) {
        firestoreDataSource.addPointsToUser(userId, points)
        userDao.addPoints(userId, points)
    }

    suspend fun resetMonthlyLeaderboard() {
        firestoreDataSource.resetAllDeveloperPoints()
        userDao.resetAllPoints()
    }

    suspend fun updateUserTeam(userId: String, teamId: String) {
        val user = getUserById(userId).firstOrNull()
        if (user != null) {
            val updatedUser = user.copy(teamId = teamId)
            updateUser(updatedUser)
        }
    }

    /**
     * Sync all users from Firestore to local Room cache.
     * Called on app startup or pull-to-refresh.
     */
    suspend fun syncUsers() {
        try {
            val users = firestoreDataSource.observeUsers().first()
            userDao.insertUsers(users.map { UserEntity.fromDomain(it) })
        } catch (_: Exception) { /* Offline mode - use cached data */ }
    }
}
