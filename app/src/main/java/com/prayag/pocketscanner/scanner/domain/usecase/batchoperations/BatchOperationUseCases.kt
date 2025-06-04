package com.prayag.pocketscanner.scanner.domain.usecase.batchoperations

import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.usecase.document.DeleteDocumentUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.document.SaveDocumentUseCase

class BatchDeleteDocumentsUseCase(
    private val deleteDocumentUseCase: DeleteDocumentUseCase
) {
    suspend operator fun invoke(documentIds: List<String>): Result<BatchResult> {
        val successfulIds = mutableListOf<String>()
        val failedIds = mutableListOf<Pair<String, String>>()

        documentIds.forEach { id ->
            try {
                deleteDocumentUseCase(id)
                successfulIds.add(id)
            } catch (e: Exception) {
                failedIds.add(id to (e.message ?: "Unknown error"))
            }
        }

        return Result.success(
            BatchResult(
                successCount = successfulIds.size,
                failureCount = failedIds.size,
                failedItems = failedIds.map { it.second }
            )
        )
    }
}

class BatchSaveDocumentsUseCase(
    private val saveDocumentUseCase: SaveDocumentUseCase
) {
    suspend operator fun invoke(documents: List<Document>): Result<BatchResult> {
        val successfulDocs = mutableListOf<String>()
        val failedDocs = mutableListOf<Pair<String, String>>()

        documents.forEach { document ->
            try {
                saveDocumentUseCase(document)
                successfulDocs.add(document.id)
            } catch (e: Exception) {
                failedDocs.add(document.id to (e.message ?: "Unknown error"))
            }
        }

        return Result.success(
            BatchResult(
                successCount = successfulDocs.size,
                failureCount = failedDocs.size,
                failedItems = failedDocs.map { it.second }
            )
        )
    }
}

data class BatchResult(
    val successCount: Int,
    val failureCount: Int,
    val failedItems: List<String>
) {
    val totalProcessed: Int get() = successCount + failureCount
    val hasFailures: Boolean get() = failureCount > 0
}