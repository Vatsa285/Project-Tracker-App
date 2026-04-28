package com.miniprojecttracker.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.TaskStatus
import com.miniprojecttracker.ui.components.AppTopBar
import com.miniprojecttracker.ui.components.BarChart
import com.miniprojecttracker.ui.components.LoadingIndicator
import com.miniprojecttracker.ui.components.PieChart
import com.miniprojecttracker.ui.theme.*

@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { AppTopBar(title = "Analytics Dashboard", onNavigateBack = onNavigateBack) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Smart Insights
                SmartInsightsSection(insights = uiState.smartInsights)
                
                Spacer(modifier = Modifier.height(24.dp))

                // Task Status Distribution (Pie Chart)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Task Status Distribution", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val pieColors = mapOf(
                            TaskStatus.TODO.name to StatusTodo,
                            TaskStatus.IN_PROGRESS.name to StatusInProgress,
                            TaskStatus.DONE.name to StatusDone
                        )
                        
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            PieChart(
                                data = uiState.taskStatusCounts,
                                colors = pieColors,
                                modifier = Modifier.size(200.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Legend
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            pieColors.forEach { (key, color) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = color, shape = MaterialTheme.shapes.small, modifier = Modifier.size(12.dp)) {}
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(key, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Top Performers (Bar Chart)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top Performers (Completed Tasks)", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            BarChart(
                                data = uiState.userCompletedTasks,
                                defaultColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Simple Legend for Bar Chart
                        uiState.userCompletedTasks.keys.forEachIndexed { index, name ->
                            Text("${index + 1}. $name", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SmartInsightsSection(insights: List<String>) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Smart Insights", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        insights.forEach { insight ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = WarningAmber)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(insight, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
