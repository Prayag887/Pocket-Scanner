package com.prayag.pocketscanner.scanner.domain.usecase.externaldocument

import android.net.Uri
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.domain.repository.DocumentRepository
import com.prayag.pocketscanner.scanner.domain.repository.PdfMetadata
import com.prayag.pocketscanner.scanner.presentation.states.cloudstorage.CloudProvider

class OpenExternalPdfUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(uri: Uri): Result<Document> {
        return try {
            val document = repository.openExternalPdf(uri)
            if (document != null) {
                Result.success(document)
            } else {
                Result.failure(Exception("Failed to open PDF or invalid PDF format"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ImportPdfFromDeviceUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(uri: Uri): Result<Document> {
        return try {
            val document = repository.importPdfFromDevice(uri)
            if (document != null) {
                Result.success(document)
            } else {
                Result.failure(Exception("Failed to import PDF from device"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ImportPdfFromCloudStorageUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(uri: Uri, cloudProvider: CloudProvider): Result<Document> {
        return try {
            val document = repository.importPdfFromCloudStorage(uri, cloudProvider)
            if (document != null) {
                Result.success(document)
            } else {
                Result.failure(Exception("Failed to import PDF from ${cloudProvider.name}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ValidatePdfAccessUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(uri: Uri): Boolean {
        return repository.validatePdfAccess(uri)
    }
}

class GetPdfMetadataUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(uri: Uri): Result<PdfMetadata> {
        return try {
            val metadata = repository.getPdfMetadata(uri)
            if (metadata != null) {
                Result.success(metadata)
            } else {
                Result.failure(Exception("Failed to extract PDF metadata"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ExtractPagesFromPdfUseCase(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(uri: Uri): Result<List<Page>> {
        return try {
            val pages = repository.extractPagesFromPdf(uri)
            Result.success(pages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}