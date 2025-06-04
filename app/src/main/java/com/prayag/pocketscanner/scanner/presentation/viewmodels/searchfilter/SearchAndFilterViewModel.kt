package com.prayag.pocketscanner.scanner.presentation.viewmodels.searchfilter


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.FilterDocumentsByDateRangeUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.FilterDocumentsByFormatUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.FilterDocumentsByTagUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.GetDocumentStatisticsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.SearchDocumentsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.SortDocumentsUseCase
import com.prayag.pocketscanner.scanner.presentation.states.searchfilter.SearchUiState
import com.prayag.pocketscanner.scanner.presentation.states.searchfilter.SortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val filterDocumentsByTagUseCase: FilterDocumentsByTagUseCase,
    private val filterDocumentsByFormatUseCase: FilterDocumentsByFormatUseCase,
    private val filterDocumentsByDateRangeUseCase: FilterDocumentsByDateRangeUseCase,
    private val sortDocumentsUseCase: SortDocumentsUseCase,
    private val getDocumentStatisticsUseCase: GetDocumentStatisticsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilters = MutableStateFlow(SearchFilters())
    val selectedFilters: StateFlow<SearchFilters> = _selectedFilters.asStateFlow()

    private var searchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        performSearch()
    }

    fun updateFilters(filters: SearchFilters) {
        _selectedFilters.value = filters
        performSearch()
    }

    fun sortDocuments(sortBy: SortBy, ascending: Boolean = true) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            try {
                sortDocumentsUseCase(sortBy, ascending, _selectedFilters.value.format).collect { documents ->
                    val filteredDocuments = applyFilters(documents)
                    _uiState.update {
                        it.copy(
                            searchResults = filteredDocuments,
                            isLoading = false,
                            currentSortBy = sortBy,
                            isAscending = ascending
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun filterByTag(tag: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            try {
                filterDocumentsByTagUseCase(tag, _selectedFilters.value.format).collect { documents ->
                    _uiState.update {
                        it.copy(
                            searchResults = documents,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun filterByFormat(format: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            try {
                filterDocumentsByFormatUseCase(format).collect { documents ->
                    val filteredDocuments = applyAdditionalFilters(documents)
                    _uiState.update {
                        it.copy(
                            searchResults = filteredDocuments,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun filterByDateRange(startDate: Long, endDate: Long) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            try {
                filterDocumentsByDateRangeUseCase(startDate, endDate, _selectedFilters.value.format)
                    .collect { documents ->
                        val filteredDocuments = applyAdditionalFilters(documents)
                        _uiState.update {
                            it.copy(
                                searchResults = filteredDocuments,
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun loadDocumentStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getDocumentStatisticsUseCase(_selectedFilters.value.format).collect { stats ->
                    _uiState.update {
                        it.copy(documentStatistics = stats)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message)
                }
            }
        }
    }

    private fun performSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                searchDocumentsUseCase(_searchQuery.value, _selectedFilters.value.format)
                    .collect { documents ->
                        val filteredDocuments = applyFilters(documents)
                        _uiState.update {
                            it.copy(
                                searchResults = filteredDocuments,
                                isLoading = false,
                                hasSearched = _searchQuery.value.isNotEmpty() || _selectedFilters.value.hasActiveFilters()
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun applyFilters(documents: List<Document>): List<Document> {
        var filtered = documents
        val filters = _selectedFilters.value

        // Apply tag filter
        if (filters.selectedTags.isNotEmpty()) {
            filtered = filtered.filter { document ->
                filters.selectedTags.any { tag -> document.tags.contains(tag) }
            }
        }

        // Apply date range filter
        if (filters.startDate != null && filters.endDate != null) {
            filtered = filtered.filter { document ->
                document.createdAt in filters.startDate..filters.endDate
            }
        }

        return filtered
    }

    private fun applyAdditionalFilters(documents: List<Document>): List<Document> {
        var filtered = documents
        val filters = _selectedFilters.value

        // Apply search query if present
        if (_searchQuery.value.isNotEmpty()) {
            filtered = filtered.filter { document ->
                document.title.contains(_searchQuery.value, ignoreCase = true) ||
                        document.tags.any { tag -> tag.contains(_searchQuery.value, ignoreCase = true) }
            }
        }

        // Apply tag filter
        if (filters.selectedTags.isNotEmpty()) {
            filtered = filtered.filter { document ->
                filters.selectedTags.any { tag -> document.tags.contains(tag) }
            }
        }

        return filtered
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _selectedFilters.value = SearchFilters()
        _uiState.update {
            it.copy(
                searchResults = emptyList(),
                hasSearched = false,
                error = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}

data class SearchFilters(
    val format: String = "pdf",
    val selectedTags: Set<String> = emptySet(),
    val startDate: Long? = null,
    val endDate: Long? = null
) {
    fun hasActiveFilters(): Boolean {
        return selectedTags.isNotEmpty() || startDate != null || endDate != null
    }
}