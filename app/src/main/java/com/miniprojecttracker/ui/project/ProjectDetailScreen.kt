package com.miniprojecttracker.ui.project

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.ProjectStatus
import com.miniprojecttracker.domain.model.ProjectStatusUpdate
import com.miniprojecttracker.domain.model.TaskStatus
import com.miniprojecttracker.domain.model.UpdateRequestStatus
import com.miniprojecttracker.domain.model.UserRole
import com.miniprojecttracker.ui.components.*
import com.miniprojecttracker.ui.theme.SuccessGreen
import com.miniprojecttracker.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onNavigateToTask: (String) -> Unit,
    onNavigateToTaskList: (String, String) -> Unit,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToKanban: () -> Unit,
    onNavigateToEditProject: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!, onDismiss = { viewModel.clearError() })
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = uiState.currentProject?.name ?: "Loading...",
                onNavigateBack = onNavigateBack,
                actions = {
                    val currentUserRole = uiState.currentUser?.role
                    val project = uiState.currentProject
                    val isCompleted = project?.status == ProjectStatus.COMPLETED

                    if (!isCompleted && project != null && project.status != ProjectStatus.NOT_STARTED) {
                        val canToggleHold = currentUserRole == UserRole.MANAGER
                        if (canToggleHold) {
                            IconButton(onClick = { viewModel.toggleProjectHold(project.id) }) {
                                Icon(
                                    if (project.status == ProjectStatus.ON_HOLD) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (project.status == ProjectStatus.ON_HOLD) "Resume Project" else "Hold Project"
                                )
                            }
                        }
                    }

                    IconButton(onClick = onNavigateToKanban) {
                        Icon(Icons.Default.ViewCarousel, contentDescription = "Kanban Board")
                    }

                    val isManager = currentUserRole == UserRole.MANAGER
                    val isHold = project?.status == ProjectStatus.ON_HOLD

                    if (isManager && !isCompleted && !isHold) {
                        IconButton(onClick = onNavigateToEditProject) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Project")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.currentProject == null) {
            LoadingIndicator()
        } else if (uiState.currentProject != null) {
            val project = uiState.currentProject!!
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(status = project.status)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.currentUser?.role == UserRole.TEAM_LEADER && project.status == ProjectStatus.NOT_STARTED) {
                                Button(
                                    onClick = { viewModel.startProject(project.id) },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Start Project")
                                }
                            }
                            PriorityBadge(priority = project.priority)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Deadline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(DateUtils.formatDate(project.deadline), style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Assigned Team", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(uiState.currentTeam?.name ?: "Loading...", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressBar(progress = project.progress)

                    Spacer(modifier = Modifier.height(24.dp))

                    TabRow(
                        selectedTabIndex = if (uiState.currentSection == "TASKS") 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = uiState.currentSection == "TASKS",
                            onClick = { viewModel.setCurrentSection("TASKS") },
                            text = { Text("TASKS") }
                        )
                        Tab(
                            selected = uiState.currentSection == "STATUS",
                            onClick = { viewModel.setCurrentSection("STATUS") },
                            text = { Text("STATUS") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.currentSection == "TASKS") {
                        TasksSection(
                            uiState = uiState,
                            onNavigateToCreateTask = onNavigateToCreateTask,
                            onNavigateToTask = onNavigateToTask,
                            onNavigateToTaskList = onNavigateToTaskList,
                            projectId = projectId
                        )
                    } else {
                        StatusSection(
                            project = project,
                            currentUserRole = uiState.currentUser?.role,
                            onStartProject = { viewModel.startProject(project.id) },
                            onSubmitUpdate = { url, comment -> viewModel.submitStatusUpdateRequest(project.id, url, comment) },
                            onReviewUpdate = { approved, nextStatus, comment ->
                                viewModel.reviewStatusUpdateRequest(project.id, approved, nextStatus, comment)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Project") },
            text = { Text("Are you sure you want to delete this project? All associated tasks will also be deleted. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        uiState.currentProject?.let { viewModel.deleteProject(it, onNavigateBack) }
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

@Composable
fun TasksSection(
    uiState: ProjectUiState,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToTask: (String) -> Unit,
    onNavigateToTaskList: (String, String) -> Unit,
    projectId: String
) {
    val activeTasksCount = uiState.projectTasks.count { it.status != TaskStatus.DONE }
    val completedTasksCount = uiState.projectTasks.count { it.status == TaskStatus.DONE }
    val isProjectCompleted = uiState.currentProject?.status == ProjectStatus.COMPLETED

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.titleLarge
            )
            val role = uiState.currentUser?.role
            if (!isProjectCompleted && (role == UserRole.MANAGER || role == UserRole.TEAM_LEADER) && uiState.currentProject?.status != ProjectStatus.NOT_STARTED && uiState.currentProject?.status != ProjectStatus.ON_HOLD) {
                IconButton(onClick = onNavigateToCreateTask) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardStatCard(
                title = "Active Tasks",
                value = activeTasksCount.toString(),
                icon = Icons.Default.PendingActions,
                modifier = Modifier.weight(1f).clickable {
                    onNavigateToTaskList("Active", projectId)
                }
            )
            DashboardStatCard(
                title = "Completed Tasks",
                value = completedTasksCount.toString(),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f).clickable {
                    onNavigateToTaskList("Completed", projectId)
                },
                valueColor = SuccessGreen
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Recent Tasks",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.projectTasks.isEmpty()) {
            Text(
                "No tasks found for this project.",
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            uiState.projectTasks.take(5).forEach { task ->
                TaskCard(
                    task = task,
                    onClick = { onNavigateToTask(task.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun StatusSection(
    project: com.miniprojecttracker.domain.model.Project,
    currentUserRole: UserRole?,
    onStartProject: () -> Unit,
    onSubmitUpdate: (String, String) -> Unit,
    onReviewUpdate: (Boolean, ProjectStatus, String) -> Unit
) {
    var reviewComment by remember { mutableStateOf("") }
    var documentUrl by remember { mutableStateOf("") }
    var submissionComment by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Current Workflow Stage", style = MaterialTheme.typography.labelLarge)
                Text(project.status.displayName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    when (project.status) {
                        ProjectStatus.NOT_STARTED -> "Project is waiting to be started by Team Lead."
                        ProjectStatus.PLANNING -> "Project is in planning phase. Create tasks to begin."
                        ProjectStatus.ACTIVE -> "Project is currently active and tasks are being worked on."
                        ProjectStatus.COMPLETED -> "Project has been successfully completed."
                        ProjectStatus.ON_HOLD -> "Project is currently on hold."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (currentUserRole == UserRole.TEAM_LEADER) {
            val isHold = project.status == ProjectStatus.ON_HOLD
            if (project.status == ProjectStatus.NOT_STARTED) {
                Button(onClick = onStartProject, modifier = Modifier.fillMaxWidth()) {
                    Text("Start Project")
                }
            } else if (project.status != ProjectStatus.COMPLETED && project.updateRequestStatus != UpdateRequestStatus.PENDING) {
                Text("Request Status Update", style = MaterialTheme.typography.titleMedium)
                
                if (isHold) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Project is ON HOLD. You cannot submit updates until it is resumed.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = documentUrl,
                    onValueChange = { documentUrl = it },
                    label = { Text("Project Link / Documentation URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://...") },
                    enabled = !isHold
                )

                OutlinedTextField(
                    value = submissionComment,
                    onValueChange = { submissionComment = it },
                    label = { Text("Submission Comment") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describe the work done...") },
                    enabled = !isHold
                )
                
                val nextStatus = when (project.status) {
                    ProjectStatus.PLANNING -> ProjectStatus.ACTIVE
                    ProjectStatus.ACTIVE -> ProjectStatus.COMPLETED
                    else -> project.status
                }
                Button(
                    onClick = { onSubmitUpdate(documentUrl, submissionComment) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = documentUrl.isNotBlank() && !isHold
                ) {
                    Text(if (isHold) "Paused" else "Submit for Review to ${nextStatus.displayName}")
                }
            }
        }

        if (project.updateRequestStatus == UpdateRequestStatus.PENDING) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Update Request Pending", style = MaterialTheme.typography.titleMedium)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Link Provided:", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = project.documentUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(project.documentUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                    )
                    
                    if (project.submissionComment.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Team Lead Comment:", style = MaterialTheme.typography.labelMedium)
                        Text(project.submissionComment, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (currentUserRole == UserRole.MANAGER) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = reviewComment,
                            onValueChange = { reviewComment = it },
                            label = { Text("Review Comment") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val nextStatus = when (project.status) {
                                ProjectStatus.PLANNING -> ProjectStatus.ACTIVE
                                ProjectStatus.ACTIVE -> ProjectStatus.COMPLETED
                                else -> project.status
                            }
                            Button(
                                onClick = { onReviewUpdate(true, nextStatus, reviewComment) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve")
                            }
                            OutlinedButton(
                                onClick = { onReviewUpdate(false, project.status, reviewComment) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Waiting for Manager review...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (project.statusHistory.isNotEmpty()) {
            Text("Review History", style = MaterialTheme.typography.titleMedium)
            project.statusHistory.asReversed().forEach { update ->
                StatusUpdateItem(update)
            }
        }
    }
}

@Composable
fun StatusUpdateItem(update: ProjectStatusUpdate) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${update.fromStatus.displayName} → ${update.toStatus.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status = update.status.name)
            }
            Text(
                text = DateUtils.formatDateTime(update.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (update.documentUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = update.documentUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.documentUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                    )
                }
            }

            if (update.submissionComment.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("TL: ${update.submissionComment}", style = MaterialTheme.typography.bodySmall)
            }
            
            if (update.reviewComment.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Manager: ${update.reviewComment}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
