package com.prayag.pocketscanner.scanner.presentation.viewmodels.externaldocument

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayag.pocketscanner.scanner.domain.usecase.combined.ImportPdfUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.combined.ImportType
import com.prayag.pocketscanner.scanner.domain.usecase.combined.ProcessMultiplePdfsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ExtractPagesFromPdfUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.GetPdfMetadataUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ImportPdfFromCloudStorageUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ImportPdfFromDeviceUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.OpenExternalPdfUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ValidatePdfAccessUseCase
import com.prayag.pocketscanner.scanner.mapper.toBatchProcessResult
import com.prayag.pocketscanner.scanner.presentation.states.cloudstorage.CloudProvider
import com.prayag.pocketscanner.scanner.presentation.states.externaldocument.ExternalPdfUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExternalPdfViewModel(
    private val openExternalPdfUseCase: OpenExternalPdfUseCase,
    private val importPdfFromDeviceUseCase: ImportPdfFromDeviceUseCase,
    private val importPdfFromCloudStorageUseCase: ImportPdfFromCloudStorageUseCase,
    private val validatePdfAccessUseCase: ValidatePdfAccessUseCase,
    private val getPdfMetadataUseCase: GetPdfMetadataUseCase,
    private val extractPagesFromPdfUseCase: ExtractPagesFromPdfUseCase,
    private val importPdfUseCase: ImportPdfUseCase,
    private val processMultiplePdfsUseCase: ProcessMultiplePdfsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExternalPdfUiState())
    val uiState: StateFlow<ExternalPdfUiState> = _uiState.asStateFlow()

    fun openExternalPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }

            openExternalPdfUseCase(uri).fold(
                onSuccess = { document ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentDocument = document,
                            isDocumentReady = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message,
                            isDocumentReady = false
                        )
                    }
                }
            )
        }
    }

    fun importPdfFromDevice(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isImporting = true, error = null) }

            importPdfFromDeviceUseCase(uri).fold(
                onSuccess = { document ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importedDocument = document,
                            importSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            error = error.message,
                            importSuccess = false
                        )
                    }
                }
            )
        }
    }

    fun importPdfFromCloudStorage(uri: Uri, cloudProvider: CloudProvider) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isImporting = true, error = null) }

            importPdfFromCloudStorageUseCase(uri, cloudProvider).fold(
                onSuccess = { document ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importedDocument = document,
                            importSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            error = error.message,
                            importSuccess = false
                        )
                    }
                }
            )
        }
    }

    fun smartImportPdf(uri: Uri, importType: ImportType = ImportType.AUTO) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isImporting = true, error = null) }

            importPdfUseCase(uri, importType).fold(
                onSuccess = { document ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importedDocument = document,
                            importSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            error = error.message,
                            importSuccess = false
                        )
                    }
                }
            )
        }
    }

    fun processMultiplePdfs(uris: List<Uri>, importType: ImportType = ImportType.AUTO) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBatchProcessing = true, error = null) }

            processMultiplePdfsUseCase(uris, importType).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isBatchProcessing = false,
                            batchProcessResult = result.toBatchProcessResult(),
                            batchProcessSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isBatchProcessing = false,
                            error = error.message,
                            batchProcessSuccess = false
                        )
                    }
                }
            )
        }
    }

    fun validatePdfAccess(uri: Uri, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val isValid = validatePdfAccessUseCase(uri)
            callback(isValid)
        }
    }

    fun getPdfMetadata(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingMetadata = true) }

            getPdfMetadataUseCase(uri).fold(
                onSuccess = { metadata ->
                    _uiState.update {
                        it.copy(
                            isLoadingMetadata = false,
                            pdfMetadata = metadata
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMetadata = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun extractPagesFromPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExtractingPages = true) }

            extractPagesFromPdfUseCase(uri).fold(
                onSuccess = { pages ->
                    _uiState.update {
                        it.copy(
                            isExtractingPages = false,
                            extractedPages = pages
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isExtractingPages = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun clearImportState() {
        _uiState.update {
            it.copy(
                importedDocument = null,
                importSuccess = false,
                batchProcessResult = null,
                batchProcessSuccess = false,
                error = null
            )
        }
    }
}