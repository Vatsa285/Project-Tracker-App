package com.miniprojecttracker.domain.model

/**
 * Task lifecycle status. Follows a linear progression:
 * TODO → IN_PROGRESS → DONE
 */
enum class TaskStatus(val displayName: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done")
}
