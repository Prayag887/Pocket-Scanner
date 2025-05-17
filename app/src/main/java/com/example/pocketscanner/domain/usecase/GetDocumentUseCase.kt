package com.example.pocketscanner.domain.usecase

import com.example.pocketscanner.domain.model.Document
import com.example.pocketscanner.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow

class GetDocumentsUseCase(
    private val documentRepository: DocumentRepository
) {
    operator fun invoke(): Flow<List<Document>> {
        return documentRepository.getAllDocuments()
    }
}
