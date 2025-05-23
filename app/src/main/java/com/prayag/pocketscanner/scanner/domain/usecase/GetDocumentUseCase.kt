package com.prayag.pocketscanner.scanner.domain.usecase

import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow

class GetDocumentsUseCase(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(desiredFormat: String): Flow<List<Document>> {
        return documentRepository.getAllDocuments(desiredFormat)
    }
}