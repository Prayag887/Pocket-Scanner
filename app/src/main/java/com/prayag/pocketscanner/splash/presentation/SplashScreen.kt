package com.prayag.pocketscanner.splash.presentation

import android.widget.Toast
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.prayag.pocketscanner.auth.domain.model.User
import com.prayag.pocketscanner.auth.presentation.login.AuthState
import com.prayag.pocketscanner.auth.presentation.login.LoginViewModel
import com.prayag.pocketscanner.auth.presentation.login.SkyAnimationState
import com.prayag.pocketscanner.auth.presentation.utils.StarrySkyBackground
import com.prayag.pocketscanner.auth.presentation.utils.precomputeAnimationValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (User) -> Unit,
    onLoginFailed: (SkyAnimationState) -> Unit
) {
    val animationState = remember { mutableStateOf(SkyAnimationState()) }
    val alpha = remember { Animatable(0f) }
    val authState by viewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Precompute starry background
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            precomputeAnimationValues(animationState)
        }
    }

    // Fade in animation and trigger login
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 1000))
        delay(5200)
        viewModel.tryAutoLogin(context)
    }

    // Handle login result and fade out before navigating
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                alpha.animateTo(0f, animationSpec = tween(durationMillis = 1000))
                onLoginSuccess((authState as AuthState.Success).user)
            }

            is AuthState.Error -> {
                alpha.animateTo(0f, animationSpec = tween(durationMillis = 1000))
                Toast.makeText(context, "Login failed, may affect cloud sync in future", Toast.LENGTH_SHORT).show()
                onLoginFailed(animationState.value)
            }

            else -> Unit
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
