package com.prayag.pocketscanner.splash.presentation.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayag.pocketscanner.auth.domain.model.User
import com.prayag.pocketscanner.auth.presentation.login.SkyAnimationState
import com.prayag.pocketscanner.auth.presentation.utils.StarrySkyBackground
import com.prayag.pocketscanner.auth.presentation.utils.precomputeAnimationValues
import com.prayag.pocketscanner.splash.presentation.viewmodel.SplashNavigationState
import com.prayag.pocketscanner.splash.presentation.viewmodel.SplashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onNavigateToHome: (SkyAnimationState) -> Unit,
    onNavigateToLogin: (SkyAnimationState) -> Unit,
    onNavigateToMainApp: (User) -> Unit
) {
    val splashViewModel: SplashViewModel = koinViewModel()
    val animationState = remember { mutableStateOf(SkyAnimationState()) }
    val navigationState by splashViewModel.navigationState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Animation states
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.3f) }
    val textAlpha = remember { Animatable(0f) }
    val textScale = remember { Animatable(0.8f) }
    val scanLineProgress = remember { Animatable(0f) }
    val particleAlpha = remember { Animatable(0f) }
    val glowIntensity = remember { Animatable(0f) }

    // Precompute starry background
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            precomputeAnimationValues(animationState)
        }
    }

    // Orchestrated animation sequence
    LaunchedEffect(Unit) {
        // Phase 1: Logo appears with scale and fade
        launch {
            delay(500)
            logoAlpha.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
        }
        launch {
            delay(500)
            logoScale.animateTo(1f, animationSpec = tween(1000, easing = EaseOutBack))
        }

        // Phase 2: Scanning effect
        delay(1300)
        launch {
            scanLineProgress.animateTo(1f, animationSpec = tween(1500, easing = EaseInOutCubic))
        }

        // Phase 3: Text appears with particles
        delay(2000)
        launch {
            textAlpha.animateTo(1f, animationSpec = tween(600, easing = EaseOutCubic))
        }
        launch {
            textScale.animateTo(1f, animationSpec = tween(800, easing = EaseOutBack))
        }
        launch {
            particleAlpha.animateTo(1f, animationSpec = tween(1000))
        }

        // Phase 4: Glow effect
        delay(2500)
        glowIntensity.animateTo(1f, animationSpec = tween(800, easing = EaseInOutSine))

        // Wait and trigger navigation
        delay(1000)
        splashViewModel.handleSplashFlow(context, animationState.value)
    }

    // Handle navigation with fade out
    LaunchedEffect(navigationState) {
        when (navigationState) {
            is SplashNavigationState.NavigateToHome -> {
                launch { logoAlpha.animateTo(0f, animationSpec = tween(600)) }
                launch { textAlpha.animateTo(0f, animationSpec = tween(600)) }
                delay(600)
                onNavigateToHome((navigationState as SplashNavigationState.NavigateToHome).animationState)
            }

            is SplashNavigationState.NavigateToLogin -> {
                launch { logoAlpha.animateTo(0f, animationSpec = tween(600)) }
                launch { textAlpha.animateTo(0f, animationSpec = tween(600)) }
                delay(600)
                onNavigateToLogin((navigationState as SplashNavigationState.NavigateToLogin).animationState)
            }

            is SplashNavigationState.NavigateToMainApp -> {
                launch { logoAlpha.animateTo(0f, animationSpec = tween(600)) }
                launch { textAlpha.animateTo(0f, animationSpec = tween(600)) }
                delay(600)
                onNavigateToMainApp((navigationState as SplashNavigationState.NavigateToMainApp).user)
            }

            SplashNavigationState.Loading -> {
                // Stay on splash screen
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0B2F),
                        Color(0xFF1A1A3A),
                        Color(0xFF0A0A1E)
                    )
                )
            )
    ) {
        // Starry background
        StarrySkyBackground(animationState.value)

        // Main logo container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo with scanner icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Animated scanner logo
                ScannerLogo(
                    scanProgress = scanLineProgress.value,
                    glowIntensity = glowIntensity.value,
                    particleAlpha = particleAlpha.value
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name with elegant typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(textScale.value)
                    .alpha(textAlpha.value)
            ) {
                Text(
                    text = "POCKET",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "SCANNER",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64B5F6),
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-8).dp)
                )

                // Subtle tagline
                Text(
                    text = "Scan • Organize • Share",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .alpha(glowIntensity.value)
                )
            }
        }

        // Floating particles effect
        FloatingParticles(
            modifier = Modifier.fillMaxSize(),
            alpha = particleAlpha.value
        )
    }
}


@Composable
fun ScannerLogo(
    scanProgress: Float,
    glowIntensity: Float,
    particleAlpha: Float
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.35f

        // Outer glow ring
        if (glowIntensity > 0f) {
            drawCircle(
                color = Color(0xFF64B5F6).copy(alpha = 0.3f * glowIntensity),
                radius = radius * 1.8f,
                center = center
            )
            drawCircle(
                color = Color(0xFF64B5F6).copy(alpha = 0.2f * glowIntensity),
                radius = radius * 2.2f,
                center = center
            )
        }

        // Main scanner body - rounded rectangle
        val rectSize = radius * 1.6f
        val cornerRadius = 16f

        drawRoundRect(
            color = Color(0xFF1E3A8A),
            topLeft = Offset(center.x - rectSize/2, center.y - rectSize/2),
            size = androidx.compose.ui.geometry.Size(rectSize, rectSize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
        )

        // Scanner screen (inner rectangle)
        val screenSize = rectSize * 0.7f
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(center.x - screenSize/2, center.y - screenSize/2),
            size = androidx.compose.ui.geometry.Size(screenSize, screenSize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
        )

        // Document icon inside screen
        drawDocumentIcon(center, screenSize * 0.4f)

        // Animated scan line
        if (scanProgress > 0f) {
            val scanY = center.y - screenSize/2 + (screenSize * scanProgress)
            drawLine(
                color = Color(0xFF64B5F6).copy(alpha = 0.8f),
                start = Offset(center.x - screenSize/2 + 8f, scanY),
                end = Offset(center.x + screenSize/2 - 8f, scanY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // Scan line glow
            drawLine(
                color = Color(0xFF64B5F6).copy(alpha = 0.3f),
                start = Offset(center.x - screenSize/2 + 8f, scanY),
                end = Offset(center.x + screenSize/2 - 8f, scanY),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        // Corner brackets (scanner UI elements)
        drawScannerBrackets(center, screenSize)
    }
}

private fun DrawScope.drawDocumentIcon(center: Offset, size: Float) {
    val docWidth = size * 0.6f
    val docHeight = size * 0.8f
    val cornerCut = size * 0.15f

    val path = Path().apply {
        moveTo(center.x - docWidth/2, center.y - docHeight/2)
        lineTo(center.x + docWidth/2 - cornerCut, center.y - docHeight/2)
        lineTo(center.x + docWidth/2, center.y - docHeight/2 + cornerCut)
        lineTo(center.x + docWidth/2, center.y + docHeight/2)
        lineTo(center.x - docWidth/2, center.y + docHeight/2)
        close()
    }

    drawPath(
        path = path,
        color = Color(0xFF64B5F6).copy(alpha = 0.8f)
    )

    // Document lines
    for (i in 0..2) {
        val lineY = center.y - docHeight/4 + (i * docHeight/6)
        drawLine(
            color = Color(0xFF1E3A8A),
            start = Offset(center.x - docWidth/3, lineY),
            end = Offset(center.x + docWidth/4, lineY),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawScannerBrackets(center: Offset, screenSize: Float) {
    val bracketSize = 20f
    val bracketThickness = 3f
    val color = Color(0xFF64B5F6).copy(alpha = 0.9f)

    val positions = listOf(
        Offset(center.x - screenSize/2, center.y - screenSize/2), // Top-left
        Offset(center.x + screenSize/2, center.y - screenSize/2), // Top-right
        Offset(center.x - screenSize/2, center.y + screenSize/2), // Bottom-left
        Offset(center.x + screenSize/2, center.y + screenSize/2)  // Bottom-right
    )

    positions.forEachIndexed { index, pos ->
        val xDir = if (index % 2 == 0) 1f else -1f
        val yDir = if (index < 2) 1f else -1f

        // Horizontal line
        drawLine(
            color = color,
            start = pos,
            end = Offset(pos.x + xDir * bracketSize, pos.y),
            strokeWidth = bracketThickness,
            cap = StrokeCap.Round
        )

        // Vertical line
        drawLine(
            color = color,
            start = pos,
            end = Offset(pos.x, pos.y + yDir * bracketSize),
            strokeWidth = bracketThickness,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    alpha: Float
) {
    Canvas(modifier = modifier) {
        if (alpha > 0f) {
            repeat(12) { i ->
                val angle = (i * 30f) * PI / 180f
                val radius = size.minDimension * (0.3f + (i % 3) * 0.1f)
                val x = size.width/2 + cos(angle.toFloat()) * radius
                val y = size.height/2 + sin(angle.toFloat()) * radius

                drawCircle(
                    color = Color(0xFF64B5F6).copy(alpha = alpha * 0.6f),
                    radius = 3f + (i % 2) * 2f,
                    center = Offset(x, y)
                )
            }
        }
    }
}