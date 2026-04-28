package com.miniprojecttracker.ui.team

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.Team
import com.miniprojecttracker.domain.model.UserRole
import com.miniprojecttracker.ui.components.AppTopBar
import com.miniprojecttracker.ui.components.ErrorDialog
import com.miniprojecttracker.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTeamScreen(
    teamId: String?,
    onNavigateBack: () -> Unit,
    viewModel: TeamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditing = teamId != null

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTeamLeaderId by remember { mutableStateOf("") }
    var selectedMemberIds by remember { mutableStateOf(setOf<String>()) }
    var managerExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isManager = uiState.currentUser?.role == UserRole.MANAGER
    val isTeamLeader = uiState.currentUser?.role == UserRole.TEAM_LEADER

    LaunchedEffect(teamId) {
        viewModel.loadAvailableTeamLeaders()
        viewModel.loadAvailableDevelopers(teamId)
        if (isEditing) {
            viewModel.loadTeam(teamId!!)
        }
    }

    LaunchedEffect(uiState.currentTeam) {
        if (isEditing && uiState.currentTeam != null) {
            val t = uiState.currentTeam!!
            name = t.name
            description = t.description
            selectedTeamLeaderId = t.leaderId
            selectedMemberIds = t.memberIds.toSet()
        }
    }

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!, onDismiss = { viewModel.clearError() })
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditing) "Edit Team" else "Create Team",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (isEditing && isManager) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Team")
                        }
                    }
                }
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Team Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = isManager
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                    enabled = isManager
                )

                if (isManager) {
                    ExposedDropdownMenuBox(
                        expanded = managerExpanded,
                        onExpandedChange = { managerExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val leaderName = uiState.availableTeamLeaders.find { it.id == selectedTeamLeaderId }?.name ?: "Select Team Leader"
                        OutlinedTextField(
                            value = leaderName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign Team Leader") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = managerExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = managerExpanded,
                            onDismissRequest = { managerExpanded = false }
                        ) {
                            uiState.availableTeamLeaders.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(text = user.name) },
                                    onClick = {
                                        selectedTeamLeaderId = user.id
                                        managerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (isTeamLeader) {
                    Text(
                        text = "Select Team Members",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    uiState.availableDevelopers.forEach { developer ->
                        val isSelf = developer.id == uiState.currentUser?.id
                        if (!isSelf) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMemberIds = if (selectedMemberIds.contains(developer.id)) {
                                            selectedMemberIds - developer.id
                                        } else {
                                            selectedMemberIds + developer.id
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedMemberIds.contains(developer.id),
                                    onCheckedChange = { checked ->
                                        selectedMemberIds = if (checked) {
                                            selectedMemberIds + developer.id
                                        } else {
                                            selectedMemberIds - developer.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = developer.name)
                                if (developer.teamId.isNotEmpty() && developer.teamId != (teamId ?: "")) {
                                    Text(
                                        text = " (In another team)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                } else if (isManager) {
                    Text(
                        text = "Team Members (Selected by Team Lead)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (selectedMemberIds.isEmpty()) {
                        Text("No members assigned yet.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        // Manager can see who is in the team but not change it according to "developers must be selected by team-lead"
                        uiState.availableDevelopers.filter { selectedMemberIds.contains(it.id) }.forEach { developer ->
                            Text("• ${developer.name}", modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val currentUserId = viewModel.getCurrentUserId()
                        if (name.isNotBlank() && selectedTeamLeaderId.isNotBlank() && currentUserId != null) {
                            val team = Team(
                                id = teamId ?: "",
                                name = name,
                                managerId = uiState.currentTeam?.managerId ?: (if (isManager) currentUserId else ""),
                                leaderId = selectedTeamLeaderId,
                                description = description,
                                memberIds = selectedMemberIds.toList(),
                                createdAt = uiState.currentTeam?.createdAt ?: System.currentTimeMillis()
                            )
                            viewModel.saveTeam(team, onNavigateBack)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = isManager || isTeamLeader
                ) {
                    Text(if (isEditing) "Save Changes" else "Create Team")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Team") },
            text = { Text("Are you sure you want to delete this team? It might leave users unassigned.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        uiState.currentTeam?.let { viewModel.deleteTeam(it, onNavigateBack) }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
