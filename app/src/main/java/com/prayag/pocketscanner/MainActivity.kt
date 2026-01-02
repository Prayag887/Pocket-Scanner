package com.prayag.pocketscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
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

    // Auto-animate the sun from sunrise to sunset
    LaunchedEffect(Unit) {
        while (true) {
            delay(50) // Update every 50ms
            timeProgress += 0.002f
            if (timeProgress > 1f) {
                timeProgress = 0f // Reset to sunrise
            }
        }
    }

//    val state = SunTimeUiState(
//        temperature = 26,
//        condition = "Sunny",
//        city = "Tokyo",
//    )

    WeatherSunScreen()
}