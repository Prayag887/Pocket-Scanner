package com.prayag.pocketscanner.scanner.presentation.viewmodels.cloudstorage

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayag.pocketscanner.scanner.domain.usecase.cloudstorage.CachePdfLocallyUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.cloudstorage.DetectCloudProviderUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.cloudstorage.SyncWithCloudStorageUseCase
import com.prayag.pocketscanner.scanner.presentation.states.cloudstorage.CloudProvider
import com.prayag.pocketscanner.scanner.presentation.states.cloudstorage.CloudSyncUiState
import com.prayag.pocketscanner.scanner.presentation.states.cloudstorage.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CloudSyncViewModel(
    private val syncWithCloudStorageUseCase: SyncWithCloudStorageUseCase,
    private val cachePdfLocallyUseCase: CachePdfLocallyUseCase,
    private val detectCloudProviderUseCase: DetectCloudProviderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudSyncUiState())
    val uiState: StateFlow<CloudSyncUiState> = _uiState.asStateFlow()

    private val _syncedDocuments = MutableStateFlow<Set<String>>(emptySet())
    val syncedDocuments: StateFlow<Set<String>> = _syncedDocuments.asStateFlow()

    fun syncDocumentWithCloud(documentId: String, cloudProvider: CloudProvider) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncStatus = SyncStatus.UPLOADING,
                    error = null
                )
            }

            syncWithCloudStorageUseCase(documentId, cloudProvider).fold(
                onSuccess = { success ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncStatus = if (success) SyncStatus.COMPLETED else SyncStatus.FAILED,
                            lastSyncTime = System.currentTimeMillis()
                        )
                    }

                    if (success) {
                        _syncedDocuments.update { it + documentId }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncStatus = SyncStatus.FAILED,
                            error = error.message,
                            lastSyncTime = System.currentTimeMillis()
                        )
                    }
                }
            )
        }
    }

    fun cachePdfLocally(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isCaching = true,
                    error = null
                )
            }

            cachePdfLocallyUseCase(uri).fold(
                onSuccess = { cachePath ->
                    _uiState.update {
                        it.copy(
                            isCaching = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCaching = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun detectCloudProvider(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isDetectingProvider = true,
                    error = null
                )
            }

            try {
                val provider = detectCloudProviderUseCase(uri)
                _uiState.update {
                    it.copy(
                        isDetectingProvider = false,
                        cloudProvider = provider
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDetectingProvider = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun batchSyncDocuments(documentIds: List<String>, cloudProvider: CloudProvider) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncStatus = SyncStatus.PREPARING,
                    syncProgress = 0f,
                    totalDocuments = documentIds.size,
                    syncedDocumentsCount = 0,
                    failedDocumentsCount = 0,
                    error = null
                )
            }

            var completed = 0
            val total = documentIds.size
            val successful = mutableListOf<String>()
            val failed = mutableListOf<String>()

            _uiState.update { it.copy(syncStatus = SyncStatus.UPLOADING) }

            documentIds.forEach { documentId ->
                syncWithCloudStorageUseCase(documentId, cloudProvider).fold(
                    onSuccess = { success ->
                        if (success) successful.add(documentId) else failed.add(documentId)
                    },
                    onFailure = { failed.add(documentId) }
                )

                completed++
                _uiState.update {
                    it.copy(
                        syncProgress = completed.toFloat() / total.toFloat(),
                        syncedDocumentsCount = successful.size,
                        failedDocumentsCount = failed.size
                    )
                }
            }

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncStatus = if (failed.isEmpty()) SyncStatus.COMPLETED else SyncStatus.FAILED,
                    syncProgress = 1f,
                    lastSyncTime = System.currentTimeMillis()
                )
            }

            _syncedDocuments.update { current -> current + successful.toSet() }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSyncState() {
        _uiState.update {
            it.copy(
                syncStatus = SyncStatus.IDLE,
                syncProgress = 0f,
                totalDocuments = 0,
                syncedDocumentsCount = 0,
                failedDocumentsCount = 0
            )
        }
    }

    fun clearSyncState() {
        _uiState.update { CloudSyncUiState() }
        _syncedDocuments.update { emptySet() }
    }
}