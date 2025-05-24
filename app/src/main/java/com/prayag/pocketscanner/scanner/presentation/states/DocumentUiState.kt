package com.prayag.pocketscanner.scanner.presentation.states

import com.prayag.pocketscanner.scanner.domain.model.Document

// Enhanced UI State with navigation states
data class DocumentsUiState(
    val documents: List<Document> = emptyList(),
    val currentDocument: Document? = null,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val isScanning: Boolean = false,
    val isDataReady: Boolean = false,
    val isDocumentReady: Boolean = false,
    val error: String? = null
)

