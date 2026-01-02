package com.prayag.pocketscanner.weather.presentation.sunny

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kotlin.math.PI

data class AnimationValues(
    val phase1: Float,
    val phase2: Float,
    val shimmer: Float,
    val ripplePhase: Float,
    val verticalWave: Float
)

@Composable
fun rememberWeatherAnimations(): AnimationValues {
    val infiniteTransition = rememberInfiniteTransition(label = "water-transition")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing)
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(3500, easing = LinearEasing)
        ),
        label = "phase2"
    )

    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(2800, easing = LinearEasing)
        ),
        label = "ripple-phase"
    )

    val verticalWave by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vertical-wave"
    )

    return AnimationValues(phase1, phase2, shimmer, ripplePhase, verticalWave)
}