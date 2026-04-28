package com.miniprojecttracker.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.Priority
import com.miniprojecttracker.domain.model.Project
import com.miniprojecttracker.domain.model.Task
import com.miniprojecttracker.domain.model.UserRole
import com.miniprojecttracker.ui.components.AppTopBar
import com.miniprojecttracker.ui.components.LoadingIndicator
import com.miniprojecttracker.ui.components.ProjectCard
import com.miniprojecttracker.ui.components.TaskCard
import com.miniprojecttracker.ui.theme.PriorityCritical
import com.miniprojecttracker.ui.theme.PriorityHigh
import com.miniprojecttracker.ui.theme.PriorityLow
import com.miniprojecttracker.ui.theme.PriorityMedium
import java.util.*

@Composable
fun CalendarScreen(
    onNavigateToTask: (String) -> Unit,
    onNavigateToProject: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Setup Calendar
    var currentCal by remember { mutableStateOf(Calendar.getInstance()) }
    val daysInMonth = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = remember(currentCal) {
        val cal = currentCal.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed offset (Sun = 0)
    }

    val year = currentCal.get(Calendar.YEAR)
    val month = currentCal.get(Calendar.MONTH) + 1

    val minCal = remember { Calendar.getInstance().apply { add(Calendar.YEAR, -1) } }
    val maxCal = remember { Calendar.getInstance().apply { add(Calendar.YEAR, 1) } }

    val canGoBack = currentCal.after(minCal)
    val canGoForward = currentCal.before(maxCal)

    Scaffold(
        topBar = { AppTopBar(title = "Calendar", onNavigateBack = onNavigateBack) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // View Type Toggle
                if (uiState.userRole == UserRole.TEAM_LEADER) {
                    TabRow(
                        selectedTabIndex = if (uiState.viewType == CalendarViewType.TASKS) 0 else 1,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Tab(
                            selected = uiState.viewType == CalendarViewType.TASKS,
                            onClick = { viewModel.setViewType(CalendarViewType.TASKS) },
                            text = { Text("Tasks") }
                        )
                        Tab(
                            selected = uiState.viewType == CalendarViewType.PROJECTS,
                            onClick = { viewModel.setViewType(CalendarViewType.PROJECTS) },
                            text = { Text("Projects") }
                        )
                    }
                }

                // Month Header with Navigation
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val next = (currentCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                            if (next.after(minCal)) currentCal = next
                        },
                        enabled = canGoBack
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }

                    Text(
                        text = "${currentCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} $year",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    IconButton(
                        onClick = {
                            val next = (currentCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                            if (next.before(maxCal)) currentCal = next
                        },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }

                // Day of Week Headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                ) {
                    // Empty cells before start of month
                    items(firstDayOfMonth) {
                        Box(modifier = Modifier.aspectRatio(1f).padding(2.dp))
                    }
                    
                    // Actual days
                    items(daysInMonth) { dayZeroIndexed ->
                        val day = dayZeroIndexed + 1
                        val dateKey = String.format("%04d-%02d-%02d", year, month, day)
                        val itemsForDay = uiState.itemsByDate[dateKey] ?: emptyList()
                        val isSelected = uiState.selectedDateKey == dateKey
                        val isToday = day == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) && 
                                      month == Calendar.getInstance().get(Calendar.MONTH) + 1 &&
                                      year == Calendar.getInstance().get(Calendar.YEAR)
                        
                        val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isToday) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(containerColor)
                                .clickable {
                                    val cal = Calendar.getInstance().apply { set(year, month-1, day) }
                                    viewModel.selectDate(dateKey, cal.timeInMillis)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColor,
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                // Indicators
                                if (itemsForDay.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.Center) {
                                        itemsForDay.take(3).forEach { item ->
                                            val color = if (item is Task) {
                                                when(item.priority) {
                                                    Priority.CRITICAL -> PriorityCritical
                                                    Priority.HIGH -> PriorityHigh
                                                    Priority.MEDIUM -> PriorityMedium
                                                    Priority.LOW -> PriorityLow
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .padding(horizontal = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Items for selected date
                Text(
                    text = if (uiState.selectedDateKey.isNotBlank()) {
                        "${if (uiState.viewType == CalendarViewType.TASKS) "Tasks" else "Projects"} for ${uiState.selectedDateKey}"
                    } else "Select a date",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (uiState.selectedItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No ${if (uiState.viewType == CalendarViewType.TASKS) "tasks" else "projects"} due on this date.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.selectedItems) { item ->
                            if (item is Task) {
                                TaskCard(task = item, onClick = { onNavigateToTask(item.id) })
                            } else if (item is Project) {
                                ProjectCard(project = item, onClick = { onNavigateToProject(item.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
