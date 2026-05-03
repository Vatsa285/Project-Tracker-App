package com.miniprojecttracker.util

import com.miniprojecttracker.domain.model.Priority

/**
 * App-wide constants.
 */
object Constants {
    // === Gamification Points ===

    // Base points for completing a task
    const val POINTS_TASK_COMPLETED = 10

    // Bonus for completing before the due date
    const val POINTS_TASK_ON_TIME = 5

    // Points for adding a comment (engagement bonus)
    const val POINTS_COMMENT_ADDED = 2

    /**
     * Multiplier based on task priority.
     * Higher priority tasks reward more points.
     */
    fun getTaskPriorityMultiplier(priority: Priority): Float = when (priority) {
        Priority.LOW -> 1.0f
        Priority.MEDIUM -> 1.5f
        Priority.HIGH -> 2.0f
        Priority.CRITICAL -> 3.0f
    }

    /**
     * Bonus points based on the parent project's priority.
     * Encourages developers to focus on high-priority projects.
     */
    fun getProjectPriorityBonus(priority: Priority): Int = when (priority) {
        Priority.LOW -> 0
        Priority.MEDIUM -> 5
        Priority.HIGH -> 10
        Priority.CRITICAL -> 20
    }

    // Notification channels
    const val NOTIFICATION_CHANNEL_ID = "project_tracker_notifications"
    const val NOTIFICATION_CHANNEL_NAME = "Project Tracker"
}
