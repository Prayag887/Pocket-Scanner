package com.example.pocketscanner

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pocketscanner.presentation.screens.DocumentDetailScreen
import com.example.pocketscanner.presentation.screens.HomeScreen
import com.example.pocketscanner.presentation.screens.ScanScreen
import com.example.pocketscanner.presentation.viewmodels.DocumentViewModel
import com.example.pocketscanner.ui.theme.PocketScannerTheme
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

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable(
            route = "home",
        ) {
            HomeScreen(
                navigateToScan = { navController.navigate("scan") },
                navigateToDocument = { documentId -> navController.navigate("document/$documentId") }
            )
        }

        composable(
            route = "scan",
        ) {
            ScanScreen(
                navigateHome = { navController.popBackStack(route = "home", inclusive = false) },
                onDocumentScanned = { success, filePath ->
                    if (success && filePath != null) {
                        // Handle scanned document
                    }
                }
            )
        }

        composable(
            route = "document/{documentId}",
            arguments = listOf(navArgument("documentId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
            val viewModel: DocumentViewModel = koinViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val document = uiState.documents.find { it.id == documentId }
            var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
            DocumentDetailScreen(
                document = document,
                navigateBack = {  navController.popBackStack(route = "home", inclusive = false) },
                selectedTabIndex = selectedTabIndex,
                onTabSelected = {selectedTabIndex = it}
            )
        }
    }
}