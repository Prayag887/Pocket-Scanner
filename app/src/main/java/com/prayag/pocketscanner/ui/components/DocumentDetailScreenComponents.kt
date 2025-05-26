package com.prayag.pocketscanner.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.presentation.viewmodels.DocumentViewModel
import com.prayag.pocketscanner.ui.theme.VibrantOrange
import com.prayag.pocketscanner.utils.Utils
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.sqrt
import androidx.core.net.toUri


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewTab(
    document: Document,
    onEdit: (Document, Int) -> Unit = { _, _ -> },
    onShare: (Document) -> Unit = {},
    navigateBack: () -> Unit = {},
    onDeleteSuccess: () -> Unit = {}
) {
    val viewModel: DocumentViewModel = koinViewModel()
    val pagerState = rememberPagerState { document.pages.size }
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        // scroll thumbnail strip (if needed)
    }

    Column(Modifier.fillMaxSize()) {
        var userScrollEnabled by remember { mutableStateOf(true) }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // This takes up remaining space
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            userScrollEnabled = userScrollEnabled,
        ) { pageIndex ->
            val page = document.pages[pageIndex]
            PagePreview(
                page = page,
                context = context,
                viewModel = viewModel,
                pageIndex = pageIndex,
                documentId = document.id,
                currentPage = pagerState.currentPage,
                onUserScrollChanged = { enabled -> userScrollEnabled = enabled }
            )
        }

        // Move these components INSIDE the Column
//        LiquidPageNavigationButtons(
//            currentPage = pagerState.currentPage,
//            totalPages = document.pages.size,
//            onPreviousPage = {
//                coroutineScope.launch {
//                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
//                }
//            },
//            onNextPage = {
//                coroutineScope.launch {
//                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
//                }
//            }
//        )

        if (document.pages.size > 1) {
            PageThumbnails(
                document = document,
                pagerState = pagerState,
                viewModel = viewModel
            )
        }

        ActionButtons(
            pagerState = pagerState,
            document = document,
            onEdit = onEdit,
            onShare = onShare,
            context = context,
            viewModel = viewModel,
            navigateBack = navigateBack
        )
    } // Close Column here
}


// Outside composable (top-level), create dispatcher once
private val pdfRenderDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()


@Composable
fun PagePreview(
    page: Page,
    context: Context,
    viewModel: DocumentViewModel,
    pageIndex: Int,
    documentId: String,
    currentPage: Int,
    onUserScrollChanged: (Boolean) -> Unit
) {
    val fileExtension = remember(page.imageUri) {
        page.imageUri.substringAfterLast('.', "").substringBefore('#').lowercase()
    }

    if (abs(currentPage - pageIndex) > 2) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading...")
        }
        return
    }

    val modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        when (fileExtension) {
            "png", "jpg", "jpeg" -> {
                val imageRequest = remember(page.imageUri) {
                    ImageRequest.Builder(context)
                        .data(page.imageUri)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .scale(Scale.FIT)
                        .size(1080, 1920)
                        .allowHardware(true)
                        .build()
                }

                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    },
                    error = { error ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Failed to load image",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Path: ${page.imageUri}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    success = { success ->
                        val painter = success.painter
                        ZoomableImageWithScrollControl(
                            painter = painter,
                            modifier = Modifier.fillMaxSize(),
                            onUserScrollChanged = onUserScrollChanged
                        )
                    }
                )
            }

            "pdf" -> {
                val cacheKey = "$documentId:$pageIndex"
                val bitmap by produceState<Bitmap?>(initialValue = null, key1 = pageIndex) {
                    try {
                        value = viewModel.getCachedBitmap(cacheKey)
                        if (value == null) {
                            val filePath = page.imageUri.substringBefore("#")
                            val file = File(filePath)

                            if (!file.exists()) {
                                return@produceState
                            }

                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )

                            value = withContext(pdfRenderDispatcher) {
                                Utils().renderPdfPage(context, uri, page.order)
                            }?.also {
                                viewModel.cacheBitmap(cacheKey, it)
                            }
                        }
                    } catch (e: Exception) {
                        value = null
                    }
                }

                if (bitmap != null) {
                    ZoomableBitmapImageWithScrollControl(
                        bitmap = bitmap!!.asImageBitmap(),
                        modifier = Modifier.fillMaxSize(),
                        onUserScrollChanged = onUserScrollChanged
                    )
                } else {
                    LoadingView("Loading PDF...")
                }
            }

            else -> LoadingView("Unsupported: .$fileExtension")
        }
    }
}

@Composable
fun ZoomableImageWithScrollControl(
    painter: Painter,
    modifier: Modifier = Modifier,
    onUserScrollChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val coroutineScope = rememberCoroutineScope()

    // Track if we're currently zoomed in
    LaunchedEffect(scale) {
        onUserScrollChanged(scale <= 1f)
    }

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        offset = Offset.Zero
                        onUserScrollChanged(scale <= 1f)
                    }
                )
            }
            .pointerInput(Unit) {
                coroutineScope.launch {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.changes.size) {
                                2 -> { // Pinch-to-zoom
                                    onUserScrollChanged(false)
                                    val change1 = event.changes[0]
                                    val change2 = event.changes[1]

                                    val distanceCurrent = calculateDistance(
                                        change1.position,
                                        change2.position
                                    )
                                    val distancePrevious = calculateDistance(
                                        change1.previousPosition,
                                        change2.previousPosition
                                    )

                                    val newScale = (scale * (distanceCurrent / distancePrevious))
                                        .coerceIn(0.5f, 5f)

                                    // Apply panning with constraints
                                    val newOffset = if (newScale > 1f && size != IntSize.Zero) {
                                        val maxOffsetX = (size.width * (newScale - 1)) / 2
                                        val maxOffsetY = (size.height * (newScale - 1)) / 2

                                        Offset(
                                            (offset.x + change1.positionChange().x).coerceIn(-maxOffsetX, maxOffsetX),
                                            (offset.y + change1.positionChange().y).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    } else {
                                        Offset.Zero
                                    }

                                    scale = newScale
                                    offset = newOffset
                                }

                                1 -> { // Single-finger drag
                                    if (scale > 1f) {
                                        val change = event.changes[0]
                                        val maxOffsetX = (size.width * (scale - 1)) / 2
                                        val maxOffsetY = (size.height * (scale - 1)) / 2

                                        offset = Offset(
                                            (offset.x + change.positionChange().x).coerceIn(-maxOffsetX, maxOffsetX),
                                            (offset.y + change.positionChange().y).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
    )
}

@Composable
fun ZoomableBitmapImageWithScrollControl(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    onUserScrollChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val coroutineScope = rememberCoroutineScope()

    // Track if we're currently zoomed in
    LaunchedEffect(scale) {
        onUserScrollChanged(scale <= 1f)
    }

    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        offset = Offset.Zero
                        onUserScrollChanged(scale <= 1f)
                    }
                )
            }
            .pointerInput(Unit) {
                coroutineScope.launch {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.changes.size) {
                                2 -> { // Pinch-to-zoom
                                    onUserScrollChanged(false)
                                    val change1 = event.changes[0]
                                    val change2 = event.changes[1]

                                    val distanceCurrent = calculateDistance(
                                        change1.position,
                                        change2.position
                                    )
                                    val distancePrevious = calculateDistance(
                                        change1.previousPosition,
                                        change2.previousPosition
                                    )

                                    val newScale = (scale * (distanceCurrent / distancePrevious))
                                        .coerceIn(0.5f, 5f)

                                    // Apply panning with constraints
                                    val newOffset = if (newScale > 1f && size != IntSize.Zero) {
                                        val maxOffsetX = (size.width * (newScale - 1)) / 2
                                        val maxOffsetY = (size.height * (newScale - 1)) / 2

                                        Offset(
                                            (offset.x + change1.positionChange().x).coerceIn(-maxOffsetX, maxOffsetX),
                                            (offset.y + change1.positionChange().y).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    } else {
                                        Offset.Zero
                                    }

                                    scale = newScale
                                    offset = newOffset
                                }

                                1 -> { // Single-finger drag
                                    if (scale > 1f) {
                                        val change = event.changes[0]
                                        val maxOffsetX = (size.width * (scale - 1)) / 2
                                        val maxOffsetY = (size.height * (scale - 1)) / 2

                                        offset = Offset(
                                            (offset.x + change.positionChange().x).coerceIn(-maxOffsetX, maxOffsetX),
                                            (offset.y + change.positionChange().y).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
    )
}

// Helper function from your original code
private fun calculateDistance(point1: Offset, point2: Offset): Float {
    val dx = point1.x - point2.x
    val dy = point1.y - point2.y
    return sqrt(dx * dx + dy * dy)
}

@Composable
fun LoadingView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(8.dp))
        Text(message)
    }
}

@Composable
fun PageThumbnails(
    document: Document,
    pagerState: PagerState,
    viewModel: DocumentViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        itemsIndexed(document.pages) { index, page ->
            val fileExtension = page.imageUri.substringAfterLast('.', "").substringBefore('#').lowercase()
            val isSelected = pagerState.currentPage == index
            val borderColor = if (isSelected) VibrantOrange else Color.Gray

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                ) {
                    when (fileExtension) {
                        "png", "jpg", "jpeg" -> {
                            val thumbnailRequest = remember(page.imageUri) {
                                ImageRequest.Builder(context)
                                    .data(page.imageUri)
                                    .size(100)
                                    .crossfade(true)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build()
                            }

                            SubcomposeAsyncImage(
                                model = thumbnailRequest,
                                contentDescription = "Thumbnail ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "?",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            )
                        }

                        "pdf" -> {
                            val cacheKey = "${document.id}:$index"
                            val bitmap = viewModel.getCachedBitmap(cacheKey)

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "PDF Thumbnail ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("PDF", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        else -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("?", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) VibrantOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActionButtons(
    pagerState: PagerState,
    document: Document,
    onEdit: (Document, Int) -> Unit,
    onShare: (Document) -> Unit,
    context: Context,
    viewModel: DocumentViewModel,
    navigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(Icons.Default.Edit, "Edit") {
            onEdit(document, pagerState.currentPage)
        }

        ActionButton(Icons.Default.Share, "Share") {
            val imageUri = document.pages.getOrNull(pagerState.currentPage)?.imageUri
            if (imageUri != null) {
                val file = File(imageUri.toUri().path ?: return@ActionButton)
                val fileUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider", // make sure to match manifest
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                try {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Document Page"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No app found to share", Toast.LENGTH_SHORT).show()
                }
            }
        }

        ActionButton(
            icon = Icons.Default.Delete,
            contentDesc = "Delete",
            tint = MaterialTheme.colorScheme.error
        ) {
            val fileUri = document.pages[pagerState.currentPage].imageUri
            val fileName = File(fileUri.substringBefore("#")).name

            viewModel.deleteDocument(context, fileName) { success ->
                Toast.makeText(
                    context,
                    if (success) "Deleted" else "Failed to delete",
                    Toast.LENGTH_SHORT
                ).show()

                if (success) navigateBack()
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDesc: String,
    tint: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    FilledTonalIconButton(onClick = onClick) {
        Icon(icon, contentDescription = contentDesc, tint = tint)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DetailsTab(document: Document) {
    val formattedDate = remember(document.createdAt) {
        SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(document.createdAt))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { AnimatedDetailItem("Document Name", document.title) }
        item { AnimatedDetailItem("Created On", formattedDate) }
        item { AnimatedDetailItem("Total Pages", document.pages.size.toString()) }
        item {
            AnimatedDetailItem(
                "Document Type",
                document.format
            )
        }
    }
}

@Composable
fun AnimatedDetailItem(title: String, value: String) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun TextTab(document: Document) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(400)) + expandIn(expandFrom = Alignment.Center),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This would display the extracted text from the document.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}