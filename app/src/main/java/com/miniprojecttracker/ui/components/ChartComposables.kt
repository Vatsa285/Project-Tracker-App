package com.miniprojecttracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PieChart(
    data: Map<String, Int>,
    colors: Map<String, Color>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || data.values.sum() == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data available")
        }
        return
    }

    val total = data.values.sum().toFloat()
    
    Canvas(modifier = modifier) {
        var startAngle = -90f
        val strokeWidth = size.minDimension * 0.2f
        val chartSize = size.minDimension - strokeWidth
        val chartOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

        data.forEach { (key, value) ->
            val sweepAngle = (value / total) * 360f
            val color = colors[key] ?: Color.Gray

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = chartOffset,
                size = Size(chartSize, chartSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun BarChart(
    data: Map<String, Int>,
    colors: Map<String, Color> = emptyMap(),
    defaultColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data available")
        }
        return
    }

    val maxVal = data.values.maxOrNull()?.toFloat() ?: 1f
    
    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2f)
        val spaceWidth = barWidth
        
        var xOffset = spaceWidth / 2f

        data.forEach { (key, value) ->
            val color = colors[key] ?: defaultColor
            val barHeight = (value / maxVal) * size.height

            drawRect(
                color = color,
                topLeft = Offset(xOffset, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
            xOffset += barWidth + spaceWidth
        }
    }
}
