package com.miniprojecttracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniprojecttracker.data.repository.ProjectRepository
import com.miniprojecttracker.data.repository.TaskRepository
import com.miniprojecttracker.data.repository.TeamRepository
import com.miniprojecttracker.data.repository.AuthRepository
import com.miniprojecttracker.domain.model.Project
import com.miniprojecttracker.domain.model.Task
import com.miniprojecttracker.domain.model.UserRole
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
    private val projectRepository: ProjectRepository,
    private val teamRepository: TeamRepository,
    private val authRepository: AuthRepository
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
                    UserRole.MANAGER -> CalendarViewType.PROJECTS
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
                        UserRole.TEAM_LEADER -> {
                            // Team leader sees tasks from ALL teams they lead
                            combine(
                                teamRepository.getAllTeams(),
                                projectRepository.getAllProjects(),
                                taskRepository.getAllTasks()
                            ) { teams, projects, tasks ->
                                val teamIds = teams.filter { it.leaderId == currentUser.id }.map { it.id }.toSet()
                                val projectIds = projects.filter { it.teamId in teamIds }.map { it.id }.toSet()
                                tasks.filter { it.projectId in projectIds } as List<Any>
                            }
                        }
                        UserRole.DEVELOPER -> {
                            taskRepository.getTasksByUser(currentUser.id).map { it as List<Any> }
                        }
                        UserRole.MANAGER -> {
                            // Managers don't see tasks in calendar anymore, only project deadlines
                            flowOf(emptyList())
                        }
                    }
                } else {
                    // PROJECTS view
                    when (currentUser.role) {
                        UserRole.MANAGER -> projectRepository.getProjectsByManager(currentUser.id).map { it as List<Any> }
                        UserRole.TEAM_LEADER -> {
                            // Team leader sees projects from ALL teams they lead
                            combine(
                                teamRepository.getAllTeams(),
                                projectRepository.getAllProjects()
                            ) { teams, projects ->
                                val teamIds = teams.filter { it.leaderId == currentUser.id }.map { it.id }.toSet()
                                projects.filter { it.teamId in teamIds } as List<Any>
                            }
                        }
                        else -> flowOf(emptyList())
                    }
                }
            }.collectLatest { items ->
                val itemsByDate = items.groupBy { item ->
                    val dueDate = if (item is Task) item.dueDate else (item as Project).deadline
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
    val userRole: UserRole? = null,
    val viewType: CalendarViewType = CalendarViewType.TASKS,
    val itemsByDate: Map<String, List<Any>> = emptyMap(),
    val selectedDateKey: String = "",
    val selectedItems: List<Any> = emptyList()
)
