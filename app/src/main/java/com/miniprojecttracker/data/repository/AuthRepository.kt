package com.miniprojecttracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.miniprojecttracker.data.local.dao.UserDao
import com.miniprojecttracker.data.local.entity.UserEntity
import com.miniprojecttracker.data.remote.FirestoreDataSource
import com.miniprojecttracker.domain.model.User
import com.miniprojecttracker.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Firebase Auth operations and user profile sync.
 * Login/signup create Firebase Auth accounts, then stores
 * extended profile data (role, team, points) in Firestore + Room.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestoreDataSource: FirestoreDataSource,
    private val userDao: UserDao
) {
    val currentUserId: String? get() = auth.currentUser?.uid

    val isLoggedIn: Boolean get() = auth.currentUser != null

    fun getCurrentUserFlow(): Flow<User?> {
        val uid = currentUserId ?: return kotlinx.coroutines.flow.flowOf(null)
        return userDao.getUserById(uid).map { it?.toDomain() }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Login failed")
            
            // First, check local DB
            var user = userDao.getUserById(uid).firstOrNull()?.toDomain()
            
            // If not in local, fetch from Firestore
            if (user == null) {
                user = firestoreDataSource.getUser(uid) ?: throw Exception("User profile not found in Firestore")
                userDao.insertUser(UserEntity.fromDomain(user))
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(name: String, email: String, password: String, role: UserRole): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Signup failed")
            val user = User(
                id = uid,
                name = name,
                email = email,
                role = role,
                createdAt = System.currentTimeMillis()
            )
            // Save profile to Firestore and local cache
            firestoreDataSource.createUser(user)
            userDao.insertUser(UserEntity.fromDomain(user))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("Not logged in")
            val email = user.email ?: throw Exception("User email not found")
            
            // Re-authenticate
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            
            // Update password
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
