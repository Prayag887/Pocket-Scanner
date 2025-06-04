package com.prayag.pocketscanner.scanner.presentation.states.externaldocument

import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.domain.repository.PdfMetadata

data class ExternalPdfUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val isBatchProcessing: Boolean = false,
    val isLoadingMetadata: Boolean = false,
    val isExtractingPages: Boolean = false,

    // Document states
    val currentDocument: Document? = null,
    val importedDocument: Document? = null,
    val isDocumentReady: Boolean = false,

    // Import results
    val importSuccess: Boolean = false,
    val batchProcessSuccess: Boolean = false,
    val batchProcessResult: BatchProcessResult? = null,

    // PDF data
    val pdfMetadata: PdfMetadata? = null,
    val extractedPages: List<Page> = emptyList(),

    // Error handling
    val error: String? = null
) {
    val hasError: Boolean get() = error != null
    val isProcessing: Boolean get() = isLoading || isImporting || isBatchProcessing || isLoadingMetadata || isExtractingPages
    val hasCurrentDocument: Boolean get() = currentDocument != null
    val hasImportedDocument: Boolean get() = importedDocument != null
    val hasPdfMetadata: Boolean get() = pdfMetadata != null
    val hasExtractedPages: Boolean get() = extractedPages.isNotEmpty()
    val canPerformOperations: Boolean get() = !isProcessing && hasCurrentDocument
}


