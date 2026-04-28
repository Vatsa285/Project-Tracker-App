package com.miniprojecttracker.data.repository

import com.miniprojecttracker.data.local.dao.TeamDao
import com.miniprojecttracker.data.local.entity.TeamEntity
import com.miniprojecttracker.data.remote.FirestoreDataSource
import com.miniprojecttracker.domain.model.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository @Inject constructor(
    private val teamDao: TeamDao,
    private val firestoreDataSource: FirestoreDataSource
) {
    fun getAllTeams(): Flow<List<Team>> =
        teamDao.getAllTeams().map { list -> list.map { it.toDomain() } }

    fun getTeamById(teamId: String): Flow<Team?> =
        teamDao.getTeamById(teamId).map { it?.toDomain() }

    fun getTeamsByManager(managerId: String): Flow<List<Team>> =
        teamDao.getTeamsByManager(managerId).map { list -> list.map { it.toDomain() } }

    suspend fun createTeam(team: Team): Team {
        val newTeam = team.copy(id = if (team.id.isBlank()) UUID.randomUUID().toString() else team.id)
        firestoreDataSource.createTeam(newTeam)
        teamDao.insertTeam(TeamEntity.fromDomain(newTeam))
        return newTeam
    }

    suspend fun updateTeam(team: Team) {
        firestoreDataSource.updateTeam(team)
        teamDao.updateTeam(TeamEntity.fromDomain(team))
    }

    suspend fun deleteTeam(team: Team) {
        firestoreDataSource.deleteTeam(team.id)
        teamDao.deleteTeam(TeamEntity.fromDomain(team))
    }

    suspend fun syncTeams() {
        try {
            // Get current user to filter if necessary, but syncing all for now
            // To isolate data, we should only sync what belongs to the user
            val teams = firestoreDataSource.observeTeams().first()
            teamDao.insertTeams(teams.map { TeamEntity.fromDomain(it) })
        } catch (_: Exception) {}
    }
}
