package com.prayag.pocketscanner.scanner.mapper

import com.prayag.pocketscanner.scanner.domain.usecase.combined.ProcessResult
import com.prayag.pocketscanner.scanner.presentation.states.externaldocument.BatchProcessResult

    fun ProcessResult.toBatchProcessResult(): BatchProcessResult {
        return BatchProcessResult(
            successful = this.successfulDocuments,
            failed = this.failedUris,
            total = this.totalProcessed,
            // ... map other properties
        )
    }