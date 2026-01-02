package com.prayag.pocketscanner.weather.presentation.sunny

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayag.pocketscanner.theme.ColorTheme
import com.prayag.pocketscanner.theme.ThemeProvider
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WeatherSunScreen(
    viewModel: WeatherSunViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val animationCache by viewModel.animationCache.collectAsState()
    val theme = remember { ThemeProvider.getTheme() }

    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var lastTapPosition by remember { mutableStateOf<Offset?>(null) }
    var tapTimestamp by remember { mutableStateOf(0L) }
    var isPressed by remember { mutableStateOf(false) }

    val rippleAnimations = remember { mutableStateListOf<RippleEffect>() }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                rippleAnimations.removeAll { it.age > 2000 }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.paperBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { offset ->
                        lastTapPosition = offset
                        tapTimestamp = System.currentTimeMillis()
                        rippleAnimations.add(
                            RippleEffect(
                                position = offset,
                                startTime = tapTimestamp
                            )
                        )
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount * 0.3f
                    },
                    onDragEnd = {
                        dragOffset = Offset.Zero
                    }
                )
            }
    ) {
        // Ambient background layers
        AnimatedBackgroundLayers(theme, dragOffset)

        // Floating clouds
        FloatingClouds(theme, dragOffset)

        // Enhanced header with parallax
        EnhancedHeader(state, theme, dragOffset, isPressed)

        // Main sun/river scene with interaction
        animationCache?.let { cache ->
            EnhancedSunRiverScene(
                cache,
                theme,
                dragOffset,
                lastTapPosition,
                tapTimestamp,
                rippleAnimations
            )
        }

        // Atmospheric particles
        AtmosphericParticles(theme, dragOffset)

        // Interactive info cards
        WeatherInfoCards(state, theme, dragOffset)

        // Enhanced footer with animation
        EnhancedFooter(state, theme)

        // Dynamic paper grain
        DynamicPaperGrain(theme, dragOffset)

        // Touch ripples
        TouchRipples(rippleAnimations, theme)
    }
}

data class RippleEffect(
    val position: Offset,
    val startTime: Long,
    val age: Long = System.currentTimeMillis() - startTime
)

@Composable
private fun AnimatedBackgroundLayers(theme: ColorTheme, dragOffset: Offset) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val layer1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "layer1"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Subtle gradient layers that respond to drag
        repeat(3) { i ->
            val angle = layer1Offset + i * 120f + dragOffset.x * 0.05f
            val radius = size.maxDimension * (1.2f + i * 0.3f)
            val offsetX = cos(angle * PI / 180f).toFloat() * 50f
            val offsetY = sin(angle * PI / 180f).toFloat() * 50f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.celestialColor.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(centerX + offsetX, centerY + offsetY),
                    radius = radius
                ),
                radius = radius,
                center = Offset(centerX + offsetX, centerY + offsetY)
            )
        }
    }
}

@Composable
private fun FloatingClouds(theme: ColorTheme, dragOffset: Offset) {
    val clouds = remember {
        List(5) { i ->
            CloudData(
                initialX = Random.nextFloat(),
                y = 0.15f + i * 0.15f,
                speed = 6f + Random.nextFloat() * 4f,
                scale = 0.6f + Random.nextFloat() * 0.8f,
                opacity = 0.02f + Random.nextFloat() * 0.04f,
                radii = List(8) { 0.8f + Random.nextFloat() * 0.4f }
            )
        }
    }


    val animationTime by rememberInfiniteTransition(label = "clouds")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(100000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cloudTime"
        )

    Canvas(modifier = Modifier.fillMaxSize()) {
        clouds.forEach { cloud ->
            val x = ((cloud.initialX + animationTime * cloud.speed) % 1f) * size.width
            val y = size.height * cloud.y - dragOffset.y * 0.1f * (1f - cloud.y)

            drawCloudShape(
                center = Offset(x, y),
                scale = cloud.scale * size.width * 0.15f,
                radii = cloud.radii,
                color = theme.ink.copy(alpha = cloud.opacity)
            )

        }
    }
}

data class CloudData(
    val initialX: Float,
    val y: Float,
    val speed: Float,
    val scale: Float,
    val opacity: Float,
    val radii: List<Float>
)


fun DrawScope.drawCloudShape(
    center: Offset,
    scale: Float,
    radii: List<Float>,
    color: Color
) {
    val path = Path()
    val segments = radii.size

    radii.forEachIndexed { i, radiusFactor ->
        val angle = (i * 2f * PI / segments).toFloat()
        val radius = scale * radiusFactor

        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * radius * 0.6f

        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    path.close()
    drawPath(path, color)
}


@Composable
private fun BoxScope.EnhancedHeader(
    state: SunTimeUiState,
    theme: ColorTheme,
    dragOffset: Offset,
    isPressed: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "headerScale"
    )

    // Temperature with parallax
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = (dragOffset.x * 0.02f).dp, y = (dragOffset.y * 0.02f).dp)
            .scale(scale)
            .padding(start = 28.dp, top = 24.dp)
    ) {
        Text(
            text = "${state.temperature}°",
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraLight,
            color = theme.ink,
            style = androidx.compose.ui.text.TextStyle(
                shadow = Shadow(
                    color = theme.ink.copy(alpha = 0.1f),
                    offset = Offset(0f, 4f),
                    blurRadius = 8f
                )
            )
        )

        // Feels like temperature
        Text(
            text = "Feels like ${state.temperature - 2}°",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = theme.ink.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp)
        )
    }

    // Animated condition badges
    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = -(dragOffset.x * 0.03f).dp, y = (dragOffset.y * 0.01f).dp)
            .padding(top = 32.dp, end = 24.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.condition.forEachIndexed { index, condition ->
            val delay = index * 100
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(delay.toLong())
                visible = true
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = androidx.compose.animation.fadeIn() +
                        androidx.compose.animation.slideInHorizontally(initialOffsetX = { it / 2 })
            ) {
                Card(
                    modifier = Modifier,
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
private fun BoxScope.EnhancedSunRiverScene(
    cache: AnimationCache,
    theme: ColorTheme,
    dragOffset: Offset,
    lastTapPosition: Offset?,
    tapTimestamp: Long,
    rippleAnimations: List<RippleEffect>
) {
    val animations = rememberWeatherAnimations()
    val phase1 = animations.phase1
    val phase2 = animations.phase2
    val shimmer = animations.shimmer
    val ripplePhase = animations.ripplePhase
    val verticalWave = animations.verticalWave

    // Sun glow pulsation
    val glowPulse by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowPulse"
        )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .align(Alignment.Center)
            .offset(x = (dragOffset.x * 0.05f).dp, y = (dragOffset.y * 0.08f).dp)
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

            // Sun surface detail
            repeat(3) { i ->
                val detailRadius = sunRadius * (0.3f + i * 0.15f)
                val detailAngle = phase1 * 0.5f + i * 2f
                val detailX = centerX + cos(detailAngle) * sunRadius * 0.3f
                val detailY = horizonY + sin(detailAngle) * sunRadius * 0.2f

                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = detailRadius,
                    center = Offset(detailX, detailY),
                    blendMode = BlendMode.Screen
                )
            }
        }

        // [Keep existing water ripples, glow layers, and reflection strokes code here]
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

@Composable
private fun AtmosphericParticles(theme: ColorTheme, dragOffset: Offset) {
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

    val animationTime by rememberInfiniteTransition(label = "particles")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(120000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "particleTime"
        )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val y = ((particle.initialY - animationTime * particle.speed) % 1f) * size.height
            val x = size.width * particle.x + sin(y * 0.01f + particle.phase) * 30f
            val adjustedX = x - dragOffset.x * 0.2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.ink.copy(alpha = particle.opacity),
                        Color.Transparent
                    )
                ),
                radius = particle.size,
                center = Offset(adjustedX, y)
            )
        }
    }
}

data class ParticleData(
    val x: Float,
    val initialY: Float,
    val speed: Float,
    val size: Float,
    val opacity: Float,
    val phase: Float
)

@Composable
private fun BoxScope.WeatherInfoCards(
    state: SunTimeUiState,
    theme: ColorTheme,
    dragOffset: Offset
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 24.dp, bottom = 80.dp)
            .offset(x = (dragOffset.x * 0.01f).dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // UV Index Card
        InfoCard(
            title = "UV INDEX",
            value = "6",
            subtitle = "Moderate",
            theme = theme,
            delay = 0
        )

        // Humidity Card
        InfoCard(
            title = "HUMIDITY",
            value = "65%",
            subtitle = "Normal",
            theme = theme,
            delay = 100
        )
    }

    Row(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 24.dp, bottom = 80.dp)
            .offset(x = -(dragOffset.x * 0.01f).dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Wind Card
        InfoCard(
            title = "WIND",
            value = "12",
            subtitle = "km/h",
            theme = theme,
            delay = 200
        )

        // Visibility Card
        InfoCard(
            title = "VISIBILITY",
            value = "10",
            subtitle = "km",
            theme = theme,
            delay = 300
        )
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
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(tween(600)) +
                androidx.compose.animation.scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
    ) {
        Card(
            modifier = Modifier
                .width(90.dp)
                .height(100.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = theme.ink.copy(alpha = 0.03f)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.ink.copy(alpha = 0.5f)
                )

                Column {
                    Text(
                        text = value,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = theme.ink
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = theme.ink.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.EnhancedFooter(state: SunTimeUiState, theme: ColorTheme) {
    val scale by rememberInfiniteTransition(label = "footer")
        .animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "footerScale"
        )

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 32.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location pin icon (you'd use actual icon)
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(theme.ink.copy(alpha = 0.3f))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = state.city,
            fontSize = 13.sp,
            letterSpacing = 5.sp,
            fontWeight = FontWeight.Medium,
            color = theme.ink,
            style = androidx.compose.ui.text.TextStyle(
                shadow = Shadow(
                    color = theme.ink.copy(alpha = 0.1f),
                    offset = Offset(0f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}

@Composable
private fun DynamicPaperGrain(theme: ColorTheme, dragOffset: Offset) {
    val points = remember {
        buildList {
            val random = Random(42)
            repeat(12_000) {
                add(
                    Triple(
                        random.nextFloat(),
                        random.nextFloat(),
                        random.nextFloat() * 0.025f
                    )
                )
            }
        }
    }

    val noiseOffset by rememberInfiniteTransition(label = "grain")
        .animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "grainOffset"
        )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val grainColor = if (theme.paperBg.luminance() > 0.5f) Color.Black else Color.White
        for ((xN, yN, alpha) in points) {
            val dynamicAlpha = alpha * (0.8f + 0.2f * sin(noiseOffset * 0.1f + xN * 10f))
            val x = xN * size.width + dragOffset.x * 0.01f
            val y = yN * size.height + dragOffset.y * 0.01f

            drawRect(
                grainColor.copy(alpha = dynamicAlpha),
                topLeft = Offset(x, y),
                size = Size(1f, 1f)
            )
        }
    }
}
@Composable
private fun TouchRipples(rippleAnimations: List<RippleEffect>, theme: ColorTheme) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        rippleAnimations.forEach { ripple ->
            val age = System.currentTimeMillis() - ripple.startTime
            val progress = age / 2000f
            if (progress < 1f) {
                val radius = progress * 200f
                val alpha = (1f - progress) * 0.3f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.celestialColor.copy(alpha = alpha),
                            Color.Transparent
                        ),
                        center = ripple.position,
                        radius = radius
                    ),
                    radius = radius,
                    center = ripple.position
                )

                drawCircle(
                    color = theme.brightColor.copy(alpha = alpha * 0.5f),
                    radius = radius,
                    center = ripple.position,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}