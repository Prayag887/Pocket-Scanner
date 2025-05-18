package com.example.pocketscanner.presentation.viewmodels

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketscanner.domain.model.Document
import com.example.pocketscanner.domain.model.Page
import com.example.pocketscanner.domain.usecase.GetDocumentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import androidx.core.net.toUri

class DocumentViewModel(
    private val getDocumentsUseCase: GetDocumentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()


    private val _currentFormat = MutableStateFlow("pdf")
    val currentFormat: StateFlow<String> = _currentFormat.asStateFlow()

    private val pdfBitmapCache = mutableMapOf<String, Bitmap>()


    init {
        loadDocuments(currentFormat.value)
    }

    fun setCurrentFormat(format: String) {
        _currentFormat.value = format
    }

    fun refreshDocuments() {
        loadDocuments(_currentFormat.value)
    }

    fun getCachedPdfBitmap(documentId: String, pageIndex: Int): Bitmap? {
        return pdfBitmapCache["$documentId:$pageIndex"]
    }

    fun cachePdfBitmap(documentId: String, pageIndex: Int, bitmap: Bitmap) {
        pdfBitmapCache["$documentId:$pageIndex"] = bitmap
    }

    fun deleteDocument(context: Context, fileName: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanedFileName = fileName.substringBefore("#")
            val file = File(context.filesDir, cleanedFileName)

            val deleted = if (file.exists()) {
                file.delete().also {
                    if (it) {
                        Log.d("Delete", "Successfully deleted file: ${file.path}")

                        val updatedDocuments = _uiState.value.documents.filterNot { document ->
                            document.pages.any { page ->
                                page.imageUri.contains(cleanedFileName)
                            }
                        }

                        _uiState.value = _uiState.value.copy(documents = updatedDocuments)
                        refreshDocuments()
                    } else {
                        Log.e("Delete", "Failed to delete file: ${file.absolutePath}")
                    }
                }
            } else {
                Log.w("Delete", "File does not exist: ${file.absolutePath}")
                false
            }

            withContext(Dispatchers.Main) {
                onResult(deleted)
            }
        }
    }



    fun loadDocuments(desiredFormat: String) {
        viewModelScope.launch {
            getDocumentsUseCase(desiredFormat).collect { documents ->
                _uiState.value = DocumentsUiState(
                    documents = documents,
                    isLoading = false
                )
            }
        }
    }

    fun mergeAndSaveImages(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        filesDir: File,
        fileName: String,
        format: String
    ) {
        viewModelScope.launch {
            try {
                val resultPath = mergeAndSaveImagesToSingleFileSuspend(
                    uris,
                    contentResolver,
                    filesDir,
                    fileName,
                    format
                )

                if (format.lowercase(Locale.ROOT) == "pdf") {
                    addDocumentFromFile(resultPath)
                } else {
                    val savedUris = resultPath.split(",").map { Uri.fromFile(File(it)) }
                    addDocumentFromFile(resultPath, savedUris)
                }

                _uiState.value = _uiState.value.copy(error = null)
                setCurrentFormat(format)
                refreshDocuments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private suspend fun mergeAndSaveImagesToSingleFileSuspend(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        filesDir: File,
        fileName: String,
        format: String
    ): String = withContext(Dispatchers.IO) {
        when (format.lowercase(Locale.ROOT)) {
            "pdf" -> {
                val pdfFiles = mutableListOf<File>()
                var needsConversion = false

                for (uri in uris) {
                    val mimeType = contentResolver.getType(uri) ?: ""
                    if (mimeType == "application/pdf" || uri.toString().endsWith(".pdf")) {
                        val filePath = uri.path
                        if (filePath != null) {
                            val file = File(filePath)
                            if (file.exists()) {
                                pdfFiles.add(file)
                            } else {
                                needsConversion = true
                                break
                            }
                        } else {
                            needsConversion = true
                            break
                        }
                    } else {
                        needsConversion = true
                        break
                    }
                }

                if (!needsConversion && pdfFiles.isNotEmpty()) {
                    return@withContext pdfFiles.first().absolutePath
                }

                val document = PdfDocument()
                var pageIndex = 0

                uris.forEach { uri ->
                    val bitmaps = extractBitmapsFromUri(uri, contentResolver)
                    bitmaps.forEach { bitmap ->
                        pageIndex++
                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageIndex).create()
                        val page = document.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        document.finishPage(page)
                    }
                }

                val file = File(filesDir, "$fileName.pdf")
                document.writeTo(FileOutputStream(file))
                document.close()
                file.absolutePath
            }

            "png", "jpg", "jpeg" -> {
                val savedFiles = mutableListOf<String>()
                var fileCounter = 0

                uris.forEach { uri ->
                    val bitmaps = extractBitmapsFromUri(uri, contentResolver)
                    bitmaps.forEach { bitmap ->
                        fileCounter++
                        val ext = if (format.lowercase(Locale.ROOT) == "jpeg") "jpg" else format.lowercase(Locale.ROOT)
                        val imageFile = File(filesDir, "$fileName-$fileCounter.$ext")
                        val formatEnum = when (ext) {
                            "jpg" -> Bitmap.CompressFormat.JPEG
                            "png" -> Bitmap.CompressFormat.PNG
                            else -> Bitmap.CompressFormat.PNG
                        }
                        FileOutputStream(imageFile).use { out ->
                            bitmap.compress(formatEnum, 100, out)
                        }
                        savedFiles.add(imageFile.absolutePath)
                    }
                }

                savedFiles.firstOrNull() ?: throw IllegalStateException("No images saved")
            }

            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    fun loadDocumentPages(documentId: String, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val document = _uiState.value.documents.find { it.id == documentId }
                if (document == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Document not found"
                    )
                    return@launch
                }

                when (document.format.lowercase()) {
                    "pdf" -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    }

                    "png", "jpg", "jpeg" -> {
                        document.pages.forEach { page ->
                            val uri = page.imageUri.toUri()
                            if (getCachedPdfBitmap(document.id, page.order - 1) == null) {
                                try {
                                    val inputStream = when (uri.scheme) {
                                        "file" -> File(uri.path!!).inputStream()
                                        "content" -> context.contentResolver.openInputStream(uri)
                                        else -> null
                                    }

                                    inputStream?.use { stream ->
                                        val bitmap = BitmapFactory.decodeStream(stream)
                                        bitmap?.let {
                                            cachePdfBitmap(document.id, page.order - 1, it)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("DocumentViewModel", "Error loading image: ${e.message}")
                                }
                            }
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    }

                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Unsupported format: ${document.format}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load document: ${e.message}"
                )
            }
        }
    }


    private fun extractBitmapsFromUri(uri: Uri, contentResolver: ContentResolver): List<Bitmap> {
        val mimeType = contentResolver.getType(uri) ?: ""

        return when {
            mimeType == "application/pdf" || uri.toString().endsWith(".pdf") -> {
                extractBitmapsFromPdf(uri, contentResolver)
            }
            mimeType.startsWith("image/") -> {
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                if (bitmap != null) listOf(bitmap) else emptyList()
            }
            else -> {
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                if (bitmap != null) listOf(bitmap) else emptyList()
            }
        }
    }

    private fun extractBitmapsFromPdf(uri: Uri, contentResolver: ContentResolver): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            for (pageIndex in 0 until renderer.pageCount) {
                renderer.openPage(pageIndex).use { page ->
                    val width = page.width
                    val height = page.height
                    val bitmap = createBitmap(width, height)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                }
            }
            renderer.close()
        }
        return bitmaps
    }

    fun addDocumentFromFile(filePath: String, pageUris: List<Uri>? = null) {
        val file = File(filePath)
        val fileNameWithoutExtension = file.nameWithoutExtension
        val extension = file.extension.lowercase(Locale.ROOT)
        val timestamp = System.currentTimeMillis()

        val documentId = fileNameWithoutExtension
        val pages = pageUris?.mapIndexed { index, uri ->
            Page(
                id = "page_${documentId}_$index",
                imageUri = uri.toString(),
                order = index + 1
            )
        } ?: listOf(
            Page(
                id = "page_$documentId",
                imageUri = filePath,
                order = 1
            )
        )

        val document = Document(
            id = documentId,
            title = "Scanned Document $documentId",
            createdAt = timestamp,
            pages = pages,
            format = extension.ifEmpty { "unknown" }
        )

        _uiState.value = _uiState.value.copy(
            documents = _uiState.value.documents + document
        )
    }
}

data class DocumentsUiState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val error: String? = null
)