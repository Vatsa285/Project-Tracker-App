package com.miniprojecttracker.ui.navigation

/**
 * Sealed class defining all navigation destinations.
 * Uses string routes for Jetpack Navigation Compose.
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object Dashboard : Screen("dashboard")
    data object ProjectList : Screen("projects?filter={filter}&showFilters={showFilters}") {
        fun createRoute(filter: String? = null, showFilters: Boolean = true) =
            "projects?filter=${filter ?: "All"}&showFilters=$showFilters"
    }
    data object ProjectDetail : Screen("project/{projectId}") {
        fun createRoute(projectId: String) = "project/$projectId"
    }
    data object CreateEditProject : Screen("project/edit?projectId={projectId}") {
        fun createRoute(projectId: String? = null) =
            if (projectId != null) "project/edit?projectId=$projectId" else "project/edit"
    }
    data object TaskDetail : Screen("task/{taskId}") {
        fun createRoute(taskId: String) = "task/$taskId"
    }
    data object TaskList : Screen("tasks?filter={filter}&projectId={projectId}") {
        fun createRoute(filter: String? = null, projectId: String? = null): String {
            val builder = StringBuilder("tasks")
            val params = mutableListOf<String>()
            filter?.let { params.add("filter=$it") }
            projectId?.let { params.add("projectId=$it") }
            if (params.isNotEmpty()) {
                builder.append("?").append(params.joinToString("&"))
            }
            return builder.toString()
        }
    }
    data object CreateEditTask : Screen("task/edit?projectId={projectId}&taskId={taskId}") {
        fun createRoute(projectId: String, taskId: String? = null) =
            if (taskId != null) "task/edit?projectId=$projectId&taskId=$taskId"
            else "task/edit?projectId=$projectId"
    }
    data object KanbanBoard : Screen("kanban/{projectId}") {
        fun createRoute(projectId: String) = "kanban/$projectId"
    }
    data object TeamManagement : Screen("teams")
    data object CreateEditTeam : Screen("team/edit?teamId={teamId}") {
        fun createRoute(teamId: String? = null) =
            if (teamId != null) "team/edit?teamId=$teamId" else "team/edit"
    }
    data object Analytics : Screen("analytics")
    data object Calendar : Screen("calendar")
    data object Leaderboard : Screen("leaderboard")
    data object Settings : Screen("settings")
}
