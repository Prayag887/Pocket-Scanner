package com.prayag.pocketscanner.presentation.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.prayag.pocketscanner.presentation.viewmodels.DocumentViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScanScreen(
    navigateHome: () -> Unit,
    onDocumentScanned: (success: Boolean, filePath: String?) -> Unit,
    viewModel: DocumentViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    var scannedDocumentPages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var isPdf by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var pendingNavigation by remember { mutableStateOf(false) }

    val options = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(20)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }

    val scanner = remember { GmsDocumentScanning.getClient(options) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // On scanner result or cancel:
        if (result.resultCode == Activity.RESULT_OK) {
            isLoading = false
            try {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)

                scanResult?.let {
                    when {
                        !it.pages.isNullOrEmpty() -> {
                            scannedDocumentPages = it.pages?.map { page -> page.imageUri } ?: emptyList()
                            isPdf = false
                            showSaveDialog = true
                        }

                        it.pdf != null -> {
                            pdfUri = it.pdf!!.uri
                            isPdf = true
                            showSaveDialog = true
                        }

                        else -> {
                            onDocumentScanned(false, null)
                            pendingNavigation = true
                        }
                    }
                } ?: run {
                    onDocumentScanned(false, null)
                    pendingNavigation = true
                }
            } catch (e: Exception) {
                onDocumentScanned(false, null)
                pendingNavigation = true
            }
        } else {
            // User cancelled or error: show loading, then navigate
            isLoading = true
            onDocumentScanned(false, null)
            pendingNavigation = true
        }
    }

    LaunchedEffect(Unit) {
        activity?.let {
            scanner.getStartScanIntent(it)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener {
                    isLoading = true
                    onDocumentScanned(false, null)
                    pendingNavigation = true
                }
        }
    }

    // When pending navigation is true, delay a bit to let loading show, then navigate
    LaunchedEffect(pendingNavigation) {
        if (pendingNavigation) {
            delay(300) // short delay so loading animation is visible
            navigateHome()
        }
    }

    when {
        showSaveDialog -> {
            SaveDocumentScreen(
                previewImageUri = scannedDocumentPages.firstOrNull(),
                onDismiss = navigateHome,
                onNavigateBack = navigateHome,
                onSave = { fileName, format ->
                    coroutineScope.launch {
                        try {
                            if (isPdf && pdfUri != null) {
                                // Save directly from PDF
                                onDocumentScanned(true, pdfUri.toString())
                            } else {
                                // Merge and save images
                                viewModel.mergeAndSaveImages(
                                    scannedDocumentPages,
                                    context.contentResolver,
                                    context.filesDir,
                                    fileName,
                                    format
                                )
                                onDocumentScanned(true, "saved_file_path_placeholder")
                            }
                        } catch (e: Exception) {
                            onDocumentScanned(false, null)
                        } finally {
                            navigateHome()
                        }
                    }
                }
            )
        }
        isLoading -> {
            // Show dark loading screen while waiting for scanner or navigating away
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        else -> {
            // Safety fallback, shouldn't get here normally because of pendingNavigation effect
        }
    }
}
