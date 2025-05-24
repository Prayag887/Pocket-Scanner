package com.prayag.pocketscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prayag.pocketscanner.auth.presentation.login.LoginScreen
import com.prayag.pocketscanner.auth.presentation.login.LoginViewModel
import com.prayag.pocketscanner.auth.presentation.login.SkyAnimationState
import com.prayag.pocketscanner.scanner.presentation.screens.DocumentDetailScreen
import com.prayag.pocketscanner.scanner.presentation.screens.HomeScreen
import com.prayag.pocketscanner.scanner.presentation.screens.ScanScreen
import com.prayag.pocketscanner.scanner.presentation.viewmodels.DocumentViewModel
import com.prayag.pocketscanner.splash.presentation.screen.SplashScreen
import com.prayag.pocketscanner.ui.theme.PocketScannerTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val cameraPermission = Manifest.permission.CAMERA

        if (ContextCompat.checkSelfPermission(this, cameraPermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(cameraPermission),
                101
            )
        }

        setContent {
            PocketScannerTheme {
                KoinContext {
                    PocketScannerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PocketScannerApp() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = koinViewModel()
    var sharedSkyAnimation by remember { mutableStateOf(SkyAnimationState()) }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToHome = { animation ->
                    sharedSkyAnimation = animation
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToLogin = { animation ->
                    sharedSkyAnimation = animation
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToMainApp = { user ->
                    // You can store the user data if needed
                    // sharedUserData = user
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginRoute(
                navController = navController,
                loginViewModel = loginViewModel,
                animationState = sharedSkyAnimation
            )
        }

        composable("home") {
//            val documentViewModel: DocumentViewModel = koinViewModel()
//            documentViewModel.uiState.collectAsState().value
            HomeScreen(
                navigateToScan = { navController.navigate("scan") },
                navigateToDocument = { documentId -> navController.navigate("document/$documentId") },
                refreshTrigger = false
            )
        }

        composable("scan") {
            val documentViewModel: DocumentViewModel = koinViewModel()
            ScanScreen(
                navigateHome = { navController.popBackStack(route = "home", inclusive = false) },
                onDocumentScanned = { success, filePath ->
                    if (success && filePath != null) {
                        documentViewModel.refreshDocuments()
                    }
                }
            )
        }

        composable(
            "document/{documentId}",
            arguments = listOf(navArgument("documentId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
            val viewModel: DocumentViewModel = koinViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val document = uiState.documents.find { it.id == documentId }
            var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

            DocumentDetailScreen(
                document = document,
                navigateBack = { navController.popBackStack(route = "home", inclusive = false) },
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )
        }
    }
}

@Composable
fun LoginRoute(
    navController: NavController,
    loginViewModel: LoginViewModel,
    animationState: SkyAnimationState
) {
    LoginScreen(
        viewModel = loginViewModel,
        animationState = animationState,
        onLoginSuccess = {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        },
        onGoogleSignInClick = {}
    )
}