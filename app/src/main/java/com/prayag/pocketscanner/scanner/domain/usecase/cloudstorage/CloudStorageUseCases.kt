package com.prayag.pocketscanner.scanner.domain.usecase.cloudstorage

import android.net.Uri
import com.prayag.pocketscanner.scanner.domain.repository.DocumentRepository
import com.prayag.pocketscanner.scanner.presentation.states.cloudstorage.CloudProvider
import com.prayag.pocketscanner.scanner.utils.PdfUtils

class SyncWithCloudStorageUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(documentId: String, cloudProvider: CloudProvider): Result<Boolean> {
        return try {
            val success = repository.syncWithCloudStorage(documentId, cloudProvider)
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CachePdfLocallyUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(uri: Uri): Result<String> {
        return try {
            val cachePath = repository.cachePdfLocally(uri)
            if (cachePath != null) {
                Result.success(cachePath)
            } else {
                Result.failure(Exception("Failed to cache PDF locally"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DetectCloudProviderUseCase(
    private val pdfUtils: PdfUtils
) {
    operator fun invoke(uri: Uri): CloudProvider? {
        return pdfUtils.detectCloudProvider(uri)
    }
}