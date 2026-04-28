package com.miniprojecttracker.domain.model

/**
 * Defines the three access levels in the application.
 * Each role has different permissions and views.
 */
enum class UserRole {
    MANAGER,        // Can manage teams, projects, and assign managers (formerly SUPER_MANAGER)
    TEAM_LEADER,    // Can manage tasks within assigned projects (formerly MANAGER)
    DEVELOPER       // Can view and update assigned tasks
}
