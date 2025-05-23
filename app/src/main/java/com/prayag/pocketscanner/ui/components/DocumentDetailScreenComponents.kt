package com.prayag.pocketscanner.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.presentation.viewmodels.DocumentViewModel
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

    LaunchedEffect(pagerState.currentPage) {
        // scroll thumbnail strip (if needed)
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Page ${pagerState.currentPage + 1} of ${document.pages.size}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            val page = document.pages[pageIndex]
            PagePreview(
                page = page,
                context = context,
                viewModel = viewModel,
                pageIndex = pageIndex,
                documentId = document.id,
                currentPage = pagerState.currentPage
            )
        }

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
    }
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
    currentPage: Int
) {
    val fileExtension = remember(page.imageUri) {
        page.imageUri.substringAfterLast('.', "").substringBefore('#').lowercase()
    }

    if (abs(currentPage - pageIndex) > 2) {
        // Do not render distant pages
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
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(page.imageUri)
                        .size(1080) // Limit resolution for performance
                        .build()
                )

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (painter.state is AsyncImagePainter.State.Loading) {
                        CircularProgressIndicator()
                    }
                }
            }

            "pdf" -> {
                val cacheKey = "$documentId:$pageIndex"
                val bitmap by produceState<Bitmap?>(initialValue = null, key1 = pageIndex) {
                    value = viewModel.getCachedBitmap(cacheKey)
                    if (value == null) {
                        val file = File(page.imageUri.substringBefore("#"))
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        value = withContext(pdfRenderDispatcher) {
                            Utils().renderPdfPage(context, uri, page.order)
                        }?.also {
                            viewModel.cacheBitmap(cacheKey, it)
                        }
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
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

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        itemsIndexed(document.pages) { index, page ->
            val fileExtension = page.imageUri.substringAfterLast('.', "").substringBefore('#').lowercase()
            val isSelected = pagerState.currentPage == index
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray

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
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(page.imageUri)
                                    .size(100)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        "pdf" -> {
                            val cacheKey = "${document.id}:$index"
                            val bitmap = viewModel.getCachedBitmap(cacheKey)

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
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
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(Icons.Default.Edit, "Edit") {
            onEdit(document, pagerState.currentPage)
        }

        ActionButton(Icons.Default.Share, "Share") {
            onShare(document)
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
