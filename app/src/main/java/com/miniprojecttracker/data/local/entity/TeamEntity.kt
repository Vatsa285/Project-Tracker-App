package com.miniprojecttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miniprojecttracker.domain.model.Team

/**
 * Room entity for teams.
 * memberIds stored as comma-separated string for Room compatibility.
 */
@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val id: String,
    val name: String,
    val managerId: String,
    val leaderId: String,
    val description: String,
    val memberIds: String,   // Comma-separated list of user IDs
    val createdAt: Long
) {
    fun toDomain(): Team = Team(
        id = id,
        name = name,
        managerId = managerId,
        leaderId = leaderId,
        description = description,
        memberIds = if (memberIds.isBlank()) emptyList() else memberIds.split(","),
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(team: Team): TeamEntity = TeamEntity(
            id = team.id,
            name = team.name,
            managerId = team.managerId,
            leaderId = team.leaderId,
            description = team.description,
            memberIds = team.memberIds.joinToString(","),
            createdAt = team.createdAt
        )
    }
}
