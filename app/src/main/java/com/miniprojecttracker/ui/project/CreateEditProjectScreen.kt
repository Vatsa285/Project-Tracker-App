package com.miniprojecttracker.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.Priority
import com.miniprojecttracker.domain.model.Project
import com.miniprojecttracker.domain.model.ProjectStatus
import com.miniprojecttracker.ui.components.AppTopBar
import com.miniprojecttracker.ui.components.ErrorDialog
import com.miniprojecttracker.ui.components.LoadingIndicator
import com.miniprojecttracker.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditProjectScreen(
    projectId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditing = projectId != null

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTeamId by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var selectedStatus by remember { mutableStateOf(ProjectStatus.NOT_STARTED) }
    
    // For date picker (simplified logic for demo, usually requires a proper DatePickerDialog)
    var deadline by remember { mutableStateOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000) } // Default 1 week from now

    var teamExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadTeamsForSelection()
        if (isEditing) {
            viewModel.loadProject(projectId!!)
        }
    }

    LaunchedEffect(uiState.currentProject) {
        if (isEditing && uiState.currentProject != null) {
            val p = uiState.currentProject!!
            name = p.name
            description = p.description
            selectedTeamId = p.teamId
            selectedPriority = p.priority
            selectedStatus = p.status
            deadline = p.deadline
        }
    }

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!, onDismiss = { viewModel.clearError() })
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditing) "Edit Project" else "Create Project",
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
                val isManager = uiState.currentUser?.role == com.miniprojecttracker.domain.model.UserRole.MANAGER
                val isProjectCompleted = uiState.currentProject?.status == ProjectStatus.COMPLETED

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = (isEditing && isManager) || isProjectCompleted
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                    readOnly = (isEditing && isManager) || isProjectCompleted
                )

                // Team Selection
                ExposedDropdownMenuBox(
                    expanded = teamExpanded && !isProjectCompleted,
                    onExpandedChange = { if (!isProjectCompleted) teamExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selectedTeamName = uiState.availableTeams.find { it.id == selectedTeamId }?.name ?: "Select Team"
                    OutlinedTextField(
                        value = selectedTeamName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign to Team") },
                        trailingIcon = { 
                            if (!isProjectCompleted) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded)
                            }
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (!isProjectCompleted) {
                        ExposedDropdownMenu(
                            expanded = teamExpanded,
                            onDismissRequest = { teamExpanded = false }
                        ) {
                            uiState.availableTeams.forEach { team ->
                                DropdownMenuItem(
                                    text = { Text(text = team.name) },
                                    onClick = {
                                        selectedTeamId = team.id
                                        teamExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Priority Selection
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded && !isProjectCompleted,
                    onExpandedChange = { if (!isProjectCompleted) priorityExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPriority.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { 
                            if (!isProjectCompleted) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded)
                            }
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (!isProjectCompleted) {
                        ExposedDropdownMenu(
                            expanded = priorityExpanded,
                            onDismissRequest = { priorityExpanded = false }
                        ) {
                            Priority.values().forEach { priority ->
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


                // Deadline Selection
                var showDatePicker by remember { mutableStateOf(false) }
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = deadline
                )

                if (showDatePicker && !isProjectCompleted) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                deadline = datePickerState.selectedDateMillis ?: deadline
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
                    value = DateUtils.formatDate(deadline),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deadline") },
                    trailingIcon = { 
                        IconButton(
                            onClick = { if (!isProjectCompleted) showDatePicker = true },
                            enabled = !isProjectCompleted
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!isProjectCompleted) {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && selectedTeamId.isNotBlank()) {
                                val project = Project(
                                    id = projectId ?: "",
                                    teamId = selectedTeamId,
                                    name = name,
                                    description = description,
                                    deadline = deadline,
                                    priority = selectedPriority,
                                    status = selectedStatus,
                                    progress = uiState.currentProject?.progress ?: 0f,
                                    createdAt = uiState.currentProject?.createdAt ?: System.currentTimeMillis()
                                )
                                viewModel.saveProject(project, onNavigateBack)
                            } else {
                                // Show error
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(if (isEditing) "Save Changes" else "Create Project")
                    }
                } else {
                    Text(
                        "Project is completed and cannot be modified.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}
