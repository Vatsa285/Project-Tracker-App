package com.miniprojecttracker.ui.kanban

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.TaskStatus
import com.miniprojecttracker.ui.components.AppTopBar
import com.miniprojecttracker.ui.components.KanbanColumn
import com.miniprojecttracker.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanBoardScreen(
    projectId: String,
    onNavigateToTask: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: KanbanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTaskId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) {
        viewModel.loadKanbanBoard(projectId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = uiState.project?.let { "${it.name} - Kanban" } ?: "Kanban Board",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.project == null) {
            LoadingIndicator()
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    KanbanColumnWrapper(
                        status = TaskStatus.TODO,
                        tasks = uiState.todoTasks,
                        onTaskClick = { 
                            onNavigateToTask(it)
                        }
                    )
                }
                item {
                    KanbanColumnWrapper(
                        status = TaskStatus.IN_PROGRESS,
                        tasks = uiState.inProgressTasks,
                        onTaskClick = { 
                            onNavigateToTask(it)
                        }
                    )
                }
                item {
                    KanbanColumnWrapper(
                        status = TaskStatus.DONE,
                        tasks = uiState.doneTasks,
                        onTaskClick = { 
                            onNavigateToTask(it)
                        }
                    )
                }
            }
        }
    }

    if (selectedTaskId != null) {
        AlertDialog(
            onDismissRequest = { selectedTaskId = null },
            title = { Text("Task Action") },
            text = { Text("Move task or view details?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onNavigateToTask(selectedTaskId!!)
                        selectedTaskId = null
                    }
                ) {
                    Text("View Details")
                }
            },
            dismissButton = {
                // Simplified status movement logic for demo
                // In a real app, this would be a proper BottomSheet or drag-drop implementation
                Column {
                   TaskStatus.values().forEach { status ->
                       TextButton(
                           onClick = {
                               viewModel.updateTaskStatus(selectedTaskId!!, status)
                               selectedTaskId = null
                           }
                       ) {
                           Text("Move to ${status.displayName}")
                       }
                   }
                }
            }
        )
    }
}

@Composable
fun KanbanColumnWrapper(
    status: TaskStatus,
    tasks: List<com.miniprojecttracker.domain.model.Task>,
    onTaskClick: (String) -> Unit
) {
    KanbanColumn(
        status = status,
        tasks = tasks,
        onTaskClick = onTaskClick,
        modifier = Modifier.fillMaxHeight()
    )
}
