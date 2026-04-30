package com.miniprojecttracker.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniprojecttracker.data.repository.AuthRepository
import com.miniprojecttracker.data.repository.TeamRepository
import com.miniprojecttracker.data.repository.UserRepository
import com.miniprojecttracker.data.repository.TaskRepository
import com.miniprojecttracker.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUserFlow().collectLatest { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun loadAllTeams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Sync before loading
            try {
                teamRepository.syncTeams()
            } catch (_: Exception) {}

            authRepository.getCurrentUserFlow().collectLatest { currentUser ->
                if (currentUser != null) {
                    val teamsFlow = when (currentUser.role) {
                        UserRole.MANAGER -> teamRepository.getTeamsByManager(currentUser.id)
                        UserRole.TEAM_LEADER -> teamRepository.getAllTeams().map { teams ->
                            teams.filter { it.leaderId == currentUser.id }
                        }
                        UserRole.DEVELOPER -> teamRepository.getAllTeams().map { teams ->
                            teams.filter { it.memberIds.contains(currentUser.id) }
                        }
                    }

                    teamsFlow.collectLatest { teams ->
                        // Hide memberIds for other team leaders or developers if not in that team
                        // Actually, filtering them out of the list is handled above.
                        // But if we want to hide member details for "other" teams:
                        val filteredTeams = teams.map { team ->
                            if (currentUser.role == UserRole.TEAM_LEADER && team.leaderId != currentUser.id) {
                                team.copy(memberIds = emptyList())
                            } else team
                        }
                        _uiState.update { it.copy(isLoading = false, teams = filteredTeams) }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun loadTeam(teamId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            teamRepository.getTeamById(teamId).collectLatest { team ->
                _uiState.update { it.copy(isLoading = false, currentTeam = team) }
            }
        }
    }

    fun loadAvailableTeamLeaders() {
        viewModelScope.launch {
            userRepository.getUsersByRole(UserRole.TEAM_LEADER).collectLatest { leaders ->
                _uiState.update { it.copy(availableTeamLeaders = leaders) }
            }
        }
    }

    fun loadAvailableDevelopers(currentTeamId: String? = null) {
        viewModelScope.launch {
            userRepository.getUsersByRole(UserRole.DEVELOPER).collectLatest { devs ->
                // Show developers who are unassigned OR already in this team
                val targetTeamId = currentTeamId ?: _uiState.value.currentTeam?.id ?: ""
                val availableDevs = devs.filter { 
                    it.teamId.isBlank() || it.teamId == "none" || it.teamId == targetTeamId 
                }
                _uiState.update { it.copy(availableDevelopers = availableDevs) }
            }
        }
    }

    fun saveTeam(team: Team, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val oldTeam = _uiState.value.currentTeam
                val removedMemberIds = oldTeam?.memberIds?.filter { !team.memberIds.contains(it) } ?: emptyList()

                // Check if removed members have pending tasks
                for (memberId in removedMemberIds) {
                    val tasks = taskRepository.getTasksByUser(memberId).first()
                    val hasPendingTasks = tasks.any { it.status != TaskStatus.DONE }
                    if (hasPendingTasks) {
                        val memberName = userRepository.getUserById(memberId).firstOrNull()?.name ?: "Member"
                        _uiState.update { it.copy(isLoading = false, error = "Cannot remove $memberName: they have incomplete tasks.") }
                        return@launch
                    }
                }

                val savedTeamId = if (team.id.isBlank()) {
                    val newTeam = teamRepository.createTeam(team)
                    userRepository.updateUserTeam(newTeam.leaderId, newTeam.id)
                    newTeam.id
                } else {
                    teamRepository.updateTeam(team)
                    userRepository.updateUserTeam(team.leaderId, team.id)
                    team.id
                }

                // Update all current members' teamId
                team.memberIds.forEach { memberId ->
                    userRepository.updateUserTeam(memberId, savedTeamId)
                }

                // Reset teamId for removed members
                removedMemberIds.forEach { memberId ->
                    userRepository.updateUserTeam(memberId, "")
                }
                
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                 _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save team") }
            }
        }
    }

    fun deleteTeam(team: Team, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                teamRepository.deleteTeam(team)
                 _uiState.update { it.copy(isLoading = false) }
                 onSuccess()
            } catch (e: Exception) {
                 _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to delete team") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getCurrentUserId(): String? = authRepository.currentUserId
}

data class TeamUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val teams: List<Team> = emptyList(),
    val currentTeam: Team? = null,
    val availableTeamLeaders: List<User> = emptyList(),
    val availableDevelopers: List<User> = emptyList(),
    val currentUser: User? = null
)
