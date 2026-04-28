package com.miniprojecttracker.domain.model

/**
 * Domain model representing a user in the system.
 * Maps to both Room entity and Firestore document.
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.DEVELOPER,
    val teamId: String = "",
    val points: Int = 0,
    val avatarUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
