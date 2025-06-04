package com.prayag.pocketscanner.scanner.domain.usecase.document

import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow

class GetAllDocumentsUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(desiredFormat: String = "pdf"): Flow<List<Document>> {
        return repository.getAllDocuments(desiredFormat)
    }
}

class GetDocumentByIdUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(id: String, desiredFormat: String = "pdf"): Document? {
        return repository.getDocumentById(id, desiredFormat)
    }
}

class SaveDocumentUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(document: Document) {
        repository.saveDocument(document)
    }
}

class DeleteDocumentUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteDocument(id)
    }
}

class GetDocumentPagesUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(documentId: String, desiredFormat: String = "pdf"): List<Page> {
        return repository.getPagesForDocument(documentId, desiredFormat)
    }
}