package com.miniprojecttracker.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.Priority
import com.miniprojecttracker.domain.model.Task
import com.miniprojecttracker.domain.model.TaskStatus
import com.miniprojecttracker.domain.model.UserRole
import com.miniprojecttracker.ui.components.AppTopBar
import com.miniprojecttracker.ui.components.ErrorDialog
import com.miniprojecttracker.ui.components.LoadingIndicator
import com.miniprojecttracker.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTaskScreen(
    projectId: String,
    taskId: String?,
    onNavigateBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditing = taskId != null
    val currentTask = uiState.currentTask
    val isTaskCompleted = currentTask?.status == TaskStatus.DONE
    val isProjectCompleted = uiState.currentProject?.status == com.miniprojecttracker.domain.model.ProjectStatus.COMPLETED
    val isReadOnly = isTaskCompleted || isProjectCompleted

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf("") }
    var selectedUserName by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var isBlocker by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000) } // 3 days default

    var userExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadTeamMembers(projectId)
        if (isEditing) {
            viewModel.loadTask(taskId!!)
        }
    }

    LaunchedEffect(uiState.currentTask) {
        if (isEditing && uiState.currentTask != null) {
            val t = uiState.currentTask!!
            title = t.title
            description = t.description
            selectedUserId = t.assignedTo
            selectedUserName = t.assignedToName
            selectedPriority = t.priority
            isBlocker = t.isBlocker
            dueDate = t.dueDate
        }
    }

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!, onDismiss = { viewModel.clearError() })
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditing) "Edit Task" else "Create Task",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = isEditing || isReadOnly
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                    readOnly = isEditing || isReadOnly
                )

                // Assignee Selection
                if (uiState.currentUser?.role != UserRole.MANAGER) {
                    ExposedDropdownMenuBox(
                        expanded = userExpanded,
                        onExpandedChange = { userExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val displayName = if (selectedUserName.isNotBlank()) selectedUserName else "Unassigned"
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {},
                            label = { Text("Assign To") },
                            trailingIcon = { 
                                if (uiState.currentUser?.role == UserRole.TEAM_LEADER && !isReadOnly) {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = userExpanded)
                                }
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            readOnly = true
                        )
                        if (uiState.currentUser?.role == UserRole.TEAM_LEADER && !isReadOnly) {
                            ExposedDropdownMenu(
                                expanded = userExpanded,
                                onDismissRequest = { userExpanded = false }
                            ) {
                                uiState.availableUsers.forEach { user ->
                                    DropdownMenuItem(
                                        text = { Text(text = user.name) },
                                        onClick = {
                                            selectedUserId = user.id
                                            selectedUserName = user.name
                                            userExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Priority Selection
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded && !isReadOnly,
                    onExpandedChange = { if (!isReadOnly) priorityExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPriority.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { 
                            if (!isReadOnly) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded)
                            }
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (!isReadOnly) {
                        ExposedDropdownMenu(
                            expanded = priorityExpanded,
                            onDismissRequest = { priorityExpanded = false }
                        ) {
                            Priority.entries.forEach { priority ->
                                DropdownMenuItem(
                                    text = { Text(text = priority.displayName) },
                                    onClick = {
                                        selectedPriority = priority
                                        priorityExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Due Date
                var showDatePicker by remember { mutableStateOf(false) }
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = dueDate
                )

                if (showDatePicker && !isReadOnly) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dueDate = datePickerState.selectedDateMillis ?: dueDate
                                showDatePicker = false
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                OutlinedTextField(
                    value = DateUtils.formatDate(dueDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Due Date") },
                    trailingIcon = { 
                        IconButton(
                            onClick = { if (!isReadOnly) showDatePicker = true },
                            enabled = !isReadOnly
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Blocker Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mark as Blocker")
                    Switch(
                        checked = isBlocker,
                        onCheckedChange = { if (!isReadOnly) isBlocker = it },
                        enabled = !isReadOnly
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isReadOnly) {
                    Button(
                        onClick = {
                            val isTl = uiState.currentUser?.role == UserRole.TEAM_LEADER
                            if (title.isBlank()) {
                                viewModel.setError("Task title is required")
                                return@Button
                            }
                            if (isTl && selectedUserId.isBlank()) {
                                viewModel.setError("Team Lead must assign an assignee")
                                return@Button
                            }
                            
                            val task = Task(
                                id = taskId ?: "",
                                projectId = projectId,
                                assignedTo = selectedUserId,
                                assignedToName = selectedUserName,
                                title = title,
                                description = description,
                                status = uiState.currentTask?.status ?: TaskStatus.TODO,
                                priority = selectedPriority,
                                dueDate = dueDate,
                                isBlocker = isBlocker,
                                createdAt = uiState.currentTask?.createdAt ?: System.currentTimeMillis()
                            )
                            viewModel.saveTask(task, projectId, onNavigateBack)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(if (isEditing) "Save Changes" else "Create Task")
                    }
                } else {
                    Text(
                        text = if (isProjectCompleted) "Project is completed. Nothing can be edited." else "Completed tasks cannot be edited.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
