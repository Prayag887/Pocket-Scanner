package com.example.pocketscanner.domain.repository

import com.example.pocketscanner.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<Document>>
    suspend fun getDocumentById(id: String): Document?
    suspend fun saveDocument(document: Document)
    suspend fun deleteDocument(id: String)
}