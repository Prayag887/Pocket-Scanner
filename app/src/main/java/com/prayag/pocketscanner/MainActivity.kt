package com.prayag.pocketscanner

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.prayag.pocketscanner.theme.ThemeProvider
import com.prayag.pocketscanner.weather.presentation.sunny.WeatherSunScreen
//import com.prayag.pocketscanner.ui.theme.PocketScannerTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AnimatedWeatherSunScreen()
        }
    }
}

@Composable
fun AnimatedWeatherSunScreen() {
    var timeProgress by remember { mutableFloatStateOf(0.1f) }

    val theme = remember { ThemeProvider.getTheme() }
    val isDay = theme == ThemeProvider.getTheme() && theme.ink != Color(0xFFE8E8E8)

    // Set system icon color
    SystemBarIconColor(
        isDarkIcons = isDay // true = black icons, false = white icons
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            timeProgress += 0.002f
            if (timeProgress > 1f) timeProgress = 0f
        }
    }

    WeatherSunScreen()
}



@Composable
fun SystemBarIconColor(isDarkIcons: Boolean) {
    val view = LocalView.current
    val activity = view.context as Activity

    LaunchedEffect(isDarkIcons) {
        val controller = WindowInsetsControllerCompat(
            activity.window,
            view
        )
        controller.isAppearanceLightStatusBars = isDarkIcons
    }
}