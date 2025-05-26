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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun StarrySkyBackground(animationState: SkyAnimationState) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        // Draw a gradient background
        drawRect(
            color = Color(0xFF0A0A1A),
            size = size
        )

        // Draw stars with optimized rendering
        animationState.stars.forEach { star ->
            val x = star.x * width
            val y = star.y * height

            // Simple star drawing for better performance
            drawCircle(
                color = Color.White.copy(alpha = star.alpha),
                radius = star.size,
                center = Offset(x, y),
                style = Fill
            )

            // Draw subtle glow only for larger stars
            if (star.size > 1.5f) {
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha * 0.3f),
                    radius = star.size * 2.5f,
                    center = Offset(x, y),
                    style = Fill
                )
            }
        }

        // Draw snowflakes with proper distribution
        animationState.snowflakes.forEach { snowflake ->
            val x = snowflake.x * width
            val y = snowflake.y * height

            // Simple particle rendering for better performance
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = snowflake.size,
                center = Offset(x, y),
                style = Fill
            )
        }

        // Draw a subtle flow effect (liquid-like without heavy computation)
        val time = animationState.scanLine * 0.001f
        val flowColor = Color(0x1087CEEB) // Very transparent blue

        for (i in 0 until 3) {
            val yPos = height * (0.25f + i * 0.25f)
            val amplitude = height * 0.05f

            val points = mutableListOf<Offset>()
            for (x in 0 until width.toInt() step 10) {
                val xFloat = x.toFloat()
                val yFloat = yPos + sin(xFloat * 0.005f + time + i) * amplitude
                points.add(Offset(xFloat, yFloat))
            }

            // Connect points with smooth lines
            for (j in 0 until points.size - 1) {
                drawLine(
                    color = flowColor,
                    start = points[j],
                    end = points[j + 1],
                    strokeWidth = 20f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// Optimized animation computation
suspend fun precomputeAnimationValues(animationState: MutableState<SkyAnimationState>) {
    withContext(Dispatchers.Default) {
        val frameDuration = 1000L / 60 // Keep 60fps target but optimize calculations
        var lastFrameTime = System.currentTimeMillis()
        val screenHeight = 2000f

        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastFrameTime) / 1000f

            if (currentTime - lastFrameTime >= frameDuration) {
                val currentState = animationState.value

                // Update scan line with consistent speed
                val scanSpeed = 70f
                val newScanLine = if (currentState.scanLine > screenHeight) -300f else currentState.scanLine + scanSpeed * deltaTime

                // Fixed scan line alpha for stability
                val newScanLineAlpha = 0.3f

                // Simplified star updates for better performance
                val updatedStars = currentState.stars.map { star ->
                    val newPulsePhase = (star.pulsePhase + deltaTime * 1.2f) % 6.28f
                    // Use simple sin calculation for smoother alpha transition
                    val pulseAlpha = star.alpha * (0.7f + 0.3f * sin(newPulsePhase))
                    star.copy(pulsePhase = newPulsePhase, alpha = pulseAlpha)
                }

                // Updated snowflakes with better distribution
                val updatedSnowflakes = currentState.snowflakes.map { snowflake ->
                    // Make sure particles move evenly across the entire screen
                    val newY = (snowflake.y + deltaTime * snowflake.speed / 200f)
                    val actualY = if (newY > 1.2f) -0.2f else newY

                    // Simplified wobble effect for better performance
                    val newWobbleOffset = (snowflake.wobbleOffset + deltaTime * snowflake.wobbleSpeed) % 6.28f
                    // Limit wobble effect for smoother movement
                    val wobbleAmount = 0.002f
                    val newX = (snowflake.x + sin(newWobbleOffset) * wobbleAmount).coerceIn(0f, 1f)

                    snowflake.copy(x = newX, y = actualY, wobbleOffset = newWobbleOffset)
                }

                animationState.value = currentState.copy(
                    scanLine = newScanLine,
                    scanLineAlpha = newScanLineAlpha,
                    stars = updatedStars,
                    snowflakes = updatedSnowflakes
                )

                lastFrameTime = currentTime
            }

            // Use a slightly longer delay to reduce CPU usage
            delay(10)
        }
    }
}

// Generate stars with better distribution
fun generateRandomStar(): Star {
    return Star(
        x = Random.nextFloat(),
        y = Random.nextFloat(),
        size = Random.nextFloat() * 2.0f + 0.5f,
        alpha = Random.nextFloat() * 0.6f + 0.4f,
        pulsePhase = Random.nextFloat() * 6.28f
    )
}

// Generate snowflakes with even distribution across the entire screen
fun generateRandomSnowflake(): Snowflake {
    return Snowflake(
        // Ensure particles are spread across the full width
        x = Random.nextFloat(),
        // Start particles from random positions above the screen
        y = Random.nextFloat() * -1f,
        // Keep sizes reasonable for better performance
        size = Random.nextFloat() * 2f + 0.5f,
        // Consistent speeds for smoother movement
        speed = Random.nextFloat() * 15f + 10f,
        wobbleOffset = Random.nextFloat() * 6.28f,
        // Limit wobble speed for smoother movement
        wobbleSpeed = Random.nextFloat() * 1.0f + 0.5f
    )
}

// Initialize the animation state with more particles for better coverage
fun initializeAnimationState(): SkyAnimationState {
    val stars = List(100) { generateRandomStar() }
    // Increase number of snowflakes for better coverage
    val snowflakes = List(150) { generateRandomSnowflake() }

    return SkyAnimationState(
        scanLine = 0f,
        scanLineAlpha = 0.3f,
        stars = stars,
        snowflakes = snowflakes
    )
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