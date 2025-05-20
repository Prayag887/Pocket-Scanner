package com.prayag.pocketscanner

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.prayag.pocketscanner.auth.presentation.login.LoginScreen
import com.prayag.pocketscanner.auth.presentation.login.LoginViewModel
import com.prayag.pocketscanner.auth.presentation.login.SkyAnimationState
import com.prayag.pocketscanner.presentation.screens.DocumentDetailScreen
import com.prayag.pocketscanner.presentation.screens.HomeScreen
import com.prayag.pocketscanner.presentation.screens.ScanScreen
import com.prayag.pocketscanner.presentation.viewmodels.DocumentViewModel
import com.prayag.pocketscanner.splash.presentation.SplashScreen
import com.prayag.pocketscanner.ui.theme.PocketScannerTheme
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

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
                PocketScannerApp()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PocketScannerApp() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = koinViewModel()

    val context = LocalContext.current
    val clientId = context.getString(R.string.default_web_client_id)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(clientId)
        .requestEmail()
        .build()
    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)
    var sharedSkyAnimation by remember { mutableStateOf(SkyAnimationState()) }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onLoginFailed = { animation ->
                    sharedSkyAnimation = animation // <-- Save state
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginRoute(
                navController = navController,
                loginViewModel = loginViewModel,
                googleSignInClient = googleSignInClient,
                animationState = sharedSkyAnimation // <-- Pass it in
            )
        }

        composable("home") {
            HomeScreen(
                navigateToScan = { navController.navigate("scan") },
                navigateToDocument = { documentId -> navController.navigate("document/$documentId") }
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
            // Define this once at the class or file level


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
    googleSignInClient: GoogleSignInClient,
    animationState: SkyAnimationState
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            loginViewModel.handleGoogleSignInResult(intent)
        }
    }

    LoginScreen(
        viewModel = loginViewModel,
        animationState = animationState,
        onLoginSuccess = {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        },
        onGoogleSignInClick = {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    )
}
