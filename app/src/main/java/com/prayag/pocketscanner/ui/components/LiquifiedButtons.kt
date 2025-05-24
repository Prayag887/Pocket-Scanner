package com.prayag.pocketscanner.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.prayag.pocketscanner.ui.theme.VibrantOrange
import com.prayag.pocketscanner.ui.theme.VibrantPurple
import kotlinx.coroutines.delay

@Composable
fun LiquidPageNavigationButtons(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val buttonPlaceholderSize = 42.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentPage > 0) {
            RippleNavigationButton(
                icon = Icons.Default.KeyboardArrowLeft,
                isLeft = true,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPreviousPage()
                }
            )
        } else {
            Box(modifier = Modifier.size(buttonPlaceholderSize))
        }

        // Center page indicator
        PageIndicatorLines(
            currentPage = currentPage,
            totalPages = totalPages
        )

        // Right navigation button
        if (currentPage < totalPages - 1) {
            RippleNavigationButton(
                icon = Icons.Default.KeyboardArrowRight,
                isLeft = false,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNextPage()
                }
            )
        } else {
            Box(modifier = Modifier.size(buttonPlaceholderSize))
        }
    }
}

@Composable
fun RippleNavigationButton(
    icon: ImageVector,
    isLeft: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Ripple ring animation progress (0 to 1)
    val rippleProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )

    // Button scale on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Expanding ripple ring
        if (rippleProgress > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                val strokeWidth = 3f
                drawCircle(
                    color = VibrantPurple.copy(alpha = 1f - rippleProgress),
                    radius = radius * rippleProgress,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        // Rounded square base with soft shadow & glow
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = if (isPressed) 1.dp else 4.dp,
            shadowElevation = if (isPressed) 2.dp else 6.dp,
            color = VibrantOrange,
            modifier = Modifier
                .size(32.dp)
                .scale(scale)
                .graphicsLayer {
                    rotationZ = if (isPressed) {
                        if (isLeft) -6f else 6f
                    } else 0f
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Icon with subtle glow behind
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = if (isLeft) "Previous" else "Next",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Reset press state after animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(400)
            isPressed = false
        }
    }
}

@Composable
fun PageIndicatorLines(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val maxLines = 5
        val showLines = minOf(totalPages, maxLines)
        val startPage = when {
            totalPages <= maxLines -> 0
            currentPage <= 1 -> 0
            currentPage >= totalPages - 2 -> totalPages - maxLines
            else -> currentPage - 2
        }

        for (i in 0 until showLines) {
            val pageIndex = startPage + i
            val isActive = pageIndex == currentPage

            val lineWidth by animateDpAsState(
                targetValue = if (isActive) 28.dp else 16.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "lineWidth"
            )

            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.4f,
                animationSpec = tween(durationMillis = 300),
                label = "lineAlpha"
            )

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(lineWidth)
                    .alpha(alpha)
                    .background(
                        color = VibrantPurple,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
