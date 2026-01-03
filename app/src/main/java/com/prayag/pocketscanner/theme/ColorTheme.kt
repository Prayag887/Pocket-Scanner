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
    val cloudColor: Color,
    val sky: Color,
    val nearHorizon: Color
)

object ThemeProvider {
    private val dayTheme = ColorTheme(
        // Warm paper, but slightly neutralized to avoid candy look
        paperBg =  Color(0xFF027399),

        // Sun gradient: deeper shadow → clean sun core
        celestialGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF8A4525), // shadowed sun edge
                Color(0xFFE57335)  // sun core
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
        cloudColor = Color(0xFF83B7BF),
        sky =  Color(0xFF00C6FF),
        nearHorizon =  Color(0xFFFFF1E4),
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
        cloudColor = Color(0xFF6B8CAE),

        sky = Color(0xFF0F1A3A),
        nearHorizon = Color(0xFF2E3F66)
    )

    private val jungleTheme = ColorTheme(
        // Paper background: warm leaf parchment
        paperBg = Color(0xFFD7F8A0),

        // Celestial gradient: canopy light filtering through leaves
        celestialGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFA86F41),// deep canopy shadow
                Color(0xFFDB8033), // filtered jungle light
            )
        ),

        // Primary celestial color (hero accents)
        celestialColor = Color(0xFF7C593E),

        // Ink: dark forest bark (softer than black)
        ink = Color(0xFF1E2A24),

        // Reflections: mossy green
        reflectionColor = Color(0xFF8F745C),

        // True depth: rainforest floor
        deepColor = Color(0xFF0F1F17),

        // Controlled brightness: fresh leaf
        brightColor = Color(0xFFAF906F),

        // Warm accent: sunlit bark / dried leaves
        warmColor = Color(0xFFEABC79),

        // Structural accents: wet wood / vines
        deepAccent = Color(0xFF2F5E46),

        // Highlights: soft mist light
        highlight = Color(0xFFE9F2EC),

        // Clouds / fog through canopy
        cloudColor = Color(0xFF7FAE95),
        sky = Color(0xFFE4FFF7),
        nearHorizon = Color(0xFFE3AB7B)
    )

    fun getTheme(): ColorTheme {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return if (hour in 5..17) nightTheme else nightTheme
    }
}