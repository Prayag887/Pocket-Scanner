package com.example.pocketscanner.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pocketscanner.domain.model.Document
import com.example.pocketscanner.domain.model.Page
import com.example.pocketscanner.presentation.viewmodels.DocumentViewModel
import com.example.pocketscanner.ui.components.DetailsTab
import com.example.pocketscanner.ui.components.PreviewTab
import com.example.pocketscanner.ui.components.TextTab
import com.example.pocketscanner.ui.theme.ScannerOrange
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentDetailScreen(
    document: Document?,
    navigateBack: () -> Unit,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val viewModel : DocumentViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val documentId = uiState.documents.find { it.id == document?.id }

    LaunchedEffect(documentId) {
        viewModel.loadDocumentPages(documentId.toString(), context)
    }
    Scaffold(
        topBar = {
            DocumentDetailTopBar(
                title = document?.title ?: "Loading...",
                navigateBack = navigateBack
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (uiState.isLoading) {
                LoadingContent()
            } else {
                document?.let {
                    DocumentDetailContent(
                        document = it,
                        tabs = listOf("Preview", "Details", "Text"),
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                        navigateBack = navigateBack,
                    )
                } ?: println("Document not found")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentDetailTopBar(title: String, navigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = navigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { /* Share */ }) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            IconButton(onClick = { /* Edit */ }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentDetailContent(
    document: Document,
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    navigateBack: () -> Unit,

) {
    Column(modifier = Modifier.fillMaxSize()) {
        DocumentScoreCard(document.score)
        TabsRow(tabs = tabs, selectedTabIndex = selectedTabIndex, onTabSelected = onTabSelected)
        when (selectedTabIndex) {
            0 -> PreviewTab(document, navigateBack = navigateBack)
            1 -> DetailsTab(document)
            2 -> TextTab(document)
        }
    }
}

@Composable
private fun DocumentScoreCard(score: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = ScannerOrange,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Document Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$score points earned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "+$score",
                style = MaterialTheme.typography.headlineSmall,
                color = ScannerOrange,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TabsRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    TabRow(selectedTabIndex = selectedTabIndex) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(text = title) }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun DocumentDetailScreenPreview() {
    val sampleDocument = Document(
        id = "1",
        title = "Sample Document",
        score = 42,
        createdAt = 123123L,
        pages = listOf(
            Page(id = "page1", imageUri = "https://example.com/image1.jpg", order = 0),
            Page(id = "page2", imageUri = "https://example.com/image2.jpg", order = 1, isEnhanced = true)
        ),
        format = "pdf",
        tags = listOf("tag1", "tag2")
    )

    var selectedTab by remember { mutableIntStateOf(0) }

    MaterialTheme {
        DocumentDetailScreen(
            document = sampleDocument,
            navigateBack = { /* no-op */ },
            selectedTabIndex = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}
