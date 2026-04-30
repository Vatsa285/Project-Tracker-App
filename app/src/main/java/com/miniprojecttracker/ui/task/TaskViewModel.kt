package com.miniprojecttracker.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniprojecttracker.data.repository.CommentRepository
import com.miniprojecttracker.data.repository.ProjectRepository
import com.miniprojecttracker.data.repository.TaskRepository
import com.miniprojecttracker.data.repository.TeamRepository
import com.miniprojecttracker.data.repository.UserRepository
import com.miniprojecttracker.data.repository.AuthRepository
import com.miniprojecttracker.domain.model.*
import com.miniprojecttracker.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
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

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Sync specific task data
            try {
                commentRepository.syncComments(taskId)
            } catch (_: Exception) {}

            taskRepository.getTaskById(taskId).collectLatest { task ->
                if (task != null) {
                    val project = projectRepository.getProjectById(task.projectId).firstOrNull()
                    _uiState.update { it.copy(currentTask = task, currentProject = project, isLoading = false) }
                    loadComments(taskId)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Task not found") }
                }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun loadTasksByProject(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            taskRepository.syncTasks()
            taskRepository.getTasksByProject(projectId).collectLatest { tasks ->
                _uiState.update { it.copy(tasks = tasks, isLoading = false) }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun loadTasks(projectId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Sync tasks from Firestore
            taskRepository.syncTasks()

            val currentUser = authRepository.getCurrentUserFlow().firstOrNull()

            val tasksFlow = when {
                projectId != null -> taskRepository.getTasksByProject(projectId)
                currentUser?.role == UserRole.DEVELOPER -> taskRepository.getTasksByUser(currentUser.id)
                currentUser?.role == UserRole.TEAM_LEADER -> {
                    // Team leader sees tasks for all projects assigned to their team
                    // We need to fetch projects for that team first
                    projectRepository.getProjectsByTeam(currentUser.teamId).flatMapLatest { projects ->
                        val projectIds = projects.map { it.id }
                        taskRepository.getAllTasks().map { tasks ->
                            tasks.filter { it.projectId in projectIds }
                        }
                    }
                }
                currentUser?.role == UserRole.MANAGER -> {
                    // Manager sees tasks for all projects they manage
                    projectRepository.getProjectsByManager(currentUser.id).flatMapLatest { projects ->
                        val projectIds = projects.map { it.id }
                        taskRepository.getAllTasks().map { tasks ->
                            tasks.filter { it.projectId in projectIds }
                        }
                    }
                }
                else -> taskRepository.getAllTasks()
            }

            tasksFlow.collectLatest { tasks ->
                _uiState.update { it.copy(tasks = tasks, isLoading = false) }
            }
        }
    }

    private fun loadComments(taskId: String) {
        viewModelScope.launch {
            commentRepository.getCommentsByTask(taskId).collectLatest { comments ->
                _uiState.update { it.copy(comments = comments) }
            }
        }
    }

    fun loadTeamMembers(projectId: String) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId).firstOrNull()
            if (project != null) {
                val team = teamRepository.getTeamById(project.teamId).firstOrNull()
                userRepository.getUsersByTeam(project.teamId).collectLatest { users ->
                     // Filter out the team leader - tasks cannot be assigned to them
                     val filteredUsers = users.filter { it.id != team?.leaderId }
                     _uiState.update { it.copy(availableUsers = filteredUsers) }
                }
            }
        }
    }

    fun saveTask(task: Task, projectId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId).firstOrNull()
            if (project?.status == ProjectStatus.ON_HOLD) {
                _uiState.update { it.copy(error = "Cannot save task while project is on hold.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (task.id.isBlank()) {
                    taskRepository.createTask(task.copy(projectId = projectId))
                } else {
                    taskRepository.updateTask(task)
                }
                
                // Recalculate project progress
                projectRepository.recalculateProgress(projectId)
                
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                 _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save task") }
            }
        }
    }

    fun updateTaskStatus(taskId: String, status: TaskStatus) {
         viewModelScope.launch {
             val task = taskRepository.getTaskById(taskId).firstOrNull()
             if (task != null) {
                 val project = projectRepository.getProjectById(task.projectId).firstOrNull()
                 if (project?.status == ProjectStatus.ON_HOLD) {
                     _uiState.update { it.copy(error = "Cannot update task status while project is on hold.") }
                     return@launch
                 }
             }
             taskRepository.updateTaskStatus(taskId, status)
             if (task != null) {
                 projectRepository.recalculateProgress(task.projectId)
                 
                 // Gamification: Add points if task is completed
                 if (status == TaskStatus.DONE && task.status != TaskStatus.DONE) {
                     val userId = task.assignedTo
                     var points = Constants.POINTS_TASK_COMPLETED
                     if (task.dueDate >= System.currentTimeMillis()) {
                         points += Constants.POINTS_TASK_ON_TIME
                     }
                     userRepository.addPoints(userId, points)
                 }
             }
         }
    }

    fun addComment(taskId: String, content: String) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId).firstOrNull()
            if (task != null) {
                val project = projectRepository.getProjectById(task.projectId).firstOrNull()
                if (project?.status == ProjectStatus.ON_HOLD) {
                    _uiState.update { it.copy(error = "Cannot add comment while project is on hold.") }
                    return@launch
                }
            }
            val userId = authRepository.currentUserId ?: return@launch
            val user = userRepository.getUserById(userId).firstOrNull() ?: return@launch
            
            val comment = Comment(
                taskId = taskId,
                userId = userId,
                userName = user.name,
                content = content
            )
            commentRepository.addComment(comment)
            
            // Gamification: points for interaction
            userRepository.addPoints(userId, Constants.POINTS_COMMENT_ADDED)
        }
    }

    fun deleteTask(task: Task, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(task.projectId).firstOrNull()
            if (project?.status == ProjectStatus.ON_HOLD) {
                _uiState.update { it.copy(error = "Cannot delete task while project is on hold.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            try {
                taskRepository.deleteTask(task)
                projectRepository.recalculateProgress(task.projectId)
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                 _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to delete task") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun submitUpdateRequest(taskId: String, documentUrl: String, comment: String) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId).firstOrNull()
            if (task != null) {
                val project = projectRepository.getProjectById(task.projectId).firstOrNull()
                if (project?.status == ProjectStatus.ON_HOLD) {
                    _uiState.update { it.copy(error = "Cannot submit update request while project is on hold.") }
                    return@launch
                }
            }
            _uiState.update { it.copy(isLoading = true) }
            try {
                taskRepository.submitUpdateRequest(taskId, documentUrl, comment)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to submit update") }
            }
        }
    }

    fun reviewUpdateRequest(taskId: String, approved: Boolean, comment: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                taskRepository.reviewUpdateRequest(taskId, approved, comment)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to review update") }
            }
        }
    }
}

data class TaskUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val tasks: List<Task> = emptyList(),
    val currentTask: Task? = null,
    val currentProject: Project? = null,
    val currentUser: User? = null,
    val comments: List<Comment> = emptyList(),
    val availableUsers: List<User> = emptyList()
)
