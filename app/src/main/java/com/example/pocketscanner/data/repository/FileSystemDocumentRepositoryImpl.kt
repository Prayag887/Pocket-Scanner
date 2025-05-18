package com.example.pocketscanner.data.repository

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.pocketscanner.domain.model.Document
import com.example.pocketscanner.domain.model.Page
import com.example.pocketscanner.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.UUID

class FileSystemDocumentRepositoryImpl(
    private val filesDir: File
) : DocumentRepository {

    companion object {
        private const val TAG = "FileSystemDocRepo"
        private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png")
        private val deletedDocumentIds = mutableSetOf<String>()
    }

    //region Repository Implementation
    override suspend fun getAllDocuments(desiredFormat: String): Flow<List<Document>> = flow {
        val documents = loadDocumentsFromFileSystem(desiredFormat)
        emit(documents)
    }

    override suspend fun getDocumentById(id: String, desiredFormat: String): Document? {
        if (isDocumentDeleted(id)) {
            Log.d(TAG, "Document $id was previously deleted, not retrieving")
            return null
        }

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

            if (filesToDelete.isEmpty()) {
                Log.d(TAG, "No files found to delete for document $id")
                return
            }

            var allFilesDeleted = true
            filesToDelete.forEach { file ->
                val deleted = file.delete()
                allFilesDeleted = allFilesDeleted && deleted
                Log.d(TAG, "Deleted file ${file.name}: $deleted")
            }

            if (allFilesDeleted) {
                markDocumentAsDeleted(id)
                Log.d(TAG, "Document $id successfully deleted and marked")
            } else {
                Log.e(TAG, "Failed to delete some files for document $id")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting document $id: ${e.message}", e)
        }
    }


    // Overloaded with desiredFormat param
    override suspend fun getPagesForDocument(documentId: String, desiredFormat: String): List<Page> {
        if (isDocumentDeleted(documentId)) {
            Log.d(TAG, "Document $documentId was previously deleted, not retrieving pages")
            return emptyList()
        }

        return getDocumentPages(documentId, desiredFormat)
    }

    private fun loadDocumentsFromFileSystem(desiredFormat: String): List<Document> {
        val files = filesDir.listFiles()?.filter { it.isFile } ?: emptyList()
        val processedDocuments = mutableListOf<Document>()
        val processedIds = mutableSetOf<String>()

        for (file in files) {
            try {
                if (shouldSkipFile(file)) {
                    continue
                }

                val id = file.nameWithoutExtension

                if (id in processedIds || isDocumentDeleted(id)) {
                    continue
                }

                val document = processFileAsDocument(file, desiredFormat)
                if (document != null) {
                    processedDocuments.add(document)
                    processedIds.add(id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing file ${file.name}: ${e.message}", e)
            }
        }

        return processedDocuments
    }

    private fun processFileAsDocument(file: File, desiredFormat: String): Document? {
        val processedFile = when {
            isPdf(file) && isValidPdf(file) -> file
            isImage(file) -> file
            else -> file
        }


        val pages = if (isPdf(processedFile)) getPdfPages(processedFile)
        else listOf(
            Page(
                id = UUID.randomUUID().toString(),
                imageUri = processedFile?.absolutePath ?: "",
                order = 0
            )
        )
        if (pages.isEmpty()) return null

        return Document(
            id = processedFile.nameWithoutExtension,
            title = processedFile.nameWithoutExtension,
            createdAt = processedFile.lastModified(),
            pages = pages,
            tags = listOf(),
            score = 0,
            format = if (isPdf(processedFile)) "pdf" else processedFile.extension.lowercase()
        )
    }

    private fun findDocumentById(id: String, desiredFormat: String): Document? {
        if (isDocumentDeleted(id)) return null

        val files = filesDir.listFiles()?.filter {
            it.nameWithoutExtension == id && (
                    (desiredFormat == "pdf" && isPdf(it)) ||
                            (desiredFormat != "pdf" && it.extension.equals(desiredFormat, ignoreCase = true))
                    )
        } ?: return null

        return files.firstOrNull()?.let { createDocumentFromFile(it, desiredFormat) }
    }

    private fun createDocumentFromFile(file: File, desiredFormat: String): Document? {
        val pages = when {
            isPdf(file) && desiredFormat == "pdf" -> getPdfPages(file)
            isImage(file) && file.extension.equals(desiredFormat, ignoreCase = true) -> {
                listOf(
                    Page(
                        id = UUID.randomUUID().toString(),
                        imageUri = file.absolutePath,
                        order = 0
                    )
                )
            }
            else -> emptyList()
        }

        if (pages.isEmpty()) return null

        return Document(
            id = file.nameWithoutExtension,
            title = file.nameWithoutExtension,
            createdAt = file.lastModified(),
            pages = pages,
            tags = listOf(),
            score = 0,
            format = file.extension.lowercase()
        )
    }

    private fun getDocumentPages(documentId: String, desiredFormat: String): List<Page> {
        if (isDocumentDeleted(documentId)) return emptyList()

        val file = filesDir.listFiles()?.find {
            it.nameWithoutExtension == documentId && (
                    (desiredFormat == "pdf" && isPdf(it)) ||
                            (desiredFormat != "pdf" && it.extension.equals(desiredFormat, ignoreCase = true))
                    )
        } ?: return emptyList()

        return if (isPdf(file)) getPdfPages(file) else listOf(
            Page(
                id = UUID.randomUUID().toString(),
                imageUri = file.absolutePath,
                order = 0
            )
        )
    }

    private fun isPdf(file: File?): Boolean {
        return file?.extension.equals("pdf", ignoreCase = true)
    }

    private fun isImage(file: File): Boolean {
        return IMAGE_EXTENSIONS.contains(file.extension.lowercase())
    }

    private fun isValidPdf(file: File): Boolean {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).close()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getPdfPages(pdfFile: File?): List<Page> {
        val pages = mutableListOf<Page>()
        try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        pages.add(
                            Page(
                                id = UUID.randomUUID().toString(),
                                imageUri = "${pdfFile?.absolutePath}#page=$i",
                                order = i
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading PDF pages: ${e.message}", e)
        }
        return pages
    }

    private fun shouldSkipFile(file: File): Boolean {
        // Skip temp or irrelevant files
        return file.name.startsWith("temp") || file.name.startsWith(".")
    }

    private fun markDocumentAsDeleted(id: String) {
        deletedDocumentIds.add(id)
    }

    private fun isDocumentDeleted(id: String): Boolean {
        return deletedDocumentIds.contains(id)
    }
}
