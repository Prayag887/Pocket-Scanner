// presentation/screen/SplashScreen.kt
package com.prayag.pocketscanner.splash.presentation.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun SplashScreen(
    onNavigateToHome: (SkyAnimationState) -> Unit,
    onNavigateToLogin: (SkyAnimationState) -> Unit,
    onNavigateToMainApp: (User) -> Unit
) {
    val splashViewModel: SplashViewModel = koinViewModel()
    val animationState = remember { mutableStateOf(SkyAnimationState()) }
    val alpha = remember { Animatable(0f) }
    val navigationState by splashViewModel.navigationState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Precompute starry background
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            precomputeAnimationValues(animationState)
        }
    }

    // Fade in animation and trigger splash flow
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 1000))
        delay(5200)
        splashViewModel.handleSplashFlow(context, animationState.value)
    }

    // Handle navigation based on state
    LaunchedEffect(navigationState) {
        when (navigationState) {
            is SplashNavigationState.NavigateToHome -> {
                alpha.animateTo(0f, animationSpec = tween(durationMillis = 500))
                onNavigateToHome((navigationState as SplashNavigationState.NavigateToHome).animationState)
            }

            is SplashNavigationState.NavigateToLogin -> {
                alpha.animateTo(0f, animationSpec = tween(durationMillis = 500))
                onNavigateToLogin((navigationState as SplashNavigationState.NavigateToLogin).animationState)
            }

            is SplashNavigationState.NavigateToMainApp -> {
                alpha.animateTo(0f, animationSpec = tween(durationMillis = 500))
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
                    listOf(Color(0xFF0A1029), Color(0xFF2A1B54))
                )
            )
    ) {
        StarrySkyBackground(animationState.value)
        Text(
            text = "Pocket Scanner",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(alpha.value)
        )
    }
}