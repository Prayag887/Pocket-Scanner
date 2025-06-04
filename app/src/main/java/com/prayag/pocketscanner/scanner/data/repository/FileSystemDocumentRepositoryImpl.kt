package com.prayag.pocketscanner.scanner.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.domain.repository.DocumentRepository
import com.prayag.pocketscanner.scanner.domain.repository.PdfMetadata
import com.prayag.pocketscanner.scanner.presentation.states.cloudstorage.CloudProvider
import com.prayag.pocketscanner.scanner.utils.FileUtils
import com.prayag.pocketscanner.scanner.utils.PdfUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.UUID

class FileSystemDocumentRepositoryImpl(
    private val context: Context,
    private val filesDir: File
) : DocumentRepository {

    companion object {
        private const val TAG = "FileSystemDocRepo"
        private val deletedDocumentIds = mutableSetOf<String>()
    }

    private val fileUtils = FileUtils()
    private val pdfUtils = PdfUtils(context)

    //region Original Repository Implementation
    override suspend fun getAllDocuments(desiredFormat: String): Flow<List<Document>> = flow {
        val documents = loadDocumentsFromFileSystem(desiredFormat)
        emit(documents)
    }

    override suspend fun getDocumentById(id: String, desiredFormat: String): Document? {
        if (isDocumentDeleted(id)) return null
        return findDocumentById(id, desiredFormat)
    }

    override suspend fun saveDocument(document: Document) {
        // Implementation if needed
    }

    override suspend fun deleteDocument(id: String) {
        try {
            val filesToDelete = filesDir.listFiles()?.filter {
                it.nameWithoutExtension == id
            } ?: emptyList()

            if (filesToDelete.isEmpty()) return

            val allFilesDeleted = filesToDelete.all { it.delete() }

            if (allFilesDeleted) {
                markDocumentAsDeleted(id)
            } else {
                Log.e(TAG, "Failed to delete some files for document $id")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting document $id: ${e.message}", e)
        }
    }

    override suspend fun getPagesForDocument(documentId: String, desiredFormat: String): List<Page> {
        if (isDocumentDeleted(documentId)) return emptyList()
        return getDocumentPages(documentId, desiredFormat)
    }

    //region External PDF Functions
    override suspend fun openExternalPdf(uri: Uri): Document? {
        return try {
            if (!validatePdfAccess(uri)) return null

            val pages = extractPagesFromPdf(uri)
            if (pages.isEmpty()) return null

            val metadata = getPdfMetadata(uri)
            val documentId = UUID.randomUUID().toString()

            Document(
                id = documentId,
                title = metadata?.title ?: "External PDF",
                createdAt = System.currentTimeMillis(),
                pages = pages,
                tags = listOf("external"),
                score = 0,
                format = "pdf",
                thumbnail = "$uri#page=0".toUri()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening external PDF: ${e.message}", e)
            null
        }
    }

    override suspend fun importPdfFromDevice(uri: Uri): Document? {
        return try {
            if (!validatePdfAccess(uri)) return null

            val fileName = "${UUID.randomUUID()}.pdf"
            val cachedPath = pdfUtils.cachePdfLocally(uri, fileName)
                ?: return null

            val cachedFile = File(cachedPath)
            val pages = pdfUtils.extractPagesFromPdf(cachedFile)

            fileUtils.createDocumentFromFile(cachedFile, pages)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing PDF from device: ${e.message}", e)
            null
        }
    }

    override suspend fun importPdfFromCloudStorage(uri: Uri, cloudProvider: CloudProvider): Document? {
        return try {
            if (!validatePdfAccess(uri)) return null

            val fileName = "${UUID.randomUUID()}_${cloudProvider.name.lowercase()}.pdf"
            val cachedPath = pdfUtils.cachePdfLocally(uri, fileName)
                ?: return null

            val cachedFile = File(cachedPath)
            val pages = pdfUtils.extractPagesFromPdf(cachedFile)
            val document = fileUtils.createDocumentFromFile(cachedFile, pages)

            // Add cloud provider tag
            document?.copy(tags = document.tags + cloudProvider.name.lowercase())
        } catch (e: Exception) {
            Log.e(TAG, "Error importing PDF from cloud: ${e.message}", e)
            null
        }
    }

    override suspend fun validatePdfAccess(uri: Uri): Boolean {
        return pdfUtils.isValidPdf(uri)
    }

    override suspend fun extractPagesFromPdf(uri: Uri): List<Page> {
        return pdfUtils.extractPagesFromPdf(uri)
    }

    override suspend fun getPdfMetadata(uri: Uri): PdfMetadata? {
        return pdfUtils.getPdfMetadata(uri)
    }

    override suspend fun cachePdfLocally(uri: Uri): String? {
        val fileName = "${UUID.randomUUID()}.pdf"
        return pdfUtils.cachePdfLocally(uri, fileName)
    }

    override suspend fun syncWithCloudStorage(documentId: String, cloudProvider: CloudProvider): Boolean {
        return try {
            Log.d(TAG, "Syncing document $documentId with ${cloudProvider.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with cloud storage: ${e.message}", e)
            false
        }
    }

    //region Private Helper Methods
    private fun loadDocumentsFromFileSystem(desiredFormat: String): List<Document> {
        val files = filesDir.listFiles()?.filter {
            it.isFile && fileUtils.isSupportedFile(it)
        } ?: emptyList()

        val documents = mutableListOf<Document>()
        val processedIds = mutableSetOf<String>()

        for (file in files) {
            try {
                if (fileUtils.shouldSkipFile(file)) continue

                val id = file.nameWithoutExtension
                if (id in processedIds || isDocumentDeleted(id)) continue

                val document = processFileAsDocument(file, desiredFormat)
                if (document != null) {
                    documents.add(document)
                    processedIds.add(id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing file ${file.name}: ${e.message}", e)
            }
        }

        return documents
    }

    private fun processFileAsDocument(file: File, desiredFormat: String): Document? {
        val pages = when {
            fileUtils.isPdf(file) && pdfUtils.isValidPdf(file) ->
                pdfUtils.extractPagesFromPdf(file)
            fileUtils.isImage(file) ->
                listOf(fileUtils.createImagePage(file))
            else -> emptyList()
        }

        return fileUtils.createDocumentFromFile(file, pages)
    }

    private fun findDocumentById(id: String, desiredFormat: String): Document? {
        val files = filesDir.listFiles()?.filter {
            it.nameWithoutExtension == id && matchesDesiredFormat(it, desiredFormat)
        } ?: return null

        return files.firstOrNull()?.let { createDocumentFromFile(it, desiredFormat) }
    }

    private fun createDocumentFromFile(file: File, desiredFormat: String): Document? {
        val pages = when {
            fileUtils.isPdf(file) && desiredFormat == "pdf" ->
                pdfUtils.extractPagesFromPdf(file)
            fileUtils.isImage(file) && file.extension.equals(desiredFormat, ignoreCase = true) ->
                listOf(fileUtils.createImagePage(file))
            else -> emptyList()
        }

        return fileUtils.createDocumentFromFile(file, pages)
    }

    private fun getDocumentPages(documentId: String, desiredFormat: String): List<Page> {
        val file = filesDir.listFiles()?.find {
            it.nameWithoutExtension == documentId && matchesDesiredFormat(it, desiredFormat)
        } ?: return emptyList()

        return when {
            fileUtils.isPdf(file) -> pdfUtils.extractPagesFromPdf(file)
            fileUtils.isImage(file) -> listOf(fileUtils.createImagePage(file))
            else -> emptyList()
        }
    }

    private fun matchesDesiredFormat(file: File, desiredFormat: String): Boolean {
        return when (desiredFormat) {
            "pdf" -> fileUtils.isPdf(file)
            else -> file.extension.equals(desiredFormat, ignoreCase = true)
        }
    }

    private fun markDocumentAsDeleted(id: String) {
        deletedDocumentIds.add(id)
    }

    private fun isDocumentDeleted(id: String): Boolean {
        return deletedDocumentIds.contains(id)
    }
}