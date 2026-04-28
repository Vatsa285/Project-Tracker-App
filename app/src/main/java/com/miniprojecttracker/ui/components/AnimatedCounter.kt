package com.miniprojecttracker.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = androidx.compose.material3.LocalTextStyle.current
) {
    var countState by remember { mutableIntStateOf(0) }

    LaunchedEffect(count) {
        countState = count
    }

    val animatedCount by animateIntAsState(
        targetValue = countState,
        animationSpec = tween(durationMillis = 1000),
        label = "counter_animation"
    )

    Text(
        text = animatedCount.toString(),
        modifier = modifier,
        style = style
    )
}
