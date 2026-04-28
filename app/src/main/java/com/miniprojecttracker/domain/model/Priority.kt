package com.miniprojecttracker.domain.model

/**
 * Priority levels for tasks and projects.
 * Used for visual color-coding and sorting.
 */
enum class Priority(val displayName: String, val level: Int) {
    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2),
    CRITICAL("Critical", 3)
}
