//package com.prayag.pocketscanner.data.repository
//
//import com.prayag.pocketscanner.domain.model.Document
//import com.prayag.pocketscanner.domain.model.Page
//import com.prayag.pocketscanner.domain.repository.DocumentRepository
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
//import java.util.UUID
//
//class DocumentRepositoryImpl : DocumentRepository {
//    private val mockDocuments = mutableListOf(
//        Document(
//            id = UUID.randomUUID().toString(),
//            title = "Invoice May 2025",
//            createdAt = System.currentTimeMillis(),
//            pages = listOf(
//                Page(UUID.randomUUID().toString(), "", 0),
//                Page(UUID.randomUUID().toString(), "", 1)
//            ),
//            score = 125,
//            format = "pdf"
//        ),
//        Document(
//            id = UUID.randomUUID().toString(),
//            title = "Contract Draft",
//            createdAt = System.currentTimeMillis() - 86400000,
//            pages = listOf(
//                Page(UUID.randomUUID().toString(), "", 0)
//            ),
//            score = 75,
//            format = "pdf"
//        ),
//        Document(
//            id = UUID.randomUUID().toString(),
//            title = "Receipt #12345",
//            createdAt = System.currentTimeMillis() - 172800000,
//            pages = listOf(
//                Page(UUID.randomUUID().toString(), "", 0)
//            ),
//            score = 50,
//            format = "png"
//        )
//    )
//
////    override fun getAllDocuments(): Flow<List<Document>> = flow {
////        emit(mockDocuments)
////    }
//
//    override suspend fun getAllDocuments(): Flow<List<Document>> = flow {
//        emit(mockDocuments)
//    }
//
//    override suspend fun getDocumentById(id: String): Document? {
//        return mockDocuments.find { it.id == id }
//    }
//
//    override suspend fun saveDocument(document: Document) {
//        val existingIndex = mockDocuments.indexOfFirst { it.id == document.id }
//        if (existingIndex >= 0) {
//            mockDocuments[existingIndex] = document
//        } else {
//            mockDocuments.add(document)
//        }
//    }
//
//    override suspend fun deleteDocument(id: String) {
//        mockDocuments.removeIf { it.id == id }
//    }
//
//    override suspend fun getPagesForDocument(documentId: String): List<Page> {
//        TODO("Not yet implemented")
//    }
//}