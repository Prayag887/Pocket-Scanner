package com.prayag.pocketscanner.scanner.presentation.states.externaldocument

import android.net.Uri
import com.prayag.pocketscanner.scanner.domain.model.Document

data class BatchProcessResult(
    val successful: List<Document>,
    val failed: List<Pair<Uri, String>>, // URI strings or error messages
    val total: Int
) {
    val successCount: Int get() = successful.size
    val failureCount: Int get() = failed.size
    val hasFailures: Boolean get() = failed.isNotEmpty()
    val allSuccessful: Boolean get() = failureCount == 0
}