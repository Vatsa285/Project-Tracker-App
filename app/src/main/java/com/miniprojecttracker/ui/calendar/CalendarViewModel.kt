package com.miniprojecttracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniprojecttracker.data.repository.TaskRepository
import com.miniprojecttracker.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class CalendarViewType {
    PROJECTS, TASKS
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val projectRepository: com.miniprojecttracker.data.repository.ProjectRepository,
    private val authRepository: com.miniprojecttracker.data.repository.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkUserRoleAndSetDefaultView()
        loadData()
    }

    private fun checkUserRoleAndSetDefaultView() {
        viewModelScope.launch {
            authRepository.getCurrentUserFlow().filterNotNull().collect { user ->
                val defaultView = when (user.role) {
                    com.miniprojecttracker.domain.model.UserRole.MANAGER -> CalendarViewType.PROJECTS
                    else -> CalendarViewType.TASKS
                }
                _uiState.update { it.copy(viewType = defaultView, userRole = user.role) }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val userFlow = authRepository.getCurrentUserFlow().filterNotNull()
            
            combine(userFlow, _uiState.map { it.viewType }.distinctUntilChanged()) { user, viewType ->
                _uiState.update { it.copy(userRole = user.role) }
                user to viewType
            }.flatMapLatest { (currentUser, viewType) ->
                if (viewType == CalendarViewType.TASKS) {
                    when (currentUser.role) {
                        com.miniprojecttracker.domain.model.UserRole.TEAM_LEADER -> {
                            projectRepository.getProjectsByTeam(currentUser.teamId).flatMapLatest { projects ->
                                val projectIds = projects.map { it.id }
                                taskRepository.getAllTasks().map { tasks ->
                                    tasks.filter { it.projectId in projectIds }
                                }
                            }
                        }
                        com.miniprojecttracker.domain.model.UserRole.DEVELOPER -> {
                            taskRepository.getTasksByUser(currentUser.id)
                        }
                        com.miniprojecttracker.domain.model.UserRole.MANAGER -> {
                            // Managers don't see tasks in calendar anymore, only project deadlines
                            flowOf(emptyList())
                        }
                    }
                } else {
                    // PROJECTS view
                    when (currentUser.role) {
                        com.miniprojecttracker.domain.model.UserRole.MANAGER -> projectRepository.getProjectsByManager(currentUser.id)
                        com.miniprojecttracker.domain.model.UserRole.TEAM_LEADER -> projectRepository.getProjectsByTeam(currentUser.teamId)
                        else -> flowOf(emptyList())
                    }
                }
            }.collectLatest { items ->
                val itemsByDate = items.groupBy { item ->
                    val dueDate = if (item is Task) item.dueDate else (item as com.miniprojecttracker.domain.model.Project).deadline
                    val cal = Calendar.getInstance().apply { setTimeInMillis(dueDate) }
                    String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        itemsByDate = itemsByDate,
                        selectedItems = if (it.selectedDateKey.isNotEmpty()) {
                            itemsByDate[it.selectedDateKey] ?: emptyList()
                        } else {
                            emptyList()
                        }
                    ) 
                }
            }
        }
    }

    fun setViewType(viewType: CalendarViewType) {
        _uiState.update { it.copy(viewType = viewType) }
    }

    fun selectDate(dateKey: String, timeInMillis: Long) {
        _uiState.update { 
            it.copy(
                selectedDateKey = dateKey,
                selectedItems = it.itemsByDate[dateKey] ?: emptyList()
            ) 
        }
    }
}

data class CalendarUiState(
    val isLoading: Boolean = false,
    val userRole: com.miniprojecttracker.domain.model.UserRole? = null,
    val viewType: CalendarViewType = CalendarViewType.TASKS,
    val itemsByDate: Map<String, List<Any>> = emptyMap(),
    val selectedDateKey: String = "",
    val selectedItems: List<Any> = emptyList()
)
