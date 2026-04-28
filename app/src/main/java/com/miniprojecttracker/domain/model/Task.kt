package com.miniprojecttracker.domain.model

/**
 * Domain model representing a task within a project.
 * Tasks follow a kanban-style workflow: TODO → IN_PROGRESS → DONE.
 */
data class Task(
    val id: String = "",
    val projectId: String = "",
    val assignedTo: String = "",       // User ID of the assigned developer
    val assignedToName: String = "",   // Cached display name
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Long = 0L,
    val isBlocker: Boolean = false,    // Flag for blocker/issue
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L,
    
    // New fields for Update Requests
    val documentUrl: String = "",
    val updateRequestStatus: UpdateRequestStatus = UpdateRequestStatus.NONE,
    val reviewComment: String = "",    // Issues raised by team lead
    val updateHistory: List<TaskUpdate> = emptyList()
)

enum class UpdateRequestStatus {
    NONE,
    PENDING,
    APPROVED,
    REJECTED
}

data class TaskUpdate(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val documentUrl: String = "",
    val comment: String = "",
    val status: UpdateRequestStatus = UpdateRequestStatus.NONE
)
