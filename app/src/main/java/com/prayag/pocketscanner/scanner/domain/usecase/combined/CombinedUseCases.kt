package com.prayag.pocketscanner.scanner.domain.usecase.combined

import android.net.Uri
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.usecase.cloudstorage.DetectCloudProviderUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ImportPdfFromCloudStorageUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ImportPdfFromDeviceUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.OpenExternalPdfUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ValidatePdfAccessUseCase

class ImportPdfUseCase(
    private val validatePdfAccessUseCase: ValidatePdfAccessUseCase,
    private val detectCloudProviderUseCase: DetectCloudProviderUseCase,
    private val importPdfFromDeviceUseCase: ImportPdfFromDeviceUseCase,
    private val importPdfFromCloudStorageUseCase: ImportPdfFromCloudStorageUseCase,
    private val openExternalPdfUseCase: OpenExternalPdfUseCase
) {
    suspend operator fun invoke(uri: Uri, importType: ImportType = ImportType.AUTO): Result<Document> {
        // Validate PDF access first
        if (!validatePdfAccessUseCase(uri)) {
            return Result.failure(Exception("Cannot access PDF or invalid PDF format"))
        }

        return when (importType) {
            ImportType.AUTO -> {
                val cloudProvider = detectCloudProviderUseCase(uri)
                if (cloudProvider != null) {
                    importPdfFromCloudStorageUseCase(uri, cloudProvider)
                } else {
                    importPdfFromDeviceUseCase(uri)
                }
            }
            ImportType.DEVICE_ONLY -> importPdfFromDeviceUseCase(uri)
            ImportType.CLOUD_ONLY -> {
                val cloudProvider = detectCloudProviderUseCase(uri)
                    ?: return Result.failure(Exception("Could not detect cloud provider"))
                importPdfFromCloudStorageUseCase(uri, cloudProvider)
            }
            ImportType.VIEW_ONLY -> openExternalPdfUseCase(uri)
        }
    }
}

enum class ImportType {
    AUTO,
    DEVICE_ONLY,
    CLOUD_ONLY,
    VIEW_ONLY
}

class ProcessMultiplePdfsUseCase(
    private val importPdfUseCase: ImportPdfUseCase
) {
    suspend operator fun invoke(uris: List<Uri>, importType: ImportType = ImportType.AUTO): Result<ProcessResult> {
        val successfulDocuments = mutableListOf<Document>()
        val failedUris = mutableListOf<Pair<Uri, String>>()

        uris.forEach { uri ->
            importPdfUseCase(uri, importType).fold(
                onSuccess = { document -> successfulDocuments.add(document) },
                onFailure = { error -> failedUris.add(uri to (error.message ?: "Unknown error")) }
            )
        }

        return Result.success(
            ProcessResult(
                successfulDocuments = successfulDocuments,
                failedUris = failedUris,
                totalProcessed = uris.size
            )
        )
    }
}

data class ProcessResult(
    val successfulDocuments: List<Document>,
    val failedUris: List<Pair<Uri, String>>,
    val totalProcessed: Int
) {
    val successCount: Int get() = successfulDocuments.size
    val failureCount: Int get() = failedUris.size
    val hasFailures: Boolean get() = failedUris.isNotEmpty()
}