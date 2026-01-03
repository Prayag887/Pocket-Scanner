package com.prayag.pocketscanner.weather.presentation.sunny

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayag.pocketscanner.theme.ColorTheme
import com.prayag.pocketscanner.theme.ThemeProvider
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random

@Composable
fun WeatherSunScreen(
    viewModel: WeatherSunViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val animationCache by viewModel.animationCache.collectAsState()
    val theme = remember { ThemeProvider.getTheme() }

    var lastTapPosition by remember { mutableStateOf<Offset?>(null) }
    var tapTimestamp by remember { mutableLongStateOf(0L) }
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        theme.sky,
                        theme.nearHorizon,
                        theme.paperBg      // Ocean color
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {}
                )
            }
            .padding(vertical = 50.dp)

    ) {
        // Ambient background layers
        AnimatedBackgroundLayers(theme)

        // Floating clouds
        FloatingClouds(theme)

        // Enhanced header with parallax
        EnhancedHeader(state, theme, isPressed)

        // Main sun/river scene with interaction
        animationCache?.let { cache ->
                EnhancedSunRiverScene(
                cache,
                theme,
                lastTapPosition,
                tapTimestamp,
            )
        }

        // Atmospheric particles
        AtmosphericParticles(theme)

        // Interactive info cards
        WeatherInfoCards(state, theme)

        // Enhanced footer with animation
//        EnhancedFooter(state, theme)

        // Dynamic paper grain
        DynamicPaperGrain(theme)
    }
}

@Composable
private fun AnimatedBackgroundLayers(theme: ColorTheme) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val layerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "layerRotation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)

        repeat(3) { i ->
            val angle = layerOffset + i * 120f
            val radius = size.maxDimension * (1.2f + i * 0.3f)

            val offset = Offset(
                x = cos(angle * PI / 180f).toFloat() * 50f,
                y = sin(angle * PI / 180f).toFloat() * 50f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.celestialColor.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = center + offset,
                    radius = radius
                ),
                radius = radius,
                center = center + offset
            )
        }
    }
}



@Composable
private fun FloatingClouds(theme: ColorTheme) {
    val transition = rememberInfiniteTransition(label = "")
    val offset by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(100000, easing = LinearEasing)))

    Canvas(modifier = Modifier.fillMaxSize()) {
        repeat(4) { i ->
            val x = ((offset + i * 0.35f) % 1f) * size.width
            val y = size.height * (0.12f + i * 0.07f)
            drawCloudShape(
                center = Offset(x, y),
                size = 140.dp.toPx(),
                fillColor = Color.White.copy(0.18f),
                strokeColor = Color.Transparent
            )
        }
    }
}

/**
 * MISSING FUNCTION FIXED: Helper to draw organic cloud shapes
 */
private fun DrawScope.drawCloudShape(
    center: Offset,
    size: Float,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float = 0f
) {
    val w = size
    val h = size * 0.5f

    val path = Path().apply {
        moveTo(center.x - w * 0.4f, center.y + h * 0.2f)
        // Bottom Base
        lineTo(center.x + w * 0.4f, center.y + h * 0.2f)
        // Right Lobe
        cubicTo(
            center.x + w * 0.6f, center.y + h * 0.2f,
            center.x + w * 0.6f, center.y - h * 0.3f,
            center.x + w * 0.3f, center.y - h * 0.3f
        )
        // Top Lobe
        cubicTo(
            center.x + w * 0.2f, center.y - h * 0.8f,
            center.x - w * 0.2f, center.y - h * 0.8f,
            center.x - w * 0.3f, center.y - h * 0.3f
        )
        // Left Lobe
        cubicTo(
            center.x - w * 0.6f, center.y - h * 0.3f,
            center.x - w * 0.6f, center.y + h * 0.2f,
            center.x - w * 0.4f, center.y + h * 0.2f
        )
        close()
    }

    drawPath(path = path, color = fillColor)
    if (strokeWidth > 0) {
        drawPath(path = path, color = strokeColor, style = Stroke(width = strokeWidth))
    }
}



@Composable
private fun BoxScope.EnhancedHeader(
    state: SunTimeUiState,
    theme: ColorTheme,
    isPressed: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "headerScale"
    )

    // Temperature (anchored, calm)
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .scale(scale)
            .padding(start = 28.dp, top = 24.dp)
    ) {

        // TEMPERATURE — optical depth, zero gimmicks
        Text(
            text = "${state.temperature}°",
            fontSize = 74.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-1).sp,
            color = theme.ink,
            style = TextStyle(
                shadow = Shadow(
                    color = theme.ink.copy(alpha = 0.18f),
                    offset = Offset(0f, 6f),
                    blurRadius = 18f
                )
            )
        )

        // Micro separator via negative spacing
        Spacer(Modifier.height((-6).dp))

        // CITY — confident, restrained
        Text(
            text = state.city.uppercase(),
            fontSize = 12.sp,
            letterSpacing = 2.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.ink.copy(alpha = 0.85f),
            style = TextStyle(
                shadow = Shadow(
                    color = theme.ink.copy(alpha = 0.08f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 4f
                )
            )
        )

        Spacer(Modifier.height(2.dp))

        // FEELS LIKE — metadata, barely there
        Text(
            text = "Feels like ${state.temperature - 2}°",
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 0.2.sp,
            color = theme.ink.copy(alpha = 0.55f)
        )
    }

    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 32.dp, end = 24.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.condition.forEachIndexed { index, condition ->
            val delay = index * 100
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(delay.toLong())
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() +
                        slideInHorizontally { it / 2 }
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = theme.ink.copy(alpha = 0.05f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Text(
                        text = condition.toString(),
                        fontSize = 11.sp,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.ink,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun AtmosphericParticles(theme: ColorTheme) {
    val particles = remember {
        List(30) { i ->
            ParticleData(
                x = Random.nextFloat(),
                initialY = Random.nextFloat(),
                speed = 0.00005f + Random.nextFloat() * 0.00003f,
                size = 1f + Random.nextFloat() * 2f,
                opacity = 0.05f + Random.nextFloat() * 0.1f,
                phase = Random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.ink.copy(alpha = particle.opacity),
                        Color.Transparent
                    )
                ),
                radius = particle.size,
            )
        }
    }
}


@Composable
private fun BoxScope.WeatherInfoCards(
    state: SunTimeUiState,
    theme: ColorTheme,
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 20.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ROW 1 — slightly forward
        Row(
            modifier = Modifier
                .offset(y = (-2).dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                title = "UV INDEX",
                value = "6",
                subtitle = "Moderate",
                theme = theme,
                delay = 0
            )

            InfoCard(
                title = "HUMIDITY",
                value = "65%",
                subtitle = "Normal",
                theme = theme,
                delay = 80
            )
        }

        // SUBTLE RHYTHM DIVIDER (kept, but ultra-light)
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(theme.ink.copy(alpha = 0.02f))
        )

        // ROW 2 — slightly recessed
        Row(
            modifier = Modifier
                .offset(y = (2).dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                title = "WIND",
                value = "12",
                subtitle = "km/h",
                theme = theme,
                delay = 160
            )

            InfoCard(
                title = "VISIBILITY",
                value = "10",
                subtitle = "km",
                theme = theme,
                delay = 240
            )
        }
    }
}




@Composable
private fun InfoCard(
    title: String,
    value: String,
    subtitle: String,
    theme: ColorTheme,
    delay: Int
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + scaleIn(initialScale = 0.85f)
    ) {
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(100.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 40.dp,
                        bottomEnd = 40.dp
                    )
                )
                .background(theme.ink.copy(alpha = 0.04f))
        ) {
            Content(title, value, subtitle, theme)
        }
    }
}


@Composable
private fun Content(
    title: String,
    value: String,
    subtitle: String,
    theme: ColorTheme
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Title (slightly lifted)
        Text(
            text = title,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium,
            color = theme.ink.copy(alpha = 0.5f),
            modifier = Modifier.offset(y = (-6).dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Value block (true center mass)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = theme.ink
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = theme.ink.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun DynamicPaperGrain(theme: ColorTheme) {
    val points = remember {
        val random = Random(42)
        // Pre-convert to Offset objects to save allocation in the draw loop
        List(12_000) { Offset(random.nextFloat(), random.nextFloat()) }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val globalAlpha by infiniteTransition.animateFloat(
        initialValue = 0.01f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val grainColor = if (theme.paperBg.luminance() > 0.5f) Color.Black else Color.White

        // Scale offsets to current canvas size
        val scaledPoints = points.map { Offset(it.x * size.width, it.y * size.height) }

        drawPoints(
            points = scaledPoints,
            pointMode = PointMode.Points,
            color = grainColor.copy(alpha = globalAlpha),
            strokeWidth = 1f // 1 pixel
        )
    }
}



@Composable
private fun BoxScope.EnhancedSunRiverScene(
    cache: AnimationCache,
    theme: ColorTheme,
    lastTapPosition: Offset?,
    tapTimestamp: Long,
) {
    val animations = rememberWeatherAnimations()
    val phase1 = animations.phase1
    val phase2 = animations.phase2
    val shimmer = animations.shimmer
    val ripplePhase = animations.ripplePhase
    val verticalWave = animations.verticalWave

    // Sun glow pulsation
    val glowPhase by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing)
            ),
            label = "glowPhase"
        )

    val glowPulse = 1.0f + 0.2f * sin(glowPhase)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .align(Alignment.Center)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        val sunRadius = size.minDimension * 0.28f
        val horizonY = size.height * 0.55f
        val centerX = size.width / 2f
        val twoPi = 2f * PI.toFloat()
        val piFloat = PI.toFloat()

        // Enhanced sun glow layers
        repeat(5) { i ->
            val glowRadius = sunRadius * (1.5f + i * 0.3f) * glowPulse
            val glowAlpha = (0.08f - i * 0.015f) / glowPulse

            clipRect(bottom = horizonY) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.celestialColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset(centerX, horizonY),
                    blendMode = BlendMode.Screen
                )
            }
        }

        // CELESTIAL BODY with depth
        // CELESTIAL BODY with depth
        clipRect(bottom = horizonY) {
            // Outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        theme.celestialColor,
                        theme.celestialColor.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, horizonY),
                    radius = sunRadius * 1.2f
                ),
                radius = sunRadius * 1.2f,
                center = Offset(centerX, horizonY)
            )

            // Main sun
            drawCircle(
                brush = theme.celestialGradient,
                radius = sunRadius,
                center = Offset(centerX, horizonY)
            )
        }

// Sun surface detail - MOVED OUTSIDE clipRect
        // Sun surface detail - SMOOTH FADE IN/OUT
        repeat(3) { i ->
            val detailRadius = sunRadius * (0.3f + i * 0.15f)
            val detailAngle = phase1 * 0.5f + i * 2f
            val detailX = centerX + cos(detailAngle) * sunRadius * 0.3f
            val detailY = horizonY + sin(detailAngle) * sunRadius * 0.2f

            // Use squared distance to avoid sqrt
            val dx = detailX - centerX
            val dy = detailY - horizonY
            val distanceSquared = dx * dx + dy * dy
            val sunRadiusSquared = sunRadius * sunRadius

            if (distanceSquared < sunRadiusSquared && detailY < horizonY) {
                // Fade based on how close to the edge of the sun
                val distanceFromEdge = sunRadius - sqrt(distanceSquared)
                val edgeFadeFactor = (distanceFromEdge / (sunRadius * 0.3f)).coerceIn(0f, 1f)

                // Fade based on vertical position (fade out near horizon)
                val verticalDistanceFromHorizon = horizonY - detailY
                val verticalFadeFactor = (verticalDistanceFromHorizon / (sunRadius * 0.4f)).coerceIn(0f, 1f)

                // Combine both fade factors
                val finalAlpha = 0.05f * edgeFadeFactor * verticalFadeFactor

                drawCircle(
                    color = Color.White.copy(alpha = finalAlpha),
                    radius = detailRadius,
                    center = Offset(detailX, detailY),
                    blendMode = BlendMode.Screen
                )
            }
        }

        // [water ripples, glow layers, and reflection strokes code here]
        cache.reflectionStrokes.forEach { stroke ->
            val wavePhase1 = (phase1 + stroke.timeOffset1) % twoPi
            val wavePhase2 = (phase2 + stroke.timeOffset2) % twoPi
            val wavePhase3 = (ripplePhase + stroke.timeOffset3) % twoPi
            val baseYPos = horizonY + stroke.baseYOffset * sunRadius
            val progressPi = stroke.progress * piFloat
            val waveHeight1 = sin(wavePhase2 + progressPi) * 3f
            val waveHeight2 = cos(wavePhase3 * 1.8f + progressPi * 2) * 2f
            val yPos = baseYPos + waveHeight1 + waveHeight2
            val waveAmplitude = sunRadius * stroke.waveAmplitude
            val wave1 = sin(wavePhase1 * 3f) * waveAmplitude
            val wave2 = cos(wavePhase2 * 2.2f + progressPi) * waveAmplitude * 0.8f
            val wave3 = sin(wavePhase3 * 3.5f + stroke.index * 0.5f) * waveAmplitude * 0.6f
            val rippleEffect = sin(ripplePhase * 1.5f + stroke.index * 0.3f) * waveAmplitude * 0.5f
            val baseWidth = sunRadius * stroke.baseWidth
            val breathe = sin(phase1 * 1.2f + stroke.index * 0.2f) * baseWidth * 0.1f
            val currentWidth = baseWidth + wave1 + wave2 + wave3 + rippleEffect + breathe
            val horizontalShimmer = shimmer * stroke.oneMinusProgress.pow(1.2f) * 15f
            val verticalShimmer = verticalWave * stroke.oneMinusProgress * 3f
            val microShimmer = sin(phase1 * 5f + stroke.index * 0.7f) * 2f
            val alphaPulse1 = 0.88f + 0.12f * sin(phase1 * 0.6f + stroke.index * 0.2f)
            val alphaPulse2 = 0.94f + 0.06f * cos(phase2 * 0.8f + stroke.index * 0.25f)
            val alpha = stroke.baseAlpha * alphaPulse1 * alphaPulse2

            // LAYER 1: Main reflection
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        theme.brightColor.copy(alpha = alpha * 0.78f),
                        theme.warmColor.copy(alpha = alpha * 0.88f),
                        theme.brightColor.copy(alpha = alpha * 0.78f),
                        Color.Transparent
                    ),
                    startX = centerX - currentWidth,
                    endX = centerX + currentWidth
                ),
                topLeft = Offset(
                    x = centerX - currentWidth + horizontalShimmer,
                    y = yPos + verticalShimmer
                ),
                size = Size(currentWidth * 2f, 7f)
            )

            // LAYER 2: Core highlight
            val coreWidth = currentWidth * 0.75f
            val coreHighlight = sin(wavePhase1 * 3f + stroke.index * 0.5f)
            val coreAlpha = alpha * 0.38f * (0.88f + 0.12f * coreHighlight)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = coreAlpha * 0.28f),
                        theme.highlight.copy(alpha = coreAlpha * 0.2f),
                        Color.White.copy(alpha = coreAlpha * 0.28f),
                        Color.Transparent
                    ),
                    startX = centerX - coreWidth,
                    endX = centerX + coreWidth
                ),
                topLeft = Offset(
                    x = centerX - coreWidth + horizontalShimmer * 0.8f,
                    y = yPos + verticalShimmer - 1.5f
                ),
                size = Size(coreWidth * 2f, 3f)
            )

            // LAYER 3: Edge highlight
            val edgeHighlight = (sin(wavePhase1 * 4f + stroke.index * 0.6f) + 1f) * 0.5f
            val currentWidth92 = currentWidth * 0.92f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.14f * alpha * edgeHighlight),
                        Color.White.copy(alpha = 0.17f * alpha * edgeHighlight),
                        Color.White.copy(alpha = 0.14f * alpha * edgeHighlight),
                        Color.Transparent
                    ),
                    startX = centerX - currentWidth92,
                    endX = centerX + currentWidth92
                ),
                topLeft = Offset(
                    x = centerX - currentWidth92 + horizontalShimmer + microShimmer,
                    y = yPos + verticalShimmer - 2f
                ),
                size = Size(currentWidth * 1.84f, 1.2f)
            )

            // LAYER 4: Shadow
            val shadowDepth = alpha * 0.58f * (0.9f + 0.1f * sin(phase2 * 1.2f + stroke.index * 0.4f))
            val shadowWidth = currentWidth * 0.65f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        theme.deepAccent.copy(alpha = shadowDepth),
                        theme.deepColor.copy(alpha = shadowDepth * 0.92f),
                        theme.deepAccent.copy(alpha = shadowDepth),
                        Color.Transparent
                    ),
                    startX = centerX - shadowWidth,
                    endX = centerX + shadowWidth
                ),
                topLeft = Offset(
                    x = centerX - shadowWidth + horizontalShimmer * 0.5f,
                    y = yPos + verticalShimmer + 3.5f
                ),
                size = Size(shadowWidth * 2f, 4f)
            )

            // LAYER 5: Particles
            if (stroke.index < 9) {
                val particleOffset = sin(phase1 * 3f + stroke.index * 1f) * currentWidth * 0.35f
                val particleAlpha = alpha * 0.28f * (sin(wavePhase2 * 3f + stroke.index * 0.6f) + 1f) * 0.5f
                drawCircle(
                    color = Color.White.copy(alpha = particleAlpha),
                    radius = 1.3f,
                    center = Offset(
                        centerX + particleOffset + horizontalShimmer,
                        yPos + verticalShimmer - 3f
                    )
                )
            }

            // LAYER 6: Diffusion
            val diffusionWidth = currentWidth * 1.15f
            val diffusionAlpha = alpha * 0.18f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        theme.warmColor.copy(alpha = diffusionAlpha),
                        Color.Transparent
                    ),
                    startX = centerX - diffusionWidth,
                    endX = centerX + diffusionWidth
                ),
                topLeft = Offset(
                    x = centerX - diffusionWidth + horizontalShimmer * 0.6f,
                    y = yPos + verticalShimmer - 1f
                ),
                size = Size(diffusionWidth * 2f, 10f)
            )
        }

        // Touch-interactive water disturbance
        lastTapPosition?.let { tapPos ->
            val timeSinceTap = System.currentTimeMillis() - tapTimestamp
            if (timeSinceTap < 2000) {
                val progress = timeSinceTap / 2000f
                val disturbanceRadius = progress * 150f
                val disturbanceAlpha = (1f - progress) * 0.4f

                repeat(3) { i ->
                    val ringRadius = disturbanceRadius + i * 20f
                    drawCircle(
                        color = Color.White.copy(alpha = disturbanceAlpha * (1f - i * 0.3f)),
                        radius = ringRadius,
                        center = tapPos,
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Enhanced horizon with foam
        val horizonPath = Path()
        horizonPath.moveTo(0f, horizonY)
        cache.horizonPoints.forEach { point ->
            val x = size.width * point.xPosition
            val wave1 = sin(phase1 * 3f + point.wave1Coefficient) * 1.2f
            val wave2 = cos(phase2 * 2.5f + point.wave2Coefficient) * 0.8f
            horizonPath.lineTo(x, horizonY + wave1 + wave2)
        }

        // Multiple horizon lines for depth
        drawPath(
            path = horizonPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.15f)
                )
            ),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Foam particles along horizon
        cache.horizonPoints.forEachIndexed { index, point ->
            if (index % 3 == 0) {
                val x = size.width * point.xPosition
                val wave = sin(phase1 * 4f + point.wave1Coefficient) * 2f
                val foamAlpha = (sin(phase2 * 3f + index * 0.5f) + 1f) * 0.5f * 0.2f

                if (foamAlpha > 0.1f) {
                    drawCircle(
                        color = Color.White.copy(alpha = foamAlpha),
                        radius = 1.5f + sin(phase1 * 5f + index) * 0.5f,
                        center = Offset(x, horizonY + wave)
                    )
                }
            }
        }

        // Enhanced sparkles with trails
        cache.sparkleData.forEach { sparkle ->
            val sparkleTime = (phase1 * 2f + sparkle.index * 0.5f) % twoPi
            val sparkleAlpha = ((sin(sparkleTime) + 1f) * 0.5f).pow(2f)
            if (sparkleAlpha > 0.25f) {
                val xOffset = sparkle.baseXOffset * sunRadius
                val x = centerX + xOffset
                val waveOffset = sin(phase2 * 2f + sparkle.waveCoefficient) * 2f
                val y = horizonY + waveOffset

                // Sparkle core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    radius = 2f * sparkleAlpha,
                    center = Offset(x, y)
                )

                // Cross flare
                val flareLength = 8f * sparkleAlpha
                drawLine(
                    color = Color.White.copy(alpha = sparkleAlpha * 0.4f),
                    start = Offset(x - flareLength, y),
                    end = Offset(x + flareLength, y),
                    strokeWidth = 0.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = sparkleAlpha * 0.4f),
                    start = Offset(x, y - flareLength),
                    end = Offset(x, y + flareLength),
                    strokeWidth = 0.5f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Underwater caustic rays
        repeat(12) { i ->
            val rayAngle = (phase1 * 0.3f + i * (360f / 12f)) * PI / 180f
            val rayLength = sunRadius * 2.5f
            val rayStartRadius = sunRadius * 0.3f
            val rayAlpha = (sin(phase2 * 2f + i * 0.5f) + 1f) * 0.5f * 0.08f

            val startX = centerX + cos(rayAngle).toFloat() * rayStartRadius
            val startY = horizonY + sin(rayAngle).toFloat() * rayStartRadius
            val endX = centerX + cos(rayAngle).toFloat() * rayLength
            val endY = horizonY + sin(rayAngle).toFloat() * rayLength * 0.5f + rayLength * 0.5f

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        theme.celestialColor.copy(alpha = rayAlpha),
                        Color.Transparent
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Screen
            )
        }
    }
}