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
            
            // Sync with Firebase in parallel
            viewModelScope.launch {
                try {
                    userRepository.syncUsers()
                } catch (_: Exception) {}
            }
            viewModelScope.launch {
                try {
                    teamRepository.syncTeams()
                } catch (_: Exception) {}
            }
            viewModelScope.launch {
                try {
                    projectRepository.syncProjects()
                } catch (_: Exception) {}
            }
            viewModelScope.launch {
                try {
                    taskRepository.syncTasks()
                } catch (_: Exception) {}
            }

            authRepository.getCurrentUserFlow().collectLatest { user ->
                if (user != null) {
                    _uiState.update { it.copy(currentUser = user) }
                    
                    when (user.role) {
                        UserRole.MANAGER -> loadManagerData()
                        UserRole.TEAM_LEADER -> loadTeamLeaderData(user.id)
                        UserRole.DEVELOPER -> loadDeveloperData(user.id)
                    }
                } else {
                     _uiState.update { it.copy(isLoading = false, error = "User not found") }
                }
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
                val activeProjects = projects.filter { it.status == ProjectStatus.ACTIVE }
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

    private fun loadTeamLeaderData(userId: String) {
        viewModelScope.launch {
            // Team Leader should see projects for their assigned team
            val teamId = _uiState.value.currentUser?.teamId ?: ""
            
            val teamLeaderTeamsFlow = teamRepository.getAllTeams().map { teams ->
                teams.filter { it.leaderId == userId }
            }

            if (teamId.isNotEmpty()) {
                combine(
                    teamRepository.getTeamById(teamId),
                    projectRepository.getProjectsByTeam(teamId),
                    teamLeaderTeamsFlow
                ) { team, projects, managedTeams ->
                    val activeProjects = projects.filter { it.status == ProjectStatus.ACTIVE || it.status == ProjectStatus.PLANNING }
                    val upcomingProjects = projects.filter { it.status == ProjectStatus.NOT_STARTED }
                    val completedProjects = projects.filter { it.status == ProjectStatus.COMPLETED }
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            teams = managedTeams,
                            totalManagedTeams = managedTeams.size,
                            projects = projects,
                            activeProjects = activeProjects,
                            upcomingProjects = upcomingProjects,
                            activeProjectsCount = activeProjects.size,
                            completedProjectsCount = completedProjects.size,
                            upcomingProjectsCount = upcomingProjects.size
                        ) 
                    }
                }.collect()
            } else {
                teamLeaderTeamsFlow.collectLatest { managedTeams ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            teams = managedTeams, 
                            totalManagedTeams = managedTeams.size,
                            projects = emptyList()
                        ) 
                    }
                }
            }
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
