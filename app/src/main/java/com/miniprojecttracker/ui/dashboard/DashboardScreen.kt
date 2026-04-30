package com.miniprojecttracker.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.Project
import com.miniprojecttracker.domain.model.ProjectStatus
import com.miniprojecttracker.domain.model.Task
import com.miniprojecttracker.domain.model.UserRole
import com.miniprojecttracker.ui.components.*
import com.miniprojecttracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToProjects: (String?, Boolean) -> Unit,
    onNavigateToProject: (String) -> Unit,
    onNavigateToTeams: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCreateProject: () -> Unit,
    onNavigateToCreateTeam: () -> Unit,
    onNavigateToTask: (String) -> Unit,
    onNavigateToTaskList: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Dashboard",
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            // Manager role (formerly SUPER_MANAGER) no longer has 'add' option on home page as per requirement
        if (uiState.currentUser?.role == UserRole.MANAGER) {
            FloatingActionButton(onClick = onNavigateToCreateProject) {
                Icon(Icons.Default.Add, contentDescription = "Create Project")
            }
        }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator()
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                uiState.currentUser?.let { user ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            WelcomeSection(userName = user.name, role = user.role)
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        when (user.role) {
                            UserRole.MANAGER -> {
                                item { 
                                    ManagerDashboard(
                                        uiState = uiState,
                                        onTeamsClick = onNavigateToTeams,
                                        onProjectsClick = onNavigateToProjects,
                                        onAnalyticsClick = onNavigateToAnalytics,
                                        onCalendarClick = onNavigateToCalendar
                                    )
                                }
                            }
                            UserRole.TEAM_LEADER -> {
                                item { 
                                    TeamLeaderDashboard(
                                        uiState = uiState,
                                        onProjectClick = onNavigateToProject,
                                        onCalendarClick = onNavigateToCalendar,
                                        onTeamsClick = onNavigateToTeams,
                                        onNavigateToProjects = onNavigateToProjects,
                                        onTeamSelected = { viewModel.selectTeam(it) }
                                    )
                                }
                            }
                            UserRole.DEVELOPER -> {
                                item { 
                                    DeveloperDashboard(
                                        uiState = uiState,
                                        onLeaderboardClick = onNavigateToLeaderboard,
                                        onTaskClick = onNavigateToTask,
                                        onTaskListClick = onNavigateToTaskList
                                    ) 
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeSection(userName: String, role: com.miniprojecttracker.domain.model.UserRole) {
    Column {
        Text(
            text = "Welcome back,",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        StatusChip(status = role)
    }
}

@Composable
fun ManagerDashboard(
    uiState: DashboardUiState,
    onTeamsClick: () -> Unit,
    onProjectsClick: (String?, Boolean) -> Unit,
    onAnalyticsClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    Column {
        Text(
            text = "Manager Dashboard",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardStatCard(
                title = "Total Teams",
                value = uiState.totalTeams.toString(),
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f).clickable { onTeamsClick() }
            )
            DashboardStatCard(
                title = "Active Projects",
                value = uiState.activeProjectsCount.toString(),
                icon = Icons.Default.Assignment,
                modifier = Modifier.weight(1f).clickable { onProjectsClick("Active", false) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardStatCard(
                title = "Upcoming Projects",
                value = uiState.upcomingProjectsCount.toString(),
                icon = Icons.Default.AccessTime,
                modifier = Modifier.weight(1f).clickable { onProjectsClick("Not Started", false) },
                valueColor = MaterialTheme.colorScheme.primary
            )
            DashboardStatCard(
                title = "Analytics",
                value = "View",
                icon = Icons.Default.PieChart,
                modifier = Modifier.weight(1f).clickable { onAnalyticsClick() },
                valueColor = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardStatCard(
                title = "Calendar View",
                value = "View",
                icon = Icons.Default.DateRange,
                modifier = Modifier.weight(1f).clickable { onCalendarClick() },
                valueColor = MaterialTheme.colorScheme.primary
            )
            DashboardStatCard(
                title = "All Projects",
                value = "List",
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                modifier = Modifier.weight(1f).clickable { onProjectsClick("All", true) },
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamLeaderDashboard(
    uiState: DashboardUiState,
    onProjectClick: (String) -> Unit,
    onCalendarClick: () -> Unit,
    onTeamsClick: () -> Unit,
    onNavigateToProjects: (String?, Boolean) -> Unit,
    onTeamSelected: (String) -> Unit
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val selectedTeamName = uiState.teams.find { it.id == uiState.selectedTeamId }?.name ?: "Select Team"

    Column {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            OutlinedCard(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Current Team", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(text = selectedTeamName, style = MaterialTheme.typography.titleMedium)
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                uiState.teams.forEach { team ->
                    DropdownMenuItem(
                        text = { Text(team.name) },
                        onClick = {
                            onTeamSelected(team.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = "Team Leader Dashboard",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardStatCard(
                title = "Active Projects",
                value = uiState.activeProjectsCount.toString(),
                icon = Icons.Default.Assignment,
                modifier = Modifier.weight(1f).clickable { onNavigateToProjects("Active", false) }
            )
            DashboardStatCard(
                title = "Completed",
                value = uiState.completedProjectsCount.toString(),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f).clickable { onNavigateToProjects("Completed", false) },
                valueColor = SuccessGreen
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardStatCard(
                title = "New Projects",
                value = uiState.upcomingProjectsCount.toString(),
                icon = Icons.Default.FiberNew,
                modifier = Modifier.weight(1f).clickable { onNavigateToProjects("Not Started", false) }
            )
            DashboardStatCard(
                title = "Teams Managed",
                value = uiState.totalManagedTeams.toString(),
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f).clickable { onTeamsClick() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardStatCard(
                title = "Calendar View",
                value = "View",
                icon = Icons.Default.DateRange,
                modifier = Modifier.weight(1f).clickable { onCalendarClick() },
                valueColor = MaterialTheme.colorScheme.primary
            )
            DashboardStatCard(
                title = "All Projects",
                value = "List",
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                modifier = Modifier.weight(1f).clickable { onNavigateToProjects("All", true) },
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
        
        /* 
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Recent Projects",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (uiState.projects.isEmpty()) {
            Text("No projects assigned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            uiState.projects.take(3).forEach { project ->
                ProjectCard(project = project, onClick = { onProjectClick(project.id) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        */
    }
}

@Composable
fun DeveloperDashboard(
    uiState: DashboardUiState,
    onLeaderboardClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onTaskListClick: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardStatCard(
                title = "Pending Tasks",
                value = uiState.pendingTasksCount.toString(),
                icon = Icons.Default.PendingActions,
                modifier = Modifier.weight(1f).clickable { onTaskListClick("PENDING") }
            )
            DashboardStatCard(
                title = "Completed",
                value = uiState.completedTasksCount.toString(),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f).clickable { onTaskListClick("COMPLETED") },
                valueColor = SuccessGreen
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardStatCard(
                title = "Overdue",
                value = uiState.overdueTasksCount.toString(),
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f).clickable { onTaskListClick("OVERDUE") },
                valueColor = if (uiState.overdueTasksCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
             DashboardStatCard(
                title = "Leaderboard",
                value = "View",
                icon = Icons.Default.EmojiEvents,
                modifier = Modifier.weight(1f).clickable { onLeaderboardClick() },
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Your Tasks",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (uiState.tasks.isEmpty()) {
            Text("No tasks assigned to you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
             Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.tasks.take(5).forEach { task ->
                    TaskCard(task = task, onClick = { onTaskClick(task.id) })
                }
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = valueColor
            )
        }
    }
}
