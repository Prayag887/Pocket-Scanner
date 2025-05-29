package com.prayag.pocketscanner

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prayag.pocketscanner.auth.presentation.login.LoginScreen
import com.prayag.pocketscanner.auth.presentation.login.LoginViewModel
import com.prayag.pocketscanner.auth.presentation.login.SkyAnimationState
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.presentation.screens.DocumentDetailScreen
import com.prayag.pocketscanner.scanner.presentation.screens.HomeScreen
import com.prayag.pocketscanner.scanner.presentation.screens.ScanScreen
import com.prayag.pocketscanner.scanner.presentation.viewmodels.DocumentViewModel
import com.prayag.pocketscanner.splash.presentation.screen.SplashScreen
import com.prayag.pocketscanner.ui.theme.PocketScannerTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {
    private var externalPdfUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

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
                    PocketScannerApp(initialPdfUri = externalPdfUri)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                if (uri.toString().endsWith(".pdf", ignoreCase = true) ||
                    intent.type == "application/pdf") {
                    externalPdfUri = uri
                }
            }
        }
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PocketScannerApp(initialPdfUri: Uri? = null) {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = koinViewModel()
    var sharedSkyAnimation by remember { mutableStateOf(SkyAnimationState()) }

    val startDestination = if (initialPdfUri != null) "external_pdf" else "splash"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Add external PDF viewer route
        composable("external_pdf") {
            if (initialPdfUri != null) {
                ExternalPdfViewerScreen(
                    pdfUri = initialPdfUri,
                    navigateBack = {
                        // Finish the activity when viewing external PDFs
                        navController.navigate("home") {
                            popUpTo("external_pdf") { inclusive = true }
                        }
                    }
                )
            }
        }

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

@Composable
fun ExternalPdfViewerScreen(
    pdfUri: Uri,
    navigateBack: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // Create a Document object from the external PDF URI
    val externalDocument = createDocumentFromUri(pdfUri)

    DocumentDetailScreen(
        document = externalDocument,
        navigateBack = navigateBack,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { selectedTabIndex = it },
        externalPdfUri = pdfUri,
        externalPdfName = externalDocument.title,
        isExternalDocument = true
    )
}

fun createDocumentFromUri(uri: Uri): Document {
    val fileName = uri.lastPathSegment ?: "External PDF"
    val cleanFileName = if (fileName.endsWith(".pdf", ignoreCase = true)) {
        fileName.substring(0, fileName.length - 4)
    } else {
        fileName
    }

    return Document(
        id = "external_${uri.hashCode()}",
        title = cleanFileName,
        score = 0,
        createdAt = System.currentTimeMillis(),
        pages = listOf(
            Page(
                id = "external_page_1",
                imageUri = uri.toString(),
                order = 0
            )
        ),
        format = "pdf",
        tags = listOf("external")
    )
}