package com.prayag.pocketscanner.weather.presentation.sunny

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*


class WeatherSunViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SunTimeUiState())
    val uiState: StateFlow<SunTimeUiState> = _uiState.asStateFlow()

    private val _animationCache = MutableStateFlow<AnimationCache?>(null)
    val animationCache: StateFlow<AnimationCache?> = _animationCache.asStateFlow()

    init {
        precomputeAnimationData()
    }

    private fun precomputeAnimationData() {
        viewModelScope.launch {
            val cache = withContext(Dispatchers.Default) {
                computeAnimationCache()
            }
            _animationCache.value = cache
        }
    }

    private fun computeAnimationCache(): AnimationCache {
        // Pre-compute glow layers
        val glowLayers = mutableListOf<GlowLayerCache>()
        val glowHeight = 1f // Normalized, will be scaled by sunRadius
        val glowStep = 2.5f
        var currentY = 0f

        while (currentY < glowHeight * 3.2f) {
            val depth = (currentY / (glowHeight * 3.2f)).coerceIn(0f, 1f)
            val ellipseFactor = sqrt(1f - (depth * depth).coerceIn(0f, 1f))
            val intensity = (1f - depth).pow(2.2f)

            glowLayers.add(
                GlowLayerCache(
                    baseYOffset = currentY,
                    depthFactor = depth,
                    ellipseFactor = ellipseFactor,
                    intensity = intensity,
                    baseWidth = 1.6f * ellipseFactor,
                    wave1Coefficient = depth * PI.toFloat() * 2,
                    wave2Coefficient = depth * PI.toFloat() * 1.5f,
                    wave3Coefficient = depth * PI.toFloat() * 3
                )
            )
            currentY += glowStep
        }

        // Pre-compute reflection strokes
        val strokeCount = 24
        val reflectionStrokes = List(strokeCount) { i ->
            val progress = i / strokeCount.toFloat()
            val depthCurve = progress.pow(1.6f)
            val oneMinusProgress = 1f - progress

            ReflectionStrokeCache(
                index = i,
                progress = progress,
                depthCurve = depthCurve,
                baseYOffset = depthCurve * 3.5f, // Normalized
                waveAmplitude = 0.09f * oneMinusProgress,
                baseWidth = 1.5f * (1 - progress * 0.55f),
                baseAlpha = oneMinusProgress.pow(2.6f) * 0.52f,
                timeOffset1 = i * 0.12f,
                timeOffset2 = i * 0.08f,
                timeOffset3 = i * 0.05f,
                oneMinusProgress = oneMinusProgress
            )
        }

        // Pre-compute horizon points
        val horizonPointCount = 50
        val horizonPoints = List(horizonPointCount) { i ->
            val normalizedX = i / horizonPointCount.toFloat()
            HorizonPointCache(
                xPosition = normalizedX,
                wave1Coefficient = normalizedX * 0.015f,
                wave2Coefficient = normalizedX * 0.02f
            )
        }

        // Pre-compute sparkle data
        val sparkleCount = 12
        val sparkles = List(sparkleCount) { i ->
            SparkleCache(
                index = i,
                baseXOffset = (i - sparkleCount / 2f) * 0.45f,
                waveCoefficient = i * 0.7f
            )
        }

        return AnimationCache(
            glowLayers = glowLayers,
            reflectionStrokes = reflectionStrokes,
            horizonPoints = horizonPoints,
            sparkleData = sparkles
        )
    }

    fun updateTemperature(temp: Int) {
        _uiState.value = _uiState.value.copy(temperature = temp)
    }
}