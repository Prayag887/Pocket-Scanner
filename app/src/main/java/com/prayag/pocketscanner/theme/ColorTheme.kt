package com.prayag.pocketscanner.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.util.Calendar

data class ColorTheme(
    val paperBg: Color,
    val celestialGradient: Brush,
    val celestialColor: Color,
    val ink: Color,
    val reflectionColor: Color,
    val deepColor: Color,
    val brightColor: Color,
    val warmColor: Color,
    val deepAccent: Color,
    val highlight: Color,
    val cloudColor: Color
)

object ThemeProvider {
    private val dayTheme = ColorTheme(
        // Warm paper, but slightly neutralized to avoid candy look
        paperBg = Color(0xFFFFF1E4),

        // Sun gradient: deeper shadow → clean sun core
        celestialGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF8A2E25), // shadowed sun edge
                Color(0xFFE53935)  // sun core
            )
        ),

        // Primary celestial (hero color)
        celestialColor = Color(0xFFE53935),

        // Ink: softer than pure black for daylight comfort
        ink = Color(0xFF2A1E1B),

        // Reflections: desaturated warm red
        reflectionColor = Color(0xFFB94A3E),

        // True shadow color (key fix)
        deepColor = Color(0xFF5A2A25),

        // Controlled brightness (not neon)
        brightColor = Color(0xFFE85C4A),

        // Warm accent — used sparingly
        warmColor = Color(0xFFF08A5D),

        // Structural accent (buttons, dividers)
        deepAccent = Color(0xFF8F3A30),

        // Daylight highlight (near-white, not yellow)
        highlight = Color(0xFFFFF7EE),
        cloudColor = Color(0xFF9E8B82)
    )


    private val nightTheme = ColorTheme(
        paperBg = Color(0xFF0A0E27),
        celestialGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xCCB8C5D6),
                Color(0xFFD4E0F0)
            )
        ),
        celestialColor = Color(0xFFD4E0F0),
        ink = Color(0xFFE8E8E8),
        reflectionColor = Color(0xFF6B8CAE),
        deepColor = Color(0xCC4A6B8A),
        brightColor = Color(0xFF8AAFD4),
        warmColor = Color(0xFF9FB9D9),
        deepAccent = Color(0xFF5577AA),
        highlight = Color(0xFFAAC5E0),
        cloudColor = Color(0xFF6B8CAE)
    )

    fun getTheme(): ColorTheme {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return if (hour in 6..17) dayTheme else nightTheme
    }
}