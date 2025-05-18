package com.example.pocketscanner.domain.usecase

import com.example.pocketscanner.domain.model.Document
import com.example.pocketscanner.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow

class GetDocumentsUseCase(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(desiredFormat: String): Flow<List<Document>> {
        return documentRepository.getAllDocuments(desiredFormat)
    }
}