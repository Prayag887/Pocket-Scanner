package com.prayag.pocketscanner.weather.presentation.sunny

data class AnimationCache(
    val glowLayers: List<GlowLayerCache>,
    val reflectionStrokes: List<ReflectionStrokeCache>,
    val horizonPoints: List<HorizonPointCache>,
    val sparkleData: List<SparkleCache>
)

data class GlowLayerCache(
    val baseYOffset: Float,
    val depthFactor: Float,
    val ellipseFactor: Float,
    val intensity: Float,
    val baseWidth: Float,
    val wave1Coefficient: Float,
    val wave2Coefficient: Float,
    val wave3Coefficient: Float
)

data class ReflectionStrokeCache(
    val index: Int,
    val progress: Float,
    val depthCurve: Float,
    val baseYOffset: Float,
    val waveAmplitude: Float,
    val baseWidth: Float,
    val baseAlpha: Float,
    val timeOffset1: Float,
    val timeOffset2: Float,
    val timeOffset3: Float,
    val oneMinusProgress: Float
)

data class HorizonPointCache(
    val xPosition: Float,
    val wave1Coefficient: Float,
    val wave2Coefficient: Float
)

data class SparkleCache(
    val index: Int,
    val baseXOffset: Float,
    val waveCoefficient: Float
)

data class ParticleData(
    val x: Float,
    val initialY: Float,
    val speed: Float,
    val size: Float,
    val opacity: Float,
    val phase: Float
)


data class CloudData(
    val initialX: Float,
    val y: Float,
    val speed: Float,
    val scale: Float,
    val opacity: Float,
    val radii: List<Float>
)