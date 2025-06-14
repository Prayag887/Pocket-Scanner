import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class PdfRenderMethod { NATIVE_RENDERER, WEBVIEW_LOCAL, WEBVIEW_ONLINE, EXTERNAL_APP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalPdfDisplayScreen(pdfUri: Uri, navigateBack: () -> Unit, navigateToHome: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf("External PDF") }
    var localPdfPath by remember { mutableStateOf<String?>(null) }
    var renderMethod by remember { mutableStateOf<PdfRenderMethod?>(null) }
    var showControls by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showControls) { if (showControls) { delay(3000); showControls = false } }

    LaunchedEffect(pdfUri) {
        try {
            fileName = getFileNameFromUri(context, pdfUri) ?: "External PDF"
            localPdfPath = copyPdfToLocal(context, pdfUri, fileName)
            renderMethod = determineBestRenderMethod(context, localPdfPath!!)
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error loading PDF: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1) },
                navigationIcon = { IconButton(onClick = navigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = navigateToHome) { Icon(Icons.Default.Home, "Home") }
                    IconButton(onClick = { scope.launch { sharePdf(context, pdfUri, fileName) } }) { Icon(Icons.Default.Share, "Share") }
                    IconButton(onClick = { openWithExternalApp(context, pdfUri) }) { Icon(Icons.Default.OpenInBrowser, "Open External") }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> LoadingContent()
                errorMessage != null -> ErrorContent(errorMessage!!, { isLoading = true; errorMessage = null }, { openWithExternalApp(context, pdfUri) })
                localPdfPath != null && renderMethod != null -> PdfContent(localPdfPath!!, renderMethod!!, { showControls = true }, { errorMessage = it })
            }
        }
    }
}

@Composable
private fun PdfContent(pdfPath: String, renderMethod: PdfRenderMethod, onTouchScreen: () -> Unit, onError: (String) -> Unit) {
    when (renderMethod) {
        PdfRenderMethod.NATIVE_RENDERER -> NativePdfRenderer(pdfPath, onTouchScreen, onError)
        PdfRenderMethod.WEBVIEW_LOCAL -> WebViewPdfRenderer(pdfPath, false, onTouchScreen, onError)
        PdfRenderMethod.WEBVIEW_ONLINE -> WebViewPdfRenderer(pdfPath, true, onTouchScreen, onError)
        PdfRenderMethod.EXTERNAL_APP -> ExternalAppPrompt(pdfPath)
    }
}

@Composable
private fun NativePdfRenderer(
    pdfPath: String,
    onTouchScreen: () -> Unit,
    onError: (String) -> Unit
) {
    var pageCount by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Get screen height
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // Caches: bitmaps, zoom, and pan per page
    val pageCache = remember { mutableStateMapOf<Int, Bitmap>() }
    val scales = remember { mutableStateMapOf<Int, Float>() }
    val offsets = remember { mutableStateMapOf<Int, Offset>() }

    // Load PDF info asynchronously
    LaunchedEffect(pdfPath) {
        try {
            withContext(Dispatchers.IO) {
                val file = File(pdfPath)
                val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(fileDescriptor).use { renderer ->
                    pageCount = renderer.pageCount
                }
                fileDescriptor.close()
            }
        } catch (e: Exception) {
            onError("Error loading PDF: ${e.message}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (pageCount > 0) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pageCount) { idx ->
                    PDFPageItem(
                        pageIndex = idx,
                        pdfPath = pdfPath,
                        pageCache = pageCache,
                        scales = scales,
                        offsets = offsets,
                        onTouchScreen = onTouchScreen,
                        targetHeight = screenHeight * 0.9f
                    )
                }
            }
        }

        // "Go to page" button
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Navigation, contentDescription = "Jump")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Go to Page") },
                text = {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Page (1–$pageCount)") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        pageInput.toIntOrNull()?.takeIf { it in 1..pageCount }?.let { targetPage ->
                            coroutineScope.launch {
                                listState.animateScrollToItem(targetPage - 1)
                            }
                        }
                        pageInput = ""
                        showDialog = false
                    }) { Text("Go") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                        pageInput = ""
                    }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun PDFPageItem(
    pageIndex: Int,
    pdfPath: String,
    pageCache: MutableMap<Int, Bitmap>,
    scales: MutableMap<Int, Float>,
    offsets: MutableMap<Int, Offset>,
    onTouchScreen: () -> Unit,
    targetHeight: Dp
) {
    val scale by remember { derivedStateOf { scales[pageIndex] ?: 1f } }
    val offset by remember { derivedStateOf { offsets[pageIndex] ?: Offset.Zero } }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load bitmap for this page
    LaunchedEffect(pageIndex) {
        bitmap = pageCache[pageIndex]
        if (bitmap == null) {
            try {
                val newBitmap = loadPageBitmap(pdfPath, pageIndex)
                pageCache[pageIndex] = newBitmap
                bitmap = newBitmap
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
        isLoading = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(targetHeight)
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            bitmap != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (scale > 1.01f) {
                        // Zoomed in state - show zoomable/pannable image
                        ZoomedPDFCanvas(
                            bitmap = bitmap!!,
                            scale = scale,
                            offset = offset,
                            onTransform = { newScale, newOffset ->
                                scales[pageIndex] = newScale
                                offsets[pageIndex] = newOffset
                                onTouchScreen()
                            }
                        )
                    } else {
                        // Zoomed out state - show simple image
                        SimplePDFImage(
                            bitmap = bitmap!!,
                            onDoubleTap = {
                                scales[pageIndex] = 2.5f
                                offsets[pageIndex] = Offset.Zero
                                onTouchScreen()
                            },
                            onTap = { onTouchScreen() }
                        )
                    }

                    // Page number indicator
                    Text(
                        text = "${pageIndex + 1}",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Failed to load page ${pageIndex + 1}")
                }
            }
        }
    }
}

@Composable
private fun SimplePDFImage(
    bitmap: Bitmap,
    onDoubleTap: () -> Unit,
    onTap: () -> Unit
) {
    // Use Image instead of Canvas to avoid consuming scroll events
    AsyncImage(
        model = bitmap,
        contentDescription = "PDF Page",
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onTap = { onTap() }
                )
            },
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun ZoomedPDFCanvas(
    bitmap: Bitmap,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit
) {
    var targetScale by remember { mutableFloatStateOf(scale) }
    var targetOffset by remember { mutableStateOf(offset) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Animate scale and offset changes smoothly
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    val animatedOffset by animateOffsetAsState(
        targetValue = targetOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        AsyncImage(
            model = bitmap,
            contentDescription = "PDF Page",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffset.x,
                    translationY = animatedOffset.y
                )
                .pointerInput(containerSize) {
                    detectTransformGestures(
                        panZoomLock = true
                    ) { _, pan, zoom, _ ->
                        if (containerSize == IntSize.Zero) return@detectTransformGestures

                        // Calculate new scale with smooth limits
                        val newScale = (targetScale * zoom).coerceIn(1f, 5f)

                        // Calculate image dimensions at current scale
                        val maxWidth = containerSize.width.toFloat()
                        val maxHeight = containerSize.height.toFloat()
                        val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val containerAspectRatio = maxWidth / maxHeight

                        val (imageWidth, imageHeight) = if (imageAspectRatio > containerAspectRatio) {
                            maxWidth to maxWidth / imageAspectRatio
                        } else {
                            maxHeight * imageAspectRatio to maxHeight
                        }

                        val scaledWidth = imageWidth * newScale
                        val scaledHeight = imageHeight * newScale

                        // Calculate pan boundaries
                        val maxPanX = maxOf(0f, (scaledWidth - maxWidth) / 2f)
                        val maxPanY = maxOf(0f, (scaledHeight - maxHeight) / 2f)

                        // Apply pan with boundaries
                        val newOffset = if (newScale > 1.01f) {
                            Offset(
                                (targetOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                (targetOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                            )
                        } else {
                            Offset.Zero
                        }

                        targetScale = newScale
                        targetOffset = newOffset
                        onTransform(newScale, newOffset)
                    }
                }
                .pointerInput(containerSize) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (containerSize == IntSize.Zero) return@detectTapGestures

                            val maxWidth = containerSize.width.toFloat()
                            val maxHeight = containerSize.height.toFloat()

                            if (targetScale > 1.5f) {
                                // Zoom out
                                targetScale = 1f
                                targetOffset = Offset.Zero
                                onTransform(1f, Offset.Zero)
                            } else {
                                // Zoom in to tap location
                                val newScale = 3f

                                // Calculate offset to center on tap point
                                val centerX = maxWidth / 2f
                                val centerY = maxHeight / 2f
                                val offsetX = (centerX - tapOffset.x) * (newScale - 1f)
                                val offsetY = (centerY - tapOffset.y) * (newScale - 1f)

                                targetScale = newScale
                                targetOffset = Offset(offsetX, offsetY)
                                onTransform(newScale, Offset(offsetX, offsetY))
                            }
                        }
                    )
                },
            contentScale = ContentScale.Fit
        )
    }
}

suspend fun loadPageBitmap(pdfPath: String, pageIndex: Int): Bitmap = withContext(Dispatchers.IO) {
    val file = File(pdfPath)
    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

    try {
        PdfRenderer(fd).use { renderer ->
            renderer.openPage(pageIndex).use { page ->
                val scaleFactor = 2
                val width = page.width * scaleFactor
                val height = page.height * scaleFactor

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    } finally {
        fd.close()
    }
}



@Composable
private fun WebViewPdfRenderer(pdfPath: String, useOnlineViewer: Boolean, onTouchScreen: () -> Unit, onError: (String) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true; domStorageEnabled = true; allowFileAccess = true
                    builtInZoomControls = true; displayZoomControls = false; setSupportZoom(true)
                }
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        onError("WebView error: $description")
                    }
                }
                setOnTouchListener { _, _ -> onTouchScreen(); false }
                loadUrl(if (useOnlineViewer) "https://mozilla.github.io/pdf.js/web/viewer.html?file=file://$pdfPath" else "file://$pdfPath")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ExternalAppPrompt(pdfPath: String) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("External App Required", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("This device requires an external PDF viewer.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Button(onClick = { openWithExternalApp(context, Uri.fromFile(File(pdfPath))) }) {
                Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open External")
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Loading PDF...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onOpenExternal: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.Error, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
            Text("Error Loading PDF", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetry) { Text("Retry") }
                Button(onClick = onOpenExternal) { Text("Open External") }
            }
        }
    }
}

// Helper functions
private suspend fun determineBestRenderMethod(context: Context, pdfPath: String): PdfRenderMethod {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(pdfPath)
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(fd).close(); fd.close()
            PdfRenderMethod.NATIVE_RENDERER
        } catch (e: Exception) {
            if (isWebViewAvailable(context)) PdfRenderMethod.WEBVIEW_LOCAL else PdfRenderMethod.EXTERNAL_APP
        }
    }
}

private fun isWebViewAvailable(context: Context): Boolean {
    return try { context.packageManager.getApplicationInfo("com.google.android.webview", 0); true } catch (e: Exception) { false }
}

private suspend fun copyPdfToLocal(context: Context, uri: Uri, fileName: String): String {
    return withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open PDF file")
        val cacheDir = File(context.cacheDir, "external_pdfs").apply { if (!exists()) mkdirs() }
        val localFile = File(cacheDir, fileName)
        inputStream.use { input -> FileOutputStream(localFile).use { output -> input.copyTo(output) } }
        localFile.absolutePath
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    return try {
        when (uri.scheme) {
            "content" -> context.contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) it.getString(idx) else null
                } else null
            }
            "file" -> uri.lastPathSegment?.substringAfterLast("/")
            else -> null
        }
    } catch (e: Exception) { "document_${System.currentTimeMillis()}.pdf" }
}

private fun sharePdf(context: Context, pdfUri: Uri, fileName: String) {
    try {
        context.startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND; type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share PDF"))
    } catch (e: Exception) { Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show() }
}

private fun openWithExternalApp(context: Context, pdfUri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()) {
            context.startActivity(intent)
        } else Toast.makeText(context, "No PDF viewer found. Please install one.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
}