package com.prayag.pocketscanner.scanner.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.usecase.document.*
import com.prayag.pocketscanner.scanner.presentation.states.DocumentsUiState
import com.prayag.pocketscanner.scanner.presentation.states.NavigationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentViewModel(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase,
    private val getDocumentByIdUseCase: GetDocumentByIdUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val getDocumentPagesUseCase: GetDocumentPagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    private val _currentFormat = MutableStateFlow("pdf")
    val currentFormat: StateFlow<String> = _currentFormat.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private var loadDocumentsJob: Job? = null

    init {
        loadDocuments(currentFormat.value)
    }

    fun setCurrentFormat(format: String) {
        if (_currentFormat.value != format) {
            _currentFormat.value = format
            loadDocuments(format)
        }
    }

    fun refreshDocuments() {
        loadDocuments(_currentFormat.value)
    }

    fun deleteDocument(documentId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true) }

                deleteDocumentUseCase(documentId)

                _uiState.update { state ->
                    state.copy(
                        documents = state.documents.filterNot { it.id == documentId },
                        isLoading = false
                    )
                }

                withContext(Dispatchers.Main) { onResult(true) }
                refreshDocuments()

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun saveDocument(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true) }
                saveDocumentUseCase(document)
                refreshDocuments()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadDocumentDetails(documentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val document = getDocumentByIdUseCase(documentId, _currentFormat.value)
                val pages = getDocumentPagesUseCase(documentId, _currentFormat.value)

                _uiState.update {
                    it.copy(
                        currentDocument = document?.copy(pages = pages),
                        isLoading = false,
                        isDocumentReady = document != null
                    )
                }

                if (document != null) {
                    _navigationEvent.send(NavigationEvent.DocumentLoaded(documentId))
                } else {
                    _navigationEvent.send(NavigationEvent.DocumentLoadFailed("Document not found"))
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load document: ${e.message}",
                        isDocumentReady = false
                    )
                }
                _navigationEvent.send(NavigationEvent.DocumentLoadFailed(e.message ?: "Unknown error"))
            }
        }
    }

    private fun loadDocuments(desiredFormat: String) {
        loadDocumentsJob?.cancel()
        loadDocumentsJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                getAllDocumentsUseCase(desiredFormat).collect { documents ->
                    _uiState.update {
                        it.copy(
                            documents = documents,
                            isLoading = false,
                            isDataReady = true
                        )
                    }
                    _navigationEvent.send(NavigationEvent.DataLoaded)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load documents: ${e.message}",
                        isDataReady = false
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadDocumentsJob?.cancel()
    }
}