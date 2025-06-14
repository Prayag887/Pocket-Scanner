package com.prayag.pocketscanner.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.presentation.viewmodels.DocumentViewModel
import org.koin.androidx.compose.koinViewModel

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

//    LaunchedEffect(pagerState.currentPage) {
//        // scroll thumbnail strip (if needed)
//    }

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            pageSpacing = 4.dp,
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