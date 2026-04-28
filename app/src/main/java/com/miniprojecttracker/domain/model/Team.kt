package com.miniprojecttracker.domain.model

/**
 * Domain model representing a team.
 * Each team has one manager and can have multiple developers.
 */
data class Team(
    val id: String = "",
    val name: String = "",
    val managerId: String = "", // The MANAGER (Owner) who created it
    val leaderId: String = "",  // The TEAM_LEADER assigned to lead it
    val description: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
