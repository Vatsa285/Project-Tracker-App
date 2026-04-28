package com.miniprojecttracker.ui.leaderboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.miniprojecttracker.domain.model.UserRole
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miniprojecttracker.domain.model.User
import com.miniprojecttracker.ui.components.AppTopBar
import com.miniprojecttracker.ui.components.LoadingIndicator

@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { 
            AppTopBar(
                title = "Leaderboard", 
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.currentUser?.role == UserRole.MANAGER) {
                        IconButton(onClick = { viewModel.resetLeaderboard() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Leaderboard")
                        }
                    }
                }
            ) 
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top 3 Podium
                if (uiState.topUsers.size >= 3) {
                    PodiumSection(top3 = uiState.topUsers.take(3))
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Rest of the list
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val listUsers = if (uiState.topUsers.size >= 3) uiState.topUsers.drop(3) else uiState.topUsers
                    val startIndex = if (uiState.topUsers.size >= 3) 3 else 0
                    
                    itemsIndexed(listUsers) { index, user ->
                        LeaderboardItem(rank = startIndex + index + 1, user = user)
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumSection(top3: List<User>) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val scale1 by animateFloatAsState(if (isVisible) 1f else 0f, tween(500, delayMillis = 300))
    val scale2 by animateFloatAsState(if (isVisible) 1f else 0f, tween(500, delayMillis = 100))
    val scale3 by animateFloatAsState(if (isVisible) 1f else 0f, tween(500, delayMillis = 500))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 32.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place
        if (top3.size > 1) {
            PodiumItem(user = top3[1], rank = 2, height = 120.dp, color = Color(0xFFE0E0E0), modifier = Modifier.scale(scale2))
        }
        // 1st Place
        if (top3.isNotEmpty()) {
            PodiumItem(user = top3[0], rank = 1, height = 160.dp, color = Color(0xFFFFD700), modifier = Modifier.scale(scale1))
        }
        // 3rd Place
        if (top3.size > 2) {
            PodiumItem(user = top3[2], rank = 3, height = 90.dp, color = Color(0xFFCD7F32), modifier = Modifier.scale(scale3))
        }
    }
}

@Composable
fun PodiumItem(user: User, rank: Int, height: androidx.compose.ui.unit.Dp, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Icon/Avatar replacement
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(height)
                .clip(MaterialTheme.shapes.medium)
                .background(color),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
                Text(rank.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(user.name.split(" ").firstOrNull() ?: "", style = MaterialTheme.typography.labelMedium, color = Color.Black)
                Text("${user.points} pts", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun LeaderboardItem(rank: Int, user: User) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(32.dp)
            )
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(user.name.firstOrNull()?.uppercase() ?: "?", color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(user.role.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "Points", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${user.points} pts", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
