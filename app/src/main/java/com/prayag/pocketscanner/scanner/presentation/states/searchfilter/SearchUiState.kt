package com.prayag.pocketscanner.scanner.presentation.states.searchfilter

import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.DocumentStatistics

data class SearchUiState(
    val searchResults: List<Document> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false,
    val currentSortBy: SortBy? = null,
    val isAscending: Boolean = true,
    val documentStatistics: DocumentStatistics? = null
)