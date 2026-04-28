package com.miniprojecttracker.domain.model

/**
 * Domain model representing a project assigned to a team.
 * Progress is auto-calculated from task completion percentage.
 */
data class Project(
    val id: String = "",
    val teamId: String = "",
    val name: String = "",
    val description: String = "",
    val deadline: Long = 0L,
    val priority: Priority = Priority.MEDIUM,
    val status: ProjectStatus = ProjectStatus.NOT_STARTED,
    val progress: Float = 0f, // 0.0 to 1.0
    val managerId: String = "", // Added for data isolation
    val createdAt: Long = System.currentTimeMillis(),
    
    // Status update request fields
    val documentUrl: String = "",
    val updateRequestStatus: UpdateRequestStatus = UpdateRequestStatus.NONE,
    val submissionComment: String = "",
    val reviewComment: String = "",
    val previousStatusBeforeHold: ProjectStatus? = null,
    val statusHistory: List<ProjectStatusUpdate> = emptyList()
)

data class ProjectStatusUpdate(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fromStatus: ProjectStatus = ProjectStatus.NOT_STARTED,
    val toStatus: ProjectStatus = ProjectStatus.NOT_STARTED,
    val documentUrl: String = "",
    val submissionComment: String = "",
    val reviewComment: String = "",
    val status: UpdateRequestStatus = UpdateRequestStatus.NONE
)
