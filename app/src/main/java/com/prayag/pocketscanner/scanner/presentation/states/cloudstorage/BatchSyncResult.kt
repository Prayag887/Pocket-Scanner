package com.prayag.pocketscanner.scanner.presentation.states.cloudstorage

data class BatchSyncResult(
    val successful: List<String>,
    val failed: List<String>,
    val total: Int
) {
    val successCount: Int get() = successful.size
    val failureCount: Int get() = failed.size
    val hasFailures: Boolean get() = failed.isNotEmpty()
}