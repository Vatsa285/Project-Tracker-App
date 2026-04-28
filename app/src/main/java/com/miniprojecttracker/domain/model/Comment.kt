package com.miniprojecttracker.domain.model

/**
 * Domain model representing a comment on a task.
 * Used for in-app communication per task.
 */
data class Comment(
    val id: String = "",
    val taskId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
