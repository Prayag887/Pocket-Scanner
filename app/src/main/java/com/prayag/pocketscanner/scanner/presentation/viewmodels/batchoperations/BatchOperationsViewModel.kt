package com.prayag.pocketscanner.scanner.presentation.viewmodels.batchoperations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.usecase.batchoperations.BatchDeleteDocumentsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.batchoperations.BatchSaveDocumentsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.batchoperations.BatchResult
import com.prayag.pocketscanner.scanner.presentation.states.batchoperations.BatchOperationType
import com.prayag.pocketscanner.scanner.presentation.states.batchoperations.BatchOperationsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BatchOperationsViewModel(
    private val batchDeleteDocumentsUseCase: BatchDeleteDocumentsUseCase,
    private val batchSaveDocumentsUseCase: BatchSaveDocumentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchOperationsUiState())
    val uiState: StateFlow<BatchOperationsUiState> = _uiState.asStateFlow()

    private val _selectedDocuments = MutableStateFlow<Set<String>>(emptySet())
    val selectedDocuments: StateFlow<Set<String>> = _selectedDocuments.asStateFlow()

    fun selectDocument(documentId: String) {
        _selectedDocuments.update { it + documentId }
    }

    fun deselectDocument(documentId: String) {
        _selectedDocuments.update { it - documentId }
    }

    fun selectAllDocuments(documentIds: List<String>) {
        _selectedDocuments.value = documentIds.toSet()
    }

    fun clearSelection() {
        _selectedDocuments.value = emptySet()
    }

    fun toggleDocumentSelection(documentId: String) {
        _selectedDocuments.update {
            if (it.contains(documentId)) it - documentId else it + documentId
        }
    }

    fun batchDeleteDocuments() {
        val documentsToDelete = _selectedDocuments.value.toList()
        if (documentsToDelete.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    operationType = BatchOperationType.DELETE,
                    error = null
                )
            }

            batchDeleteDocumentsUseCase(documentsToDelete).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastResult = result,
                            operationSuccess = true
                        )
                    }
                    clearSelection()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message,
                            operationSuccess = false
                        )
                    }
                }
            )
        }
    }

    fun batchSaveDocuments(documents: List<Document>) {
        if (documents.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    operationType = BatchOperationType.SAVE,
                    error = null
                )
            }

            batchSaveDocumentsUseCase(documents).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastResult = result,
                            operationSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message,
                            operationSuccess = false
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetOperationResult() {
        _uiState.update {
            it.copy(
                lastResult = null,
                operationSuccess = null,
                operationType = null
            )
        }
    }

    val isSelectionEmpty: Boolean
        get() = _selectedDocuments.value.isEmpty()

    val selectedCount: Int
        get() = _selectedDocuments.value.size
}