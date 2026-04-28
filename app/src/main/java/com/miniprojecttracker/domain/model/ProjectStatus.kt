package com.miniprojecttracker.domain.model

/**
 * Overall status of a project.
 */
enum class ProjectStatus(val displayName: String) {
    NOT_STARTED("Not Started"),
    PLANNING("Planning"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    ON_HOLD("On Hold")
}
