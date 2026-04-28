package com.miniprojecttracker.ui.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.Comment
import com.miniprojecttracker.domain.model.TaskStatus
import com.miniprojecttracker.domain.model.TaskUpdate
import com.miniprojecttracker.domain.model.UpdateRequestStatus
import com.miniprojecttracker.domain.model.UserRole
import com.miniprojecttracker.ui.components.AppTopBar
import com.miniprojecttracker.ui.components.ErrorDialog
import com.miniprojecttracker.ui.components.LoadingIndicator
import com.miniprojecttracker.ui.components.PriorityBadge
import com.miniprojecttracker.ui.components.StatusChip
import com.miniprojecttracker.util.DateUtils

@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateToEditTask: (String) -> Unit, // passes projectId
    onNavigateBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusBottomSheet by remember { mutableStateOf(false) }
    var showReviewRequestDialog by remember { mutableStateOf(false) }
    var reviewComment by remember { mutableStateOf("") }

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!, onDismiss = { viewModel.clearError() })
    }

    val isManager = uiState.currentUser?.role == UserRole.MANAGER
    val isTeamLeader = uiState.currentUser?.role == UserRole.TEAM_LEADER
    val isAssignedDeveloper = uiState.currentUser?.role == UserRole.DEVELOPER && uiState.currentTask?.assignedTo == uiState.currentUser?.id
    val isProjectCompleted = uiState.currentProject?.status == com.miniprojecttracker.domain.model.ProjectStatus.COMPLETED
    val isProjectOnHold = uiState.currentProject?.status == com.miniprojecttracker.domain.model.ProjectStatus.ON_HOLD
    val canEdit = (isManager || isTeamLeader) && !isProjectCompleted && !isProjectOnHold
    val uriHandler = LocalUriHandler.current

    var showCommentsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Task Details",
                onNavigateBack = onNavigateBack,
                actions = {
                    val currentTask = uiState.currentTask
                    if (canEdit && currentTask != null && currentTask.status != TaskStatus.DONE) {
                        IconButton(onClick = { 
                            onNavigateToEditTask(currentTask.projectId)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Task")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.currentTask == null) {
            LoadingIndicator()
        } else if (uiState.currentTask != null) {
            val task = uiState.currentTask!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Task Content (Scrollable)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(status = task.status)
                            PriorityBadge(priority = task.priority)
                            if (task.isBlocker) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("BLOCKER", modifier = Modifier.padding(4.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (task.updateRequestStatus != UpdateRequestStatus.NONE) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when (task.updateRequestStatus) {
                                        UpdateRequestStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                                        UpdateRequestStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Update Request: ${task.updateRequestStatus}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (task.documentUrl.isNotBlank()) {
                                        Text(
                                            text = "Document: ${task.documentUrl}",
                                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                try {
                                                    val url = task.documentUrl
                                                    uriHandler.openUri(if (url.startsWith("http")) url else "https://$url")
                                                } catch (e: Exception) {}
                                            }
                                        )
                                    }
                                    if (task.reviewComment.isNotBlank()) {
                                        Text("Reviewer Comment: ${task.reviewComment}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        if (isAssignedDeveloper && !isProjectOnHold) {
                            when (task.status) {
                                TaskStatus.TODO -> {
                                    Button(
                                        onClick = { viewModel.updateTaskStatus(task.id, TaskStatus.IN_PROGRESS) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                    ) {
                                        Text("Start Task")
                                    }
                                }
                                TaskStatus.IN_PROGRESS -> {
                                    if (task.updateRequestStatus != UpdateRequestStatus.PENDING) {
                                        Button(
                                            onClick = { viewModel.submitUpdateRequest(task.id, "", "") },
                                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                        ) {
                                            Text("Submit for Review")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { /* Already pending */ },
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                        ) {
                                            Text("Review Pending")
                                        }
                                    }
                                }
                                else -> {}
                            }
                        } else if (isAssignedDeveloper && isProjectOnHold) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Text(
                                    "Project is currently ON HOLD. Actions are disabled.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        if (isTeamLeader && task.updateRequestStatus == UpdateRequestStatus.PENDING) {
                            Button(
                                onClick = { showReviewRequestDialog = true },
                                modifier = Modifier.padding(top = 16.dp),
                                enabled = !isProjectOnHold
                            ) {
                                Text("Review Update Request")
                            }
                        }

                        if (task.updateHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Update History & Past Issues", style = MaterialTheme.typography.titleMedium)
                            task.updateHistory.asReversed().forEach { update ->
                                HistoryItem(update)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Text("Assignee", style = MaterialTheme.typography.labelMedium)
                                Text(task.assignedToName.ifBlank { "Unassigned" }, style = MaterialTheme.typography.bodyLarge)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Due Date", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = DateUtils.formatDate(task.dueDate), 
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (DateUtils.isOverdue(task.dueDate) && task.status != TaskStatus.DONE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        TextButton(
                            onClick = { showCommentsDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View All Comments (${uiState.comments.size})")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Comment Input Area (Fixed at bottom)
                if (!isProjectCompleted && !isProjectOnHold) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("Add a comment...") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    viewModel.addComment(taskId, commentText)
                                    commentText = ""
                                }),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    viewModel.addComment(taskId, commentText)
                                    commentText = ""
                                },
                                enabled = commentText.isNotBlank()
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send Comment", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else if (isProjectOnHold) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Commenting is disabled while project is on hold", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            // Status update removed as per requirement
            /*
            if (showStatusBottomSheet) {
                ...
            }
            */
        }
    }

    if (showCommentsDialog) {
        AlertDialog(
            onDismissRequest = { showCommentsDialog = false },
            title = { Text("Comments & Activity") },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (uiState.comments.isEmpty()) {
                        Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(uiState.comments) { comment ->
                                CommentItem(comment)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCommentsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        uiState.currentTask?.let { viewModel.deleteTask(it, onNavigateBack) }
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

    if (showReviewRequestDialog) {
        AlertDialog(
            onDismissRequest = { showReviewRequestDialog = false },
            title = { Text("Review Update Request") },
            text = {
                Column {
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("Review Comment / Issues") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.reviewUpdateRequest(taskId, true, reviewComment)
                            showReviewRequestDialog = false
                        }
                    ) {
                        Text("Approve")
                    }
                    TextButton(
                        onClick = {
                            viewModel.reviewUpdateRequest(taskId, false, reviewComment)
                            showReviewRequestDialog = false
                        }
                    ) {
                        Text("Raise Issue", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewRequestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryItem(update: TaskUpdate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(DateUtils.getRelativeTime(update.timestamp), style = MaterialTheme.typography.labelSmall)
                StatusChip(statusString = update.status.name)
            }
            if (update.documentUrl.isNotBlank()) {
                val uriHandler = LocalUriHandler.current
                Text(
                    text = "Document: ${update.documentUrl}",
                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        try {
                            val url = update.documentUrl
                            val intentUrl = if (url.startsWith("http")) url else "https://$url"
                            uriHandler.openUri(intentUrl)
                        } catch (e: Exception) {}
                    }
                )
            }
            if (update.comment.isNotBlank()) {
                Text("Developer Comment: ${update.comment}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun StatusChip(statusString: String) {
    val (color, text) = when (statusString) {
        "APPROVED" -> MaterialTheme.colorScheme.primary to "Approved"
        "REJECTED" -> MaterialTheme.colorScheme.error to "Rejected"
        "PENDING" -> MaterialTheme.colorScheme.secondary to "Pending"
        else -> MaterialTheme.colorScheme.outline to statusString
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(comment.userName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(DateUtils.getRelativeTime(comment.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
