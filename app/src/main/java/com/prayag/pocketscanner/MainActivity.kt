package com.prayag.pocketscanner

import ExternalPdfDisplayScreen
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private var pdfIntentUri: Uri? = null

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

        // Handle initial intent
        handleIntent(intent)

        setContent {
            PocketScannerTheme {
                KoinContext {
                    PocketScannerApp(pdfUri = pdfIntentUri)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri != null && (intent.type == "application/pdf" || uri.toString().endsWith(".pdf"))) {
                // Request temporary permission to read this URI
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                pdfIntentUri = uri
            }
        }
    }

}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PocketScannerApp(pdfUri: Uri? = null) {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = koinViewModel()
    var sharedSkyAnimation by remember { mutableStateOf(SkyAnimationState()) }

    // Handle external PDF intent
    LaunchedEffect(pdfUri) {
        if (pdfUri != null) {
            navController.navigate("external_pdf/${Uri.encode(pdfUri.toString())}")
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (pdfUri != null) "external_pdf/${Uri.encode(pdfUri.toString())}" else "splash"
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

        // External PDF route
        composable(
            "external_pdf/{encodedUri}",
            arguments = listOf(navArgument("encodedUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("encodedUri") ?: ""
            val decodedUri = Uri.decode(encodedUri)

            ExternalPdfDisplayScreen(
                pdfUri = decodedUri.toUri(),
                navigateBack = { navController.popBackStack() },
                navigateToHome = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
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