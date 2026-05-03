package com.miniprojecttracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniprojecttracker.data.repository.ProjectRepository
import com.miniprojecttracker.data.repository.TaskRepository
import com.miniprojecttracker.data.repository.TeamRepository
import com.miniprojecttracker.data.repository.UserRepository
import com.miniprojecttracker.data.repository.AuthRepository
import com.miniprojecttracker.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val currentUser = authRepository.getCurrentUserFlow().filterNotNull().first()
            _uiState.update { it.copy(currentUser = currentUser) }

            // Sync with Firebase in parallel
            viewModelScope.launch {
                try {
                    userRepository.syncUsers()
                } catch (_: Exception) {}
            }
            viewModelScope.launch {
                try {
                    teamRepository.syncTeams(currentUser.id, currentUser.role)
                } catch (_: Exception) {}
            }
            viewModelScope.launch {
                try {
                    projectRepository.syncProjects(currentUser.id, currentUser.role)
                } catch (_: Exception) {}
            }
            viewModelScope.launch {
                try {
                    taskRepository.syncTasks(currentUser.id, currentUser.role)
                } catch (_: Exception) {}
            }

            when (currentUser.role) {
                UserRole.MANAGER -> loadManagerData()
                UserRole.TEAM_LEADER -> loadTeamLeaderData(currentUser.id)
                UserRole.DEVELOPER -> loadDeveloperData(currentUser.id)
            }
        }
    }

    private fun loadManagerData() {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUser?.id ?: return@launch
            combine(
                teamRepository.getTeamsByManager(currentUserId),
                projectRepository.getProjectsByManager(currentUserId),
                userRepository.getAllUsers()
            ) { teams, projects, users ->
                val projectStatusCounts = projects.groupingBy { it.status.name }.eachCount()
                val activeProjects = projects.filter { 
                    it.status != ProjectStatus.COMPLETED && it.status != ProjectStatus.NOT_STARTED 
                }
                val upcomingProjects = projects.filter { it.status == ProjectStatus.NOT_STARTED }
                val completedProjectsCount = projects.count { it.status == ProjectStatus.COMPLETED }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        teams = teams,
                        projects = projects,
                        activeProjects = activeProjects,
                        upcomingProjects = upcomingProjects,
                        totalTeams = teams.size,
                        totalProjects = projects.size,
                        completedProjectsCount = completedProjectsCount,
                        totalUsers = users.size,
                        projectStatusCounts = projectStatusCounts,
                        activeProjectsCount = activeProjects.size,
                        upcomingProjectsCount = upcomingProjects.size
                    )
                }
            }.collect()
        }
    }

    private var selectTeamJob: kotlinx.coroutines.Job? = null

    private fun loadTeamLeaderData(userId: String) {
        viewModelScope.launch {
            // First, load all projects across all led teams for accurate total counts
            combine(
                teamRepository.getAllTeams().map { teams -> teams.filter { it.leaderId == userId } },
                projectRepository.getAllProjects()
            ) { managedTeams, allProjects ->
                val teamIds = managedTeams.map { it.id }.toSet()
                val teamProjects = allProjects.filter { it.teamId in teamIds }
                managedTeams to teamProjects
            }.collectLatest { (managedTeams, allTeamProjects) ->
                _uiState.update { it.copy(
                    teams = managedTeams, 
                    totalManagedTeams = managedTeams.size,
                    totalProjects = allTeamProjects.size
                ) }
                
                if (managedTeams.isNotEmpty()) {
                    val initialTeamId = _uiState.value.selectedTeamId.ifBlank { managedTeams.first().id }
                    selectTeam(initialTeamId)
                } else {
                    _uiState.update { it.copy(isLoading = false, projects = emptyList()) }
                }
            }
        }
    }

    fun selectTeam(teamId: String) {
        _uiState.update { it.copy(selectedTeamId = teamId, isLoading = true) }
        // Cancel any previous team-selection collector to avoid duplicates
        selectTeamJob?.cancel()
        selectTeamJob = viewModelScope.launch {
            combine(
                teamRepository.getTeamById(teamId),
                projectRepository.getProjectsByTeam(teamId)
            ) { team, projects ->
                val activeProjects = projects.filter { 
                    it.status != ProjectStatus.COMPLETED && it.status != ProjectStatus.NOT_STARTED 
                }
                val upcomingProjects = projects.filter { it.status == ProjectStatus.NOT_STARTED }
                val completedProjects = projects.filter { it.status == ProjectStatus.COMPLETED }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        projects = projects,
                        activeProjects = activeProjects,
                        upcomingProjects = upcomingProjects,
                        activeProjectsCount = activeProjects.size,
                        completedProjectsCount = completedProjects.size,
                        upcomingProjectsCount = upcomingProjects.size
                    ) 
                }
            }.collect()
        }
    }

    private fun loadDeveloperData(userId: String) {
        viewModelScope.launch {
            val tasksFlow = taskRepository.getTasksByUser(userId)
            val overdueTasksFlow = taskRepository.getOverdueTasks().map { tasks -> tasks.filter { it.assignedTo == userId } }
            
            combine(tasksFlow, overdueTasksFlow) { allTasks, overdueTasks ->
                val pendingTasks = allTasks.filter { it.status != TaskStatus.DONE }
                val completedCount = allTasks.size - pendingTasks.size
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        tasks = allTasks,
                        pendingTasksCount = pendingTasks.size,
                        completedTasksCount = completedCount,
                        overdueTasksCount = overdueTasks.size
                    ) 
                }
            }.collect()
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: User? = null,
    val selectedTeamId: String = "",
    
    // Manager Data (formerly SUPER_MANAGER)
    val totalTeams: Int = 0,
    val totalProjects: Int = 0,
    val completedProjectsCount: Int = 0,
    val totalUsers: Int = 0,
    val projectStatusCounts: Map<String, Int> = emptyMap(),
    
    // Team Leader Data (formerly MANAGER)
    val teams: List<Team> = emptyList(),
    val totalManagedTeams: Int = 0,
    val projects: List<Project> = emptyList(),
    val activeProjects: List<Project> = emptyList(),
    val upcomingProjects: List<Project> = emptyList(),
    val activeProjectsCount: Int = 0,
    val upcomingProjectsCount: Int = 0,
    
    // Developer Data
    val tasks: List<Task> = emptyList(),
    val pendingTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val overdueTasksCount: Int = 0
)
