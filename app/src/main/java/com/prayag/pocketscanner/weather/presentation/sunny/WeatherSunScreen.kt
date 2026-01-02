package com.prayag.pocketscanner.weather.presentation.sunny

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.paperBg)
    ) {
        Header(state, theme)
        animationCache?.let { cache ->
            SunRiverScene(cache, theme)
        }
        Footer(state, theme)
        PaperGrain(theme)
    }
}

@Composable
private fun BoxScope.Header(state: SunTimeUiState, theme: ColorTheme) {
    Text(
        text = "${state.temperature}°",
        fontSize = 56.sp,
        fontWeight = FontWeight.Light,
        color = theme.ink,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 28.dp, top = 24.dp)
    )
    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 28.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        state.condition.forEach {
            Text(
                text = it.toString(),
                fontSize = 12.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Medium,
                color = theme.ink
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun BoxScope.SunRiverScene(cache: AnimationCache, theme: ColorTheme) {
    val animations = rememberWeatherAnimations()
    val phase1 = animations.phase1
    val phase2 = animations.phase2
    val shimmer = animations.shimmer
    val ripplePhase = animations.ripplePhase
    val verticalWave = animations.verticalWave

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .align(Alignment.Center)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        val sunRadius = size.minDimension * 0.26f
        val horizonY = size.height * 0.55f
        val centerX = size.width / 2f
        val twoPi = 2f * PI.toFloat()
        val piFloat = PI.toFloat()

        // CELESTIAL BODY (SUN/MOON - 55% ABOVE WATER)
        clipRect(bottom = horizonY) {
            drawCircle(
                brush = theme.celestialGradient,
                radius = sunRadius,
                center = Offset(centerX, horizonY)
            )
        }

        // ANIMATED WATER SURFACE RIPPLES
        val rippleCount = 5
        val rippleInvCount = 1f / rippleCount
        repeat(rippleCount) { i ->
            val rippleTime = (ripplePhase + i * piFloat / 1.8f) % twoPi
            val rippleProgress = sin(rippleTime)
            if (rippleProgress > 0f) {
                val rippleAlpha = (1f - i * rippleInvCount) * 0.18f
                val rippleRadius = sunRadius * (0.75f + rippleProgress * 0.35f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = rippleAlpha * rippleProgress * 0.9f),
                            theme.brightColor.copy(alpha = rippleAlpha * rippleProgress * 0.5f),
                            Color.Transparent
                        )
                    ),
                    radius = rippleRadius,
                    center = Offset(centerX, horizonY),
                    style = Stroke(width = 0.8f, cap = StrokeCap.Round)
                )
            }
        }

        // SUBSURFACE GLOW
        cache.glowLayers.forEach { layer ->
            val wave1 = sin(layer.wave1Coefficient + phase1 * 0.8f) * 2f
            val wave2 = cos(layer.wave2Coefficient + phase2 * 0.6f) * 1.5f
            val wave3 = sin(layer.wave3Coefficient + phase1 * 0.4f) * 1f
            val verticalShift = verticalWave * layer.depthFactor
            val baseWidth = sunRadius * layer.baseWidth
            val halfWidth = baseWidth + wave1 + wave2 + wave3
            val glowY = horizonY + layer.baseYOffset * sunRadius
            val pulse = 0.88f + 0.12f * sin(phase1 * 0.8f)

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        theme.celestialColor.copy(alpha = layer.intensity * 0.4f * pulse),
                        theme.celestialColor.copy(alpha = layer.intensity * 0.36f * pulse),
                        theme.celestialColor.copy(alpha = layer.intensity * 0.4f * pulse),
                        Color.Transparent
                    ),
                    startX = centerX - halfWidth,
                    endX = centerX + halfWidth
                ),
                topLeft = Offset(centerX - halfWidth, glowY + verticalShift),
                size = Size(halfWidth * 2f, 3f)
            )

            if (layer.depthFactor < 0.7f) {
                val halfWidth85 = halfWidth * 0.85f
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            theme.deepAccent.copy(alpha = layer.intensity * 0.2f),
                            Color.Transparent
                        ),
                        startX = centerX - halfWidth85,
                        endX = centerX + halfWidth85
                    ),
                    topLeft = Offset(centerX - halfWidth85, glowY + verticalShift + 1f),
                    size = Size(halfWidth * 1.7f, 2.5f)
                )
            }

            if (layer.depthFactor < 0.5f) {
                val highlightIntensity = (1f - layer.depthFactor * 2f).pow(3f)
                val halfWidthHalf = halfWidth * 0.5f
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            theme.warmColor.copy(alpha = highlightIntensity * 0.15f),
                            Color.Transparent
                        ),
                        startX = centerX - halfWidthHalf,
                        endX = centerX + halfWidthHalf
                    ),
                    topLeft = Offset(centerX - halfWidthHalf, glowY + verticalShift),
                    size = Size(halfWidth, 2f)
                )
            }
        }

        // REFLECTION STROKES
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

        // HORIZON LINE
        val horizonPath = Path()
        horizonPath.moveTo(0f, horizonY)
        cache.horizonPoints.forEach { point ->
            val x = size.width * point.xPosition
            val wave1 = sin(phase1 * 3f + point.wave1Coefficient) * 0.8f
            val wave2 = cos(phase2 * 2.5f + point.wave2Coefficient) * 0.5f
            horizonPath.lineTo(x, horizonY + wave1 + wave2)
        }
        drawPath(
            path = horizonPath,
            color = Color.White.copy(alpha = 0.18f),
            style = Stroke(width = 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val horizonPath2 = Path()
        horizonPath2.moveTo(0f, horizonY + 1.5f)
        cache.horizonPoints.forEach { point ->
            val x = size.width * point.xPosition
            val wave = sin(phase1 * 2.5f + point.wave1Coefficient * 0.9f) * 0.5f
            horizonPath2.lineTo(x, horizonY + 1.5f + wave)
        }
        drawPath(
            path = horizonPath2,
            color = theme.reflectionColor.copy(alpha = 0.1f),
            style = Stroke(width = 1.1f, cap = StrokeCap.Round)
        )

        // SPARKLES
        cache.sparkleData.forEach { sparkle ->
            val sparkleTime = (phase1 * 2f + sparkle.index * 0.5f) % twoPi
            val sparkleAlpha = ((sin(sparkleTime) + 1f) * 0.5f).pow(2.2f)
            if (sparkleAlpha > 0.3f) {
                val xOffset = sparkle.baseXOffset * sunRadius
                val x = centerX + xOffset
                val waveOffset = sin(phase2 * 2f + sparkle.waveCoefficient) * 1.5f
                val y = horizonY + waveOffset
                drawCircle(
                    color = Color.White.copy(alpha = sparkleAlpha * 0.25f),
                    radius = 1.3f,
                    center = Offset(x, y)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = sparkleAlpha * 0.1f),
                            Color.Transparent
                        )
                    ),
                    radius = 3.5f,
                    center = Offset(x, y)
                )
            }
        }

        // PARTICLES
        val particleCount = 10
        repeat(particleCount) { i ->
            val particlePhase = (phase2 * 1.5f + i * 0.7f) % twoPi
            val particleAlpha = ((sin(particlePhase) + 1f) * 0.5f).pow(1.6f)
            if (particleAlpha > 0.35f) {
                val depth = 12f + i * 10f
                val xOffset = cos(phase1 * 1.5f + i * 1f) * sunRadius * 0.65f
                val x = centerX + xOffset
                val y = horizonY + depth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = particleAlpha * 0.18f),
                            theme.highlight.copy(alpha = particleAlpha * 0.1f),
                            Color.Transparent
                        )
                    ),
                    radius = 2.8f,
                    center = Offset(x, y)
                )
            }
        }

        // CAUSTICS
        val causticCount = 8
        repeat(causticCount) { i ->
            val causticPhase = (ripplePhase * 1.5f + i * 0.9f) % twoPi
            val causticIntensity = (sin(causticPhase) + 1f) * 0.5f
            if (causticIntensity > 0.45f) {
                val depth = 18f + i * 14f
                val xOffset = sin(phase1 * 2f + i * 1.3f) * sunRadius * 0.55f
                val x = centerX + xOffset
                val y = horizonY + depth
                val causticWidth = 10f + causticIntensity * 7f
                val causticHeight = 2.5f + causticIntensity * 2f
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = causticIntensity * 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(x, y),
                        radius = causticWidth
                    ),
                    topLeft = Offset(x - causticWidth, y - causticHeight),
                    size = Size(causticWidth * 2f, causticHeight * 2f)
                )
            }
        }
    }
}

@Composable
private fun BoxScope.Footer(state: SunTimeUiState, theme: ColorTheme) {
    Text(
        text = state.city,
        fontSize = 12.sp,
        letterSpacing = 6.sp,
        fontWeight = FontWeight.Medium,
        color = theme.ink,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 28.dp)
    )
}

@Composable
private fun PaperGrain(theme: ColorTheme) {
    val points = remember {
        buildList {
            val random = Random(42)
            repeat(10_000) {
                add(
                    Triple(
                        random.nextFloat(),
                        random.nextFloat(),
                        random.nextFloat() * 0.02f
                    )
                )
            }
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val grainColor = if (theme.paperBg.luminance() > 0.5f) Color.Black else Color.White
        for ((xN, yN, alpha) in points) {
            drawRect(
                grainColor.copy(alpha = alpha),
                topLeft = Offset(xN * size.width, yN * size.height),
                size = Size(1f, 1f)
            )
        }
    }
}