package com.prayag.pocketscanner.scanner.presentation.states.batchoperations

import com.prayag.pocketscanner.scanner.domain.usecase.batchoperations.BatchResult

data class BatchOperationsUiState(
    val isProcessing: Boolean = false,
    val operationType: BatchOperationType? = null,
    val lastResult: BatchResult? = null,
    val operationSuccess: Boolean? = null,
    val error: String? = null
) {
    val hasError: Boolean get() = error != null
    val showResult: Boolean get() = lastResult != null && operationSuccess == true
}