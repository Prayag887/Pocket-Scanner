package com.prayag.pocketscanner.auth.presentation.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayag.pocketscanner.R
import com.prayag.pocketscanner.auth.presentation.login.SkyAnimationState
import com.prayag.pocketscanner.auth.presentation.login.Snowflake
import com.prayag.pocketscanner.auth.presentation.login.Star
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun StarrySkyBackground(animationState: SkyAnimationState) {
//    val density = LocalDensity.current

    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        // Access current animation state
        val state = animationState

        // Draw stars
        state.stars.forEach { star ->
            val x = star.x * width
            val y = star.y * height
            drawCircle(
                color = Color.White.copy(alpha = star.alpha),
                radius = star.size,
                center = Offset(x, y),
                style = Fill
            )

            // Draw subtle glow around larger stars
            if (star.size > 1.5f) {
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha * 0.3f),
                    radius = star.size * 2.5f,
                    center = Offset(x, y),
                    style = Fill
                )
            }
        }

        // Draw snowflakes/particles
        state.snowflakes.forEach { snowflake ->
            val x = snowflake.x * width
            val y = snowflake.y * height

            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = snowflake.size,
                center = Offset(x, y),
                style = Fill
            )
        }

        // Draw scan line
        val scanLine = state.scanLine
//        val scanLineAlpha = state.scanLineAlpha

        drawLine(
            color = Color(0x66A0D1FF),  // Light blue with transparency
            start = Offset(0f, scanLine),
            end = Offset(width, scanLine),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )

        // Add a subtle glow effect
        drawLine(
            color = Color(0x33A0D1FF),  // Even more transparent blue
            start = Offset(0f, scanLine),
            end = Offset(width, scanLine),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
}


fun generateRandomStar(): Star {
    return Star(
        x = Random.nextFloat(),
        y = Random.nextFloat(),
        size = Random.nextFloat() * 2.5f + 0.5f,
        alpha = Random.nextFloat() * 0.7f + 0.3f,
        pulsePhase = Random.nextFloat() * 6.28f
    )
}

fun generateRandomSnowflake(): Snowflake {
    return Snowflake(
        x = Random.nextFloat(),
        y = Random.nextFloat() - 1f, // Start above the screen
        size = Random.nextFloat() * 1.5f + 0.5f,
        speed = Random.nextFloat() * 20f + 10f,
        wobbleOffset = Random.nextFloat() * 6.28f,
        wobbleSpeed = Random.nextFloat() * 2f + 0.5f
    )
}

// Precompute animation values
suspend fun precomputeAnimationValues(animationState: MutableState<SkyAnimationState>) {
    withContext(Dispatchers.Default) {
        val frameDuration = 1000L / 60 // 60fps is sufficient
        var lastFrameTime = System.currentTimeMillis()
        val screenHeight = 2000f

        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastFrameTime) / 1000f // Convert to seconds

            if (currentTime - lastFrameTime >= frameDuration) {
                val currentState = animationState.value

                // Update scan line
                val scanSpeed = 70f // pixels per second
                val newScanLine = if (currentState.scanLine > screenHeight) -300f else currentState.scanLine + scanSpeed * deltaTime

                // Calculate scan line alpha
                val newScanLineAlpha = when {
                    newScanLine < 0 -> 0.2f
                    newScanLine > screenHeight - 300f -> 0.2f
                    else -> 0.4f
                }

                // Update stars (pulsing effect)
                val updatedStars = currentState.stars.map { star ->
                    val newPulsePhase = (star.pulsePhase + deltaTime * 1.2f) % 6.28f
                    val pulseAlpha = star.alpha * (0.7f + 0.3f * sin(newPulsePhase))
                    star.copy(pulsePhase = newPulsePhase, alpha = pulseAlpha)
                }

                // Update snowflakes
                val updatedSnowflakes = currentState.snowflakes.map { snowflake ->
                    val newY = (snowflake.y + deltaTime * snowflake.speed / 200f) % 1.2f
                    val newWobbleOffset = (snowflake.wobbleOffset + deltaTime * snowflake.wobbleSpeed) % 6.28f
                    val newX = (snowflake.x + sin(newWobbleOffset) * 0.003f).coerceIn(0f, 1f)
                    snowflake.copy(x = newX, y = newY, wobbleOffset = newWobbleOffset)
                }

                animationState.value = currentState.copy(
                    scanLine = newScanLine,
                    scanLineAlpha = newScanLineAlpha,
                    stars = updatedStars,
                    snowflakes = updatedSnowflakes
                )

                lastFrameTime = currentTime
            }

            delay(5) // Small delay to reduce CPU usage
        }
    }
}


@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1A1A1A),
            disabledContainerColor = Color.White.copy(alpha = 0.4f)
        ),
        border = BorderStroke(0.dp, Color.Transparent),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_google_logo),
            contentDescription = "Google logo",
            tint = Color.Unspecified,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Sign in with Google",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}