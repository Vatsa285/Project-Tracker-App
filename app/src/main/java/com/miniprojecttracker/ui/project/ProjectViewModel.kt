package com.miniprojecttracker.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniprojecttracker.data.repository.AuthRepository
import com.miniprojecttracker.data.repository.ProjectRepository
import com.miniprojecttracker.data.repository.TaskRepository
import com.miniprojecttracker.data.repository.TeamRepository
import com.miniprojecttracker.domain.model.*
import com.miniprojecttracker.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val teamRepository: TeamRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private var loadProjectsJob: kotlinx.coroutines.Job? = null

    private fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUserFlow().collectLatest { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun loadProjects(teamId: String? = null) {
        loadProjectsJob?.cancel()
        loadProjectsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val currentUser = authRepository.getCurrentUserFlow().filterNotNull().first()
            
            // Sync before loading
            try {
                projectRepository.syncProjects(currentUser.id, currentUser.role)
            } catch (_: Exception) {}

            val projectsFlow = when {
                teamId != null -> projectRepository.getProjectsByTeam(teamId)
                currentUser.role == UserRole.MANAGER -> projectRepository.getProjectsByManager(currentUser.id)
                currentUser.role == UserRole.TEAM_LEADER -> {
                    // Team leader sees projects from ALL teams they lead
                    combine(
                        teamRepository.getAllTeams(),
                        projectRepository.getAllProjects()
                    ) { teams, projects ->
                        val teamIds = teams.filter { it.leaderId == currentUser.id }.map { it.id }.toSet()
                        projects.filter { it.teamId in teamIds }
                    }
                }
                currentUser.role == UserRole.DEVELOPER -> 
                    projectRepository.getProjectsByTeam(currentUser.teamId)
                else -> projectRepository.getAllProjects()
            }

            projectsFlow.collectLatest { projects ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        projects = projects,
                        filteredProjects = filterProjects(projects, it.searchQuery, it.selectedStatusFilter)
                    ) 
                }
            }
        }
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                combine(
                    projectRepository.getProjectById(projectId),
                    taskRepository.getTasksByProject(projectId),
                    authRepository.getCurrentUserFlow()
                ) { project, tasks, user ->
                    Triple(project, tasks, user)
                }.collectLatest { (project, tasks, user) ->
                    if (project != null) {
                        val team = teamRepository.getTeamById(project.teamId).firstOrNull()
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                currentProject = project, 
                                projectTasks = tasks,
                                currentTeam = team,
                                currentUser = user
                            ) 
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Project not found") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadTeamsForSelection() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUserFlow().firstOrNull()
            val teamsFlow = if (currentUser?.role == UserRole.MANAGER) {
                teamRepository.getTeamsByManager(currentUser.id)
            } else {
                teamRepository.getAllTeams()
            }
            
            teamsFlow.collectLatest { teams ->
                _uiState.update { it.copy(availableTeams = teams) }
            }
        }
    }

    fun searchProjects(query: String) {
        _uiState.update { 
            it.copy(
                searchQuery = query,
                filteredProjects = filterProjects(it.projects, query, it.selectedStatusFilter)
            ) 
        }
    }

    fun setStatusFilter(status: String) {
        val newFilter = if (_uiState.value.selectedStatusFilter == status) "All" else status
        _uiState.update { 
            it.copy(
                selectedStatusFilter = newFilter,
                filteredProjects = filterProjects(it.projects, it.searchQuery, newFilter)
            ) 
        }
    }

    /**
     * Applies a filter without toggling. Used by navigation to set
     * the initial filter without risk of toggling it off on re-entry.
     */
    fun applyInitialFilter(status: String) {
        if (_uiState.value.selectedStatusFilter != status) {
            _uiState.update { 
                it.copy(
                    selectedStatusFilter = status,
                    filteredProjects = filterProjects(it.projects, it.searchQuery, status)
                ) 
            }
        }
    }

    private fun filterProjects(projects: List<Project>, query: String, statusFilter: String): List<Project> {
        var filtered = projects
        if (query.isNotBlank()) {
            filtered = filtered.filter { 
                it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) 
            }
        }
        if (statusFilter != "All") {
            filtered = when (statusFilter) {
                "Active" -> filtered.filter { 
                    it.status != ProjectStatus.COMPLETED && it.status != ProjectStatus.NOT_STARTED 
                }
                "Completed" -> filtered.filter { it.status == ProjectStatus.COMPLETED }
                "Not Started" -> filtered.filter { it.status == ProjectStatus.NOT_STARTED }
                else -> filtered.filter { it.status.displayName == statusFilter }
            }
        }
        return filtered
    }

    fun saveProject(project: Project, onSuccess: () -> Unit) {
        if (project.id.isNotEmpty() && project.status == ProjectStatus.ON_HOLD) {
            setError("Cannot edit project while it is on hold.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentUser = authRepository.getCurrentUserFlow().firstOrNull()
                val projectToSave = if (project.id.isBlank()) {
                    project.copy(managerId = currentUser?.id ?: "")
                } else {
                    project
                }

                if (projectToSave.id.isBlank()) {
                    projectRepository.createProject(projectToSave)
                } else {
                    projectRepository.updateProject(projectToSave)
                }
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save project") }
            }
        }
    }

    fun deleteProject(project: Project, onSuccess: () -> Unit) {
        if (project.status == ProjectStatus.ON_HOLD) {
            setError("Cannot delete project while it is on hold.")
            return
        }
         viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                projectRepository.deleteProject(project)
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to delete project") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setCurrentSection(section: String) {
        _uiState.update { it.copy(currentSection = section) }
    }

    fun startProject(projectId: String) {
        val project = _uiState.value.projects.find { it.id == projectId }
        if (project?.status == ProjectStatus.ON_HOLD) {
            setError("Cannot start project while it is on hold.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                projectRepository.startProject(projectId)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleProjectHold(projectId: String) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId).firstOrNull() ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (project.status == ProjectStatus.ON_HOLD) {
                    val previousStatus = project.previousStatusBeforeHold ?: ProjectStatus.ACTIVE
                    projectRepository.updateProject(project.copy(status = previousStatus, previousStatusBeforeHold = null))
                } else {
                    projectRepository.updateProject(project.copy(status = ProjectStatus.ON_HOLD, previousStatusBeforeHold = project.status))
                }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun submitStatusUpdateRequest(projectId: String, documentUrl: String, comment: String) {
        val project = _uiState.value.projects.find { it.id == projectId } ?: _uiState.value.currentProject
        if (project?.status == ProjectStatus.ON_HOLD) {
            setError("Cannot submit update request while project is on hold.")
            return
        }
        viewModelScope.launch {
            val tasks = taskRepository.getTasksByProject(projectId).first()
            val allTasksDone = tasks.all { it.status == TaskStatus.DONE }
            val project = projectRepository.getProjectById(projectId).firstOrNull()

            // If moving to COMPLETED (next status would be completed if current is ACTIVE)
            if (project?.status == ProjectStatus.ACTIVE && !allTasksDone) {
                _uiState.update { it.copy(isLoading = false, error = "Cannot submit for completion. All tasks must be completed first.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            try {
                projectRepository.submitStatusUpdateRequest(projectId, documentUrl, comment)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun reviewStatusUpdateRequest(projectId: String, approved: Boolean, nextStatus: ProjectStatus, comment: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                projectRepository.reviewStatusUpdateRequest(projectId, approved, nextStatus, comment)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

data class ProjectUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val projects: List<Project> = emptyList(),
    val filteredProjects: List<Project> = emptyList(),
    val currentProject: Project? = null,
    val currentTeam: Team? = null,
    val currentUser: User? = null,
    val projectTasks: List<com.miniprojecttracker.domain.model.Task> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: String = "All",
    val currentSection: String = "TASKS",
    val availableTeams: List<Team> = emptyList(),
    val filters: List<String> = listOf("All") + ProjectStatus.entries.map { it.displayName }
)
