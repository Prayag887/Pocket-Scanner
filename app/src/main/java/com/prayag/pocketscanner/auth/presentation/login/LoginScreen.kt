package com.prayag.pocketscanner.auth.presentation.login

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayag.pocketscanner.auth.domain.model.User
import com.prayag.pocketscanner.auth.presentation.utils.GoogleSignInButton
import com.prayag.pocketscanner.auth.presentation.utils.StarrySkyBackground
import com.prayag.pocketscanner.auth.presentation.utils.generateRandomSnowflake
import com.prayag.pocketscanner.auth.presentation.utils.generateRandomStar
import com.prayag.pocketscanner.ui.theme.VibrantGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (User) -> Unit,
    onGoogleSignInClick: () -> Unit,
    animationState: SkyAnimationState
) {
    val authState by viewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val showTitle = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        showTitle.value = true
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
        StarrySkyBackground(animationState)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x552A1B54), Color(0x33362870))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pocket",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .background(VibrantGreen, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Text(
                    text = "Scanner",
                    color = VibrantGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .background(Color.White, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Document scanning made simple",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            GoogleSignInButton(
                onClick = {
                    scope.launch {
                        // Pass the activity context to the ViewModel
                        viewModel.signInWithGoogle(context)
                    }
                },
                enabled = authState !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }

                is AuthState.Success -> {
                    val user = (authState as AuthState.Success).user
                    LaunchedEffect(Unit) {
                        onLoginSuccess(user)
                    }
                }

                is AuthState.ProceedWithoutLogin -> {
                    LaunchedEffect(Unit) {
                        Toast.makeText(
                            context,
                            "Could not sign in. You can still use the app, but cloud sync won't be available in future.",
                            Toast.LENGTH_LONG
                        ).show()
                        onLoginSuccess(
                            User(
                                uid = "guest-${System.currentTimeMillis()}",
                                name = "Guest User",
                                email = ""
                            )
                        )
                    }
                }

                is AuthState.Error -> {
                    val message = (authState as AuthState.Error).message
                    Text(
                        text = message,
                        color = Color(0xFFFF6B6B),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                else -> Unit
            }
        }

            // Bottom text
        Text(
            text = "© 2025 Pocket Scanner",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}


// Data class to hold pre-computed animation values for starry sky
data class SkyAnimationState(
    val scanLine: Float = 0f,
    val scanLineAlpha: Float = 0.4f,
    val stars: List<Star> = List(150) { generateRandomStar() },
    val snowflakes: List<Snowflake> = List(80) { generateRandomSnowflake() }
)

data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val pulsePhase: Float
)

data class Snowflake(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val wobbleOffset: Float,
    val wobbleSpeed: Float
)