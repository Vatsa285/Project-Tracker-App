package com.miniprojecttracker.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.UserRole
import com.miniprojecttracker.ui.components.*

@Composable
fun ProjectListScreen(
    initialFilter: String = "All",
    showFilters: Boolean = true,
    teamId: String? = null,
    onNavigateToProject: (String) -> Unit,
    onNavigateToCreateProject: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(teamId) {
        viewModel.loadProjects(teamId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = when (initialFilter) {
                    "Not Started" -> "New Projects"
                    "Active" -> "Active Projects"
                    "Completed" -> "Completed Projects"
                    else -> "Projects"
                },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            if (uiState.currentUser?.role == UserRole.MANAGER) {
                FloatingActionButton(onClick = onNavigateToCreateProject) {
                    Icon(Icons.Default.Add, contentDescription = "Add Project")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LaunchedEffect(initialFilter) {
                if (initialFilter != "All") {
                    viewModel.applyInitialFilter(initialFilter)
                }
            }

            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.searchProjects(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (showFilters) {
                FilterChips(
                    filters = uiState.filters,
                    selectedFilter = uiState.selectedStatusFilter,
                    onFilterSelected = { viewModel.setStatusFilter(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            val emptyMessage = when (initialFilter) {
                "Active" -> "No active projects found."
                "Completed" -> "No completed projects found."
                "Not Started" -> "No new projects found."
                else -> "No projects found."
            }

            if (uiState.isLoading) {
                LoadingIndicator(fullScreen = false, modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
            } else if (uiState.filteredProjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.filteredProjects) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onNavigateToProject(project.id) }
                        )
                    }
                }
            }
        }
    }
}
