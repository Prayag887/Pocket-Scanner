package com.prayag.pocketscanner.scanner.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.presentation.viewmodels.DocumentViewModel
import com.prayag.pocketscanner.scanner.presentation.viewmodels.DocumentsUiState
import com.prayag.pocketscanner.ui.components.DocumentCard
import com.prayag.pocketscanner.ui.components.ScanButton
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToScan: () -> Unit,
    navigateToDocument: (String) -> Unit,
    viewModel: DocumentViewModel = koinViewModel(),
    refreshTrigger: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshDocuments()
    }

    HomeScreenContent(
        uiState = uiState,
        navigateToScan = navigateToScan,
        navigateToDocument = navigateToDocument
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: DocumentsUiState,
    navigateToScan: () -> Unit,
    navigateToDocument: (String) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "PocketScanner",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ScanButton { navigateToScan() }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            DocumentStatsCard(uiState.documents)

            DocumentListHeader()

            Crossfade(
                targetState = Triple(uiState.isLoading, uiState.documents.isEmpty(), uiState.documents),
                label = "DocumentStateCrossfade"
            ) { (isLoading, isEmpty, documents) ->
                when {
                    isLoading -> LoadingState()
                    isEmpty -> EmptyDocumentsState()
                    else -> DocumentsList(
                        documents = documents.sortedByDescending { it.createdAt },
                        navigateToDocument = navigateToDocument
                    )
                }
            }

        }
    }

    ScanningOverlay(isVisible = uiState.isScanning)
}

@Composable
private fun DocumentStatsCard(documents: List<Document>) {
    val totalDocs = documents.size
    val totalPages = documents.sumOf { it.pages.size }
    val avgPages = if (totalDocs > 0) totalPages / totalDocs else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize(), // smooth size changes
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatsItem(label = "Documents", value = "$totalDocs")
                StatsItem(label = "Pages", value = "$totalPages")
                StatsItem(label = "Avg Pages", value = "$avgPages")
            }
        }
    }
}


@Composable
private fun StatsItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun DocumentListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent Documents",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "View All",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyDocumentsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No documents yet",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap the scan button to create your first document",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DocumentsList(
    documents: List<Document>,
    navigateToDocument: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        itemsIndexed(documents, key = { _, item -> item.id }) { index, document ->
            val delayMillis = index * 50
            val enter = slideInVertically(
                initialOffsetY = { fullHeight -> -50 },
                animationSpec = tween(durationMillis = 300, delayMillis = delayMillis)
            ) + fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = delayMillis))

            AnimatedVisibility(
                visible = true,
                enter = enter
            ) {
                DocumentCard(
                    document = document,
                    onClick = {
                        navigateToDocument(document.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ScanningOverlay(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 6.dp
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "Scanning Document...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Hold steady for best results",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val mockDocuments = listOf(
        Document(
            id = UUID.randomUUID().toString(),
            title = "Invoice May 2025",
            createdAt = System.currentTimeMillis(),
            pages = listOf(Page(UUID.randomUUID().toString(), "", 0)),
            score = 0,
            format = "png"
        ),
        Document(
            id = UUID.randomUUID().toString(),
            title = "Contract Draft",
            createdAt = System.currentTimeMillis() - 86400000,
            pages = listOf(Page(UUID.randomUUID().toString(), "", 0)),
            score = 0,
            format = "jpg"
        )
    )
    val previewState = DocumentsUiState(
        documents = mockDocuments,
        isLoading = false,
        isScanning = false
    )
    HomeScreenContent(
        uiState = previewState,
        navigateToScan = {},
        navigateToDocument = {}
    )
}
