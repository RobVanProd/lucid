package com.lucid.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * The pulsing cursor - a single point of focus
 * breathing gently, waiting for intention.
 */
@Composable
fun PulsingCursor(
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    if (isActive) {
        Box(
            modifier = modifier
                .width(2.dp)
                .height(32.dp)
                .alpha(alpha)
                .background(MaterialTheme.colorScheme.onBackground)
        )
    }
}

/**
 * The breathing dot - indicates LUCID is listening
 */
@Composable
fun BreathingDot(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breath")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathAlpha"
    )

    Box(
        modifier = modifier
            .size((8 * scale).dp)
            .alpha(alpha)
            .background(
                MaterialTheme.colorScheme.onBackground,
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}
