package com.miniprojecttracker.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniprojecttracker.data.repository.UserRepository
import com.miniprojecttracker.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.miniprojecttracker.domain.model.UserRole

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: com.miniprojecttracker.data.repository.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadLeaderboard()
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUserFlow().collectLatest { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.getLeaderboard().collectLatest { users ->
                val developerOnly = users.filter { it.role == UserRole.DEVELOPER }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        topUsers = developerOnly
                    ) 
                }
            }
        }
    }

    fun resetLeaderboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.resetMonthlyLeaderboard()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val topUsers: List<User> = emptyList(),
    val currentUser: User? = null
)
