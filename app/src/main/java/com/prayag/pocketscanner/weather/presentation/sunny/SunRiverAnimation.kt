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
import kotlin.math.sin

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

    // Convert to continuous sine wave instead of reversing
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(2500, easing = LinearEasing)
        ),
        label = "shimmer-phase"
    )
    val shimmer = sin(shimmerPhase)

    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(2800, easing = LinearEasing)
        ),
        label = "ripple-phase"
    )

    // Convert to continuous sine wave instead of reversing
    val verticalWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing)
        ),
        label = "vertical-wave-phase"
    )
    val verticalWave = sin(verticalWavePhase) * 0.4f

    return AnimationValues(phase1, phase2, shimmer, ripplePhase, verticalWave)
}