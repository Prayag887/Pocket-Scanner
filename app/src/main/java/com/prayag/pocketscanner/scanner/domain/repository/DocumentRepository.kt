package com.prayag.pocketscanner.scanner.domain.repository

import android.net.Uri
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.presentation.states.cloudstorage.CloudProvider
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    // Existing functions
    suspend fun saveDocument(document: Document)
    suspend fun deleteDocument(id: String)
    suspend fun getAllDocuments(desiredFormat: String): Flow<List<Document>>
    suspend fun getDocumentById(id: String, desiredFormat: String): Document?
    suspend fun getPagesForDocument(documentId: String, desiredFormat: String): List<Page>

    // External PDF functions
    suspend fun openExternalPdf(uri: Uri): Document?
    suspend fun importPdfFromDevice(uri: Uri): Document?
    suspend fun importPdfFromCloudStorage(uri: Uri, cloudProvider: CloudProvider): Document?
    suspend fun validatePdfAccess(uri: Uri): Boolean
    suspend fun extractPagesFromPdf(uri: Uri): List<Page>
    suspend fun getPdfMetadata(uri: Uri): PdfMetadata?
    suspend fun cachePdfLocally(uri: Uri): String? // Returns local cache path
    suspend fun syncWithCloudStorage(documentId: String, cloudProvider: CloudProvider): Boolean
}

data class PdfMetadata(
    val title: String?,
    val author: String?,
    val creationDate: String?,
    val pageCount: Int,
    val fileSize: Long,
    val isPasswordProtected: Boolean
)