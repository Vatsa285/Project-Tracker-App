package com.miniprojecttracker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.ui.settings.SettingsViewModel
import com.miniprojecttracker.ui.theme.MiniProjectTrackerTheme

@Composable
fun MainAppContainer(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    
    // We only want to override if the user has explicitly set a preference, 
    // otherwise fallback to system setting. But for this app, we just use 
    // the stored preference (defaults to false out of box, could default to system).
    
    val isSystemDark = isSystemInDarkTheme()
    // For simplicity, we just bind the theme to the Datastore value directly.
    val useDarkTheme = uiState.isDarkMode 

    MiniProjectTrackerTheme(
        darkTheme = useDarkTheme,
        content = content
    )
}
