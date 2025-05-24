package com.prayag.pocketscanner.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs

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