package com.miniprojecttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miniprojecttracker.domain.model.User
import com.miniprojecttracker.domain.model.UserRole

/**
 * Room entity for users. Mirrors Firestore user documents.
 * Uses Firebase Auth UID as primary key for consistency.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String,        // Stored as string, converted to UserRole enum
    val teamId: String,
    val points: Int,
    val avatarUrl: String,
    val createdAt: Long
) {
    fun toDomain(): User = User(
        id = id,
        name = name,
        email = email,
        role = try { UserRole.valueOf(role) } catch (e: Exception) { UserRole.DEVELOPER },
        teamId = teamId,
        points = points,
        avatarUrl = avatarUrl,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            role = user.role.name,
            teamId = user.teamId,
            points = user.points,
            avatarUrl = user.avatarUrl,
            createdAt = user.createdAt
        )
    }
}
