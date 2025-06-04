package com.prayag.pocketscanner.scanner.presentation.states.cloudstorage

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudSyncUiState(
    val isInitializing: Boolean = false,
    val isSyncing: Boolean = false,
    val isCaching: Boolean = false,
    val isDetectingProvider: Boolean = false,
    val cloudProvider: CloudProvider? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSyncTime: Long? = null,
    val syncProgress: Float = 0f,
    val totalDocuments: Int = 0,
    val syncedDocumentsCount: Int = 0,
    val failedDocumentsCount: Int = 0,
    val error: String? = null,
    val isConnected: Boolean = false,
    val isOnline: Boolean = true,
    val autoSyncEnabled: Boolean = false,
    val conflictDocuments: List<String> = emptyList()
) {
    val hasError: Boolean get() = error != null
    val isProcessing: Boolean get() = isSyncing || isCaching || isDetectingProvider || isInitializing
    val hasConflicts: Boolean get() = conflictDocuments.isNotEmpty()
    val syncProgressPercentage: Int get() = (syncProgress * 100).toInt()
    val canSync: Boolean get() = isConnected && isOnline && !isProcessing && cloudProvider != null
    val lastSyncTimeFormatted: String? get() = lastSyncTime?.let {
        // Format timestamp to readable date/time
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            .format(Date(it))
    }
}

enum class SyncStatus {
    IDLE,
    PREPARING,
    UPLOADING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CONFLICT_DETECTED,
    CANCELLED
}

enum class CloudProvider {
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE,
    ICLOUD
}