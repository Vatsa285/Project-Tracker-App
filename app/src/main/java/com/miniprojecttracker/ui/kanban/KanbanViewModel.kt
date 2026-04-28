package com.miniprojecttracker.ui.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniprojecttracker.data.repository.ProjectRepository
import com.miniprojecttracker.data.repository.TaskRepository
import com.miniprojecttracker.domain.model.Project
import com.miniprojecttracker.domain.model.Task
import com.miniprojecttracker.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KanbanViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KanbanUiState())
    val uiState = _uiState.asStateFlow()

    fun loadKanbanBoard(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            projectRepository.getProjectById(projectId).collectLatest { project ->
                 _uiState.update { it.copy(project = project) }
            }
        }
        
        viewModelScope.launch {
            taskRepository.getTasksByProject(projectId).collectLatest { tasks ->
                val groupedTasks = tasks.groupBy { it.status }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        todoTasks = groupedTasks[TaskStatus.TODO] ?: emptyList(),
                        inProgressTasks = groupedTasks[TaskStatus.IN_PROGRESS] ?: emptyList(),
                        doneTasks = groupedTasks[TaskStatus.DONE] ?: emptyList()
                    ) 
                }
            }
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        viewModelScope.launch {
             taskRepository.updateTaskStatus(taskId, newStatus)
             uiState.value.project?.id?.let { projectId ->
                 projectRepository.recalculateProgress(projectId)
             }
        }
    }
}

data class KanbanUiState(
    val isLoading: Boolean = false,
    val project: Project? = null,
    val todoTasks: List<Task> = emptyList(),
    val inProgressTasks: List<Task> = emptyList(),
    val doneTasks: List<Task> = emptyList()
)
