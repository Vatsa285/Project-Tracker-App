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
import com.miniprojecttracker.domain.model.Priority
import com.miniprojecttracker.ui.theme.PriorityCritical
import com.miniprojecttracker.ui.theme.PriorityHigh
import com.miniprojecttracker.ui.theme.PriorityLow
import com.miniprojecttracker.ui.theme.PriorityMedium

@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    val color = when (priority) {
        Priority.LOW -> PriorityLow
        Priority.MEDIUM -> PriorityMedium
        Priority.HIGH -> PriorityHigh
        Priority.CRITICAL -> PriorityCritical
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = priority.displayName,
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
