package com.miniprojecttracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniprojecttracker.util.PreferencesManager
import com.miniprojecttracker.data.repository.AuthRepository
import com.miniprojecttracker.data.repository.UserRepository
import com.miniprojecttracker.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                authRepository.getCurrentUserFlow(),
                preferencesManager.isDarkMode
            ) { user, isDarkMode ->
                 _uiState.update { 
                     it.copy(
                         currentUser = user,
                         isDarkMode = isDarkMode,
                         isLoading = false
                     ) 
                 }
            }.collect()
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
             preferencesManager.setDarkMode(enabled)
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
             authRepository.signOut()
             onSuccess()
        }
    }
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val currentUser: User? = null,
    val isDarkMode: Boolean = false,
    val error: String? = null
)
