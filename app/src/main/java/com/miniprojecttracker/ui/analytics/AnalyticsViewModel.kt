package com.miniprojecttracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniprojecttracker.data.repository.ProjectRepository
import com.miniprojecttracker.data.repository.TaskRepository
import com.miniprojecttracker.data.repository.UserRepository
import com.miniprojecttracker.domain.model.Priority
import com.miniprojecttracker.domain.model.ProjectStatus
import com.miniprojecttracker.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            combine(
                projectRepository.getAllProjects(),
                taskRepository.getAllTasks(),
                userRepository.getAllUsers()
            ) { projects, tasks, users ->
            
                val taskStatusCounts = tasks.groupingBy { it.status.name }.eachCount()
                
                val userCompletedTasks = tasks
                    .filter { it.status == TaskStatus.DONE }
                    .groupingBy { task -> 
                        users.find { it.id == task.assignedTo }?.name ?: "Unknown" 
                    }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .associate { it.key to it.value }

                val overdueTasksCount = tasks.count { 
                    it.dueDate < System.currentTimeMillis() && it.status != TaskStatus.DONE && it.dueDate > 0 
                }

                val delayedProjects = projects.filter { 
                    it.deadline < System.currentTimeMillis() && it.status != ProjectStatus.COMPLETED 
                }

                val smartInsights = mutableListOf<String>()
                if (overdueTasksCount > 0) {
                    smartInsights.add("Attention: There are $overdueTasksCount overdue tasks across projects.")
                }
                if (delayedProjects.isNotEmpty()) {
                    smartInsights.add("${delayedProjects.size} projects are behind schedule.")
                }
                
                val topPerformer = userCompletedTasks.maxByOrNull { it.value }
                if (topPerformer != null) {
                    smartInsights.add("${topPerformer.key} is leading with ${topPerformer.value} completed tasks.")
                } else {
                    smartInsights.add("No tasks have been completed yet.")
                }

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        totalProjects = projects.size,
                        totalTasks = tasks.size,
                        taskStatusCounts = taskStatusCounts,
                        userCompletedTasks = userCompletedTasks,
                        smartInsights = smartInsights
                    )
                }
            }.collect()
        }
    }
}

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val totalProjects: Int = 0,
    val totalTasks: Int = 0,
    val taskStatusCounts: Map<String, Int> = emptyMap(),
    val userCompletedTasks: Map<String, Int> = emptyMap(), // Top 5 developers by completed tasks
    val smartInsights: List<String> = emptyList()
)
