package com.prayag.pocketscanner.scanner.domain.repository

import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    suspend fun saveDocument(document: Document)
    suspend fun deleteDocument(id: String)
    suspend fun getAllDocuments(desiredFormat: String): Flow<List<Document>>
    suspend fun getDocumentById(id: String, desiredFormat: String): Document?
    suspend fun getPagesForDocument(documentId: String, desiredFormat: String): List<Page>
}