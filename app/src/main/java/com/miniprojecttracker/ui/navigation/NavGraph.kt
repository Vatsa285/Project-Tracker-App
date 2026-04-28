package com.miniprojecttracker.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.miniprojecttracker.ui.analytics.AnalyticsScreen
import com.miniprojecttracker.ui.auth.LoginScreen
import com.miniprojecttracker.ui.auth.SignupScreen
import com.miniprojecttracker.ui.calendar.CalendarScreen
import com.miniprojecttracker.ui.dashboard.DashboardScreen
import com.miniprojecttracker.ui.kanban.KanbanBoardScreen
import com.miniprojecttracker.ui.leaderboard.LeaderboardScreen
import com.miniprojecttracker.ui.project.CreateEditProjectScreen
import com.miniprojecttracker.ui.project.ProjectDetailScreen
import com.miniprojecttracker.ui.project.ProjectListScreen
import com.miniprojecttracker.ui.settings.SettingsScreen
import com.miniprojecttracker.ui.task.CreateEditTaskScreen
import com.miniprojecttracker.ui.task.TaskDetailScreen
import com.miniprojecttracker.ui.task.TaskListScreen
import com.miniprojecttracker.ui.team.CreateEditTeamScreen
import com.miniprojecttracker.ui.team.TeamManagementScreen

/**
 * Central navigation graph for the entire application.
 * Start destination depends on auth state, determined in MainActivity.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { 100 }) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { -100 }) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { 100 }) }
    ) {
        // === AUTH ===
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignupSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                }
            )
        }

        // === DASHBOARD ===
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToProjects = { filter, showFilters -> navController.navigate(Screen.ProjectList.createRoute(filter, showFilters)) },
                onNavigateToProject = { navController.navigate(Screen.ProjectDetail.createRoute(it)) },
                onNavigateToTeams = { navController.navigate(Screen.TeamManagement.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToCreateProject = { navController.navigate(Screen.CreateEditProject.createRoute()) },
                onNavigateToCreateTeam = { navController.navigate(Screen.CreateEditTeam.createRoute()) },
                onNavigateToTask = { navController.navigate(Screen.TaskDetail.createRoute(it)) },
                onNavigateToTaskList = { filter -> navController.navigate(Screen.TaskList.createRoute(filter)) }
            )
        }

        // === PROJECTS ===
        composable(
            route = Screen.ProjectList.route,
            arguments = listOf(
                navArgument("filter") { type = NavType.StringType; defaultValue = "All" },
                navArgument("showFilters") { type = NavType.BoolType; defaultValue = true }
            )
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter") ?: "All"
            val showFilters = backStackEntry.arguments?.getBoolean("showFilters") ?: true
            ProjectListScreen(
                initialFilter = filter,
                showFilters = showFilters,
                onNavigateToProject = { navController.navigate(Screen.ProjectDetail.createRoute(it)) },
                onNavigateToCreateProject = { navController.navigate(Screen.CreateEditProject.createRoute()) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ProjectDetail.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProjectDetailScreen(
                projectId = projectId,
                onNavigateToTask = { navController.navigate(Screen.TaskDetail.createRoute(it)) },
                onNavigateToTaskList = { filter, pId -> 
                    navController.navigate(Screen.TaskList.createRoute(filter, pId)) 
                },
                onNavigateToCreateTask = { navController.navigate(Screen.CreateEditTask.createRoute(projectId)) },
                onNavigateToKanban = { navController.navigate(Screen.KanbanBoard.createRoute(projectId)) },
                onNavigateToEditProject = { navController.navigate(Screen.CreateEditProject.createRoute(projectId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CreateEditProject.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.takeIf { it.isNotBlank() }
            CreateEditProjectScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // === TASKS ===
        composable(
            route = Screen.TaskList.route,
            arguments = listOf(
                navArgument("filter") { type = NavType.StringType; defaultValue = "ALL" },
                navArgument("projectId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter") ?: "ALL"
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            TaskListScreen(
                filter = filter,
                projectId = projectId,
                onNavigateToTask = { navController.navigate(Screen.TaskDetail.createRoute(it)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskDetailScreen(
                taskId = taskId,
                onNavigateToEditTask = { projectId ->
                    navController.navigate(Screen.CreateEditTask.createRoute(projectId, taskId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CreateEditTask.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType; defaultValue = "" },
                navArgument("taskId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val taskId = backStackEntry.arguments?.getString("taskId")?.takeIf { it.isNotBlank() }
            CreateEditTaskScreen(
                projectId = projectId,
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // === KANBAN ===
        composable(
            route = Screen.KanbanBoard.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            KanbanBoardScreen(
                projectId = projectId,
                onNavigateToTask = { navController.navigate(Screen.TaskDetail.createRoute(it)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // === TEAMS ===
        composable(Screen.TeamManagement.route) {
            TeamManagementScreen(
                onNavigateToCreateTeam = { navController.navigate(Screen.CreateEditTeam.createRoute()) },
                onNavigateToEditTeam = { navController.navigate(Screen.CreateEditTeam.createRoute(it)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CreateEditTeam.route,
            arguments = listOf(navArgument("teamId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId")?.takeIf { it.isNotBlank() }
            CreateEditTeamScreen(
                teamId = teamId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // === ANALYTICS, CALENDAR, LEADERBOARD, SETTINGS ===
        composable(Screen.Analytics.route) {
            AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateToTask = { navController.navigate(Screen.TaskDetail.createRoute(it)) },
                onNavigateToProject = { navController.navigate(Screen.ProjectDetail.createRoute(it)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
