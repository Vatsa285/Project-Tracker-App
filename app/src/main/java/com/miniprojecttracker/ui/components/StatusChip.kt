package com.miniprojecttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.miniprojecttracker.domain.model.ProjectStatus
import com.miniprojecttracker.domain.model.TaskStatus
import com.miniprojecttracker.ui.theme.StatusDone
import com.miniprojecttracker.ui.theme.StatusInProgress
import com.miniprojecttracker.ui.theme.StatusTodo

@Composable
fun StatusChip(
    status: Any, // Can be TaskStatus or ProjectStatus
    modifier: Modifier = Modifier
) {
    val (text, color) = when (status) {
        is TaskStatus -> {
            when (status) {
                TaskStatus.TODO -> status.displayName to StatusTodo
                TaskStatus.IN_PROGRESS -> status.displayName to StatusInProgress
                TaskStatus.DONE -> status.displayName to StatusDone
            }
        }
        is ProjectStatus -> {
             when (status) {
                 ProjectStatus.NOT_STARTED -> status.displayName to Color.LightGray
                 ProjectStatus.PLANNING -> status.displayName to StatusTodo
                 ProjectStatus.ACTIVE -> status.displayName to StatusInProgress
                 ProjectStatus.COMPLETED -> status.displayName to StatusDone
                 ProjectStatus.ON_HOLD -> status.displayName to Color.Gray
             }
        }
        else -> "Unknown" to Color.Gray
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
