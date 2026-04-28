package com.miniprojecttracker.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.TaskStatus
import com.miniprojecttracker.ui.components.AppTopBar
import com.miniprojecttracker.ui.components.LoadingIndicator
import com.miniprojecttracker.ui.components.TaskCard
import androidx.compose.material.icons.Icons

@Composable
fun TaskListScreen(
    filter: String,
    projectId: String = "",
    onNavigateToTask: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(filter, projectId) {
        if (projectId.isNotEmpty()) {
            viewModel.loadTasksByProject(projectId)
        } else {
            viewModel.loadTasks()
        }
    }

    val filteredTasks = when (filter) {
        "PENDING", "Active" -> uiState.tasks.filter { it.status != TaskStatus.DONE }
        "COMPLETED", "Completed" -> uiState.tasks.filter { it.status == TaskStatus.DONE }
        "OVERDUE" -> uiState.tasks.filter { it.dueDate < System.currentTimeMillis() && it.status != TaskStatus.DONE }
        else -> uiState.tasks
    }

    val isManager = uiState.currentUser?.role == com.miniprojecttracker.domain.model.UserRole.MANAGER
    val isTeamLeader = uiState.currentUser?.role == com.miniprojecttracker.domain.model.UserRole.TEAM_LEADER
    val canEdit = isManager || isTeamLeader

    Scaffold(
        topBar = {
            AppTopBar(
                title = when (filter) {
                    "PENDING" -> "Pending Tasks"
                    "COMPLETED" -> "Completed Tasks"
                    "OVERDUE" -> "Overdue Tasks"
                    else -> "Tasks"
                },
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator()
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (filteredTasks.isEmpty()) {
                Text(
                    text = "No tasks found.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks) { task ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TaskCard(
                                task = task,
                                onClick = { onNavigateToTask(task.id) }
                            )
                            if (canEdit && task.status != TaskStatus.DONE) {
                                IconButton(
                                    onClick = { onNavigateToTask(task.id) }, // Navigate to detail, which has edit
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
