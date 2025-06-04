package com.prayag.pocketscanner.scanner.domain.usecase.searchfilter

import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.usecase.document.GetAllDocumentsUseCase
import com.prayag.pocketscanner.scanner.presentation.states.searchfilter.SortBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchDocumentsUseCase(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase
) {
    suspend operator fun invoke(
        query: String,
        format: String = "pdf"
    ): Flow<List<Document>> {
        return getAllDocumentsUseCase(format).map { documents ->
            if (query.isBlank()) {
                documents
            } else {
                documents.filter { document ->
                    document.title.contains(query, ignoreCase = true) ||
                            document.tags.any { tag -> tag.contains(query, ignoreCase = true) }
                }
            }
        }
    }
}

class FilterDocumentsByTagUseCase(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase
) {
    suspend operator fun invoke(
        tag: String,
        format: String = "pdf"
    ): Flow<List<Document>> {
        return getAllDocumentsUseCase(format).map { documents ->
            documents.filter { document ->
                document.tags.contains(tag)
            }
        }
    }
}

class FilterDocumentsByFormatUseCase(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase
) {
    suspend operator fun invoke(format: String): Flow<List<Document>> {
        return getAllDocumentsUseCase(format).map { documents ->
            documents.filter { document ->
                document.format.equals(format, ignoreCase = true)
            }
        }
    }
}

class FilterDocumentsByDateRangeUseCase(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase
) {
    suspend operator fun invoke(
        startDate: Long,
        endDate: Long,
        format: String = "pdf"
    ): Flow<List<Document>> {
        return getAllDocumentsUseCase(format).map { documents ->
            documents.filter { document ->
                document.createdAt in startDate..endDate
            }
        }
    }
}

class SortDocumentsUseCase(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase
) {
    suspend operator fun invoke(
        sortBy: SortBy,
        ascending: Boolean = true,
        format: String = "pdf"
    ): Flow<List<Document>> {
        return getAllDocumentsUseCase(format).map { documents ->
            val sorted = when (sortBy) {
                SortBy.TITLE -> documents.sortedBy { it.title }
                SortBy.CREATED_DATE -> documents.sortedBy { it.createdAt }
                SortBy.SCORE -> documents.sortedBy { it.score }
                SortBy.PAGE_COUNT -> documents.sortedBy { it.pages.size }
                SortBy.FORMAT -> documents.sortedBy { it.format }
            }
            if (ascending) sorted else sorted.reversed()
        }
    }
}

class GetDocumentStatisticsUseCase(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase
) {
    suspend operator fun invoke(format: String = "pdf"): Flow<DocumentStatistics> {
        return getAllDocumentsUseCase(format).map { documents ->
            DocumentStatistics(
                totalDocuments = documents.size,
                totalPages = documents.sumOf { it.pages.size },
                formatCounts = documents.groupBy { it.format }.mapValues { it.value.size },
                tagCounts = documents.flatMap { it.tags }.groupBy { it }.mapValues { it.value.size },
                averageScore = if (documents.isNotEmpty()) documents.map { it.score }.average() else 0.0
            )
        }
    }
}

data class DocumentStatistics(
    val totalDocuments: Int,
    val totalPages: Int,
    val formatCounts: Map<String, Int>,
    val tagCounts: Map<String, Int>,
    val averageScore: Double
)