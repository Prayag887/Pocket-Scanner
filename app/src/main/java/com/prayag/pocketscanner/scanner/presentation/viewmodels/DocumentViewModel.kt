package com.prayag.pocketscanner.scanner.presentation.viewmodels

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
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.domain.usecase.GetDocumentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import androidx.core.net.toUri
import kotlinx.coroutines.flow.update
import androidx.collection.LruCache
import com.prayag.pocketscanner.scanner.presentation.states.DocumentsUiState
import com.prayag.pocketscanner.scanner.presentation.states.NavigationEvent
import kotlin.math.min

class DocumentViewModel(
    private val getDocumentsUseCase: GetDocumentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    private val _currentFormat = MutableStateFlow("pdf")
    val currentFormat: StateFlow<String> = _currentFormat.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    // Improved bitmap cache with safer memory management
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // 1/8th of available memory

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.allocationByteCount / 1024
        }
        // Removed entryRemoved to prevent premature recycling
    }

    // Job management for cancellation
    private var loadDocumentsJob: Job? = null
    private var processImagesJob: Job? = null

    init {
        loadDocuments(currentFormat.value)
    }

    fun setCurrentFormat(format: String) {
        if (_currentFormat.value != format) {
            _currentFormat.value = format
            loadDocuments(format)
        }
    }

    fun refreshDocuments() {
        loadDocuments(_currentFormat.value)
    }

    fun getCachedBitmap(key: String): Bitmap? {
        val bitmap = bitmapCache.get(key)
        // Return null if bitmap is recycled to prevent crashes
        return if (bitmap?.isRecycled == false) bitmap else null
    }

    fun cacheBitmap(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            bitmapCache.put(key, bitmap)
        }
    }

    fun deleteDocument(context: Context, fileName: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val cleanedFileName = fileName.substringBefore("#")
                val file = File(context.filesDir, cleanedFileName)

                val deleted = if (file.exists()) {
                    val result = file.delete()
                    if (result) {
                        Log.d("Delete", "Successfully deleted file: ${file.path}")

                        // Clear related cache entries BEFORE updating UI
                        clearCacheForDocument(cleanedFileName)

                        // Update state immediately for better UX
                        _uiState.update { state ->
                            state.copy(
                                documents = state.documents.filterNot { document ->
                                    document.pages.any { it.imageUri.contains(cleanedFileName) }
                                },
                                isLoading = false
                            )
                        }

                        withContext(Dispatchers.Main) {
                            onResult(true)
                        }

                        // Refresh documents in background
                        refreshDocuments()
                    } else {
                        Log.e("Delete", "Failed to delete file: ${file.absolutePath}")
                        _uiState.update { it.copy(isLoading = false) }
                        withContext(Dispatchers.Main) {
                            onResult(false)
                        }
                    }
                    result
                } else {
                    Log.w("Delete", "File does not exist: ${file.absolutePath}")
                    _uiState.update { it.copy(isLoading = false) }
                    withContext(Dispatchers.Main) {
                        onResult(false)
                    }
                    false
                }
            } catch (e: Exception) {
                Log.e("Delete", "Error deleting document: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    private fun loadDocuments(desiredFormat: String) {
        loadDocumentsJob?.cancel()
        loadDocumentsJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                getDocumentsUseCase(desiredFormat).collect { documents ->
                    _uiState.update {
                        it.copy(
                            documents = documents,
                            isLoading = false,
                            isDataReady = true
                        )
                    }

                    // Send navigation event when data is ready
                    _navigationEvent.send(NavigationEvent.DataLoaded)
                }
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error loading documents: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load documents: ${e.message}",
                        isDataReady = false
                    )
                }
            }
        }
    }

    suspend fun mergeAndSaveImages(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        filesDir: File,
        fileName: String,
        format: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isProcessing = true, error = null) }

                val resultPath = processImages(uris, contentResolver, filesDir, fileName, format)

                if (format.lowercase(Locale.ROOT) == "pdf") {
                    addDocumentFromFile(resultPath)
                } else {
                    val savedUris = resultPath.split(",").map { Uri.fromFile(File(it)) }
                    addDocumentFromFile(resultPath, savedUris)
                }

                _uiState.update { it.copy(isProcessing = false) }
                setCurrentFormat(format)

                resultPath // return the actual saved file path

            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error processing images: ${e.message}")
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Failed to process images: ${e.message}"
                    )
                }
                throw e // rethrow so caller (e.g., ScanScreen) can handle it
            }
        }
    }

    private suspend fun processImages(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        filesDir: File,
        fileName: String,
        format: String
    ): String = withContext(Dispatchers.IO) {
        when (format.lowercase(Locale.ROOT)) {
            "pdf" -> processToPdf(uris, contentResolver, filesDir, fileName)
            "png", "jpg", "jpeg" -> processToImages(uris, contentResolver, filesDir, fileName, format)
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private suspend fun processToPdf(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        filesDir: File,
        fileName: String
    ): String = withContext(Dispatchers.IO) {
        // Quick path for single PDF files
        if (uris.size == 1) {
            val uri = uris.first()
            val mimeType = contentResolver.getType(uri) ?: ""
            if (mimeType == "application/pdf" && uri.scheme == "file") {
                val file = File(uri.path!!)
                if (file.exists()) return@withContext file.absolutePath
            }
        }

        // Create new PDF with safer bitmap handling
        val document = PdfDocument()
        var pageIndex = 0
        val processedBitmaps = mutableListOf<Bitmap>() // Track bitmaps for cleanup

        try {
            for (uri in uris) {
                yield() // Allow cancellation

                val bitmaps = extractBitmapsFromUri(uri, contentResolver, optimizeForPdf = true)
                for (bitmap in bitmaps) {
                    yield() // Allow cancellation

                    if (bitmap.isRecycled) {
                        Log.w("DocumentViewModel", "Skipping recycled bitmap")
                        continue
                    }

                    pageIndex++
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        bitmap.width,
                        bitmap.height,
                        pageIndex
                    ).create()

                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)

                    // Keep track of bitmaps for later cleanup
                    processedBitmaps.add(bitmap)
                }
            }

            val file = File(filesDir, "$fileName.pdf")
            BufferedOutputStream(FileOutputStream(file), 8192).use { out ->
                document.writeTo(out)
            }

            return@withContext file.absolutePath
        } finally {
            document.close()

            // Clean up bitmaps after PDF is completely written
            processedBitmaps.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    private suspend fun processToImages(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        filesDir: File,
        fileName: String,
        format: String
    ): String = withContext(Dispatchers.IO) {
        val savedFiles = mutableListOf<String>()
        var fileCounter = 0
        val ext = if (format.lowercase(Locale.ROOT) == "jpeg") "jpg" else format.lowercase(Locale.ROOT)
        val formatEnum = when (ext) {
            "jpg" -> Bitmap.CompressFormat.JPEG
            else -> Bitmap.CompressFormat.PNG
        }
        val quality = if (ext == "jpg") 90 else 100
        val processedBitmaps = mutableListOf<Bitmap>() // Track bitmaps for cleanup

        try {
            for (uri in uris) {
                yield() // Allow cancellation

                val bitmaps = extractBitmapsFromUri(uri, contentResolver, optimizeForPdf = false)
                for (bitmap in bitmaps) {
                    yield() // Allow cancellation

                    if (bitmap.isRecycled) {
                        Log.w("DocumentViewModel", "Skipping recycled bitmap")
                        continue
                    }

                    fileCounter++
                    val imageFile = File(filesDir, "$fileName-$fileCounter.$ext")

                    BufferedOutputStream(FileOutputStream(imageFile), 8192).use { out ->
                        bitmap.compress(formatEnum, quality, out)
                    }
                    savedFiles.add(imageFile.absolutePath)

                    // Keep track of bitmaps for later cleanup
                    processedBitmaps.add(bitmap)
                }
            }

            return@withContext savedFiles.joinToString(",")
        } finally {
            // Clean up bitmaps after all files are written
            processedBitmaps.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    fun loadDocumentPages(documentId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val document = _uiState.value.documents.find { it.id == documentId }
                    ?: throw IllegalStateException("Document not found")

                when (document.format.lowercase()) {
                    "pdf" -> {
                        // PDF pages are rendered on-demand, no pre-loading needed
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentDocument = document,
                                isDocumentReady = true
                            )
                        }
                        _navigationEvent.send(NavigationEvent.DocumentLoaded(documentId))
                    }
                    "png", "jpg", "jpeg" -> {
                        loadImagePages(document, context)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentDocument = document,
                                isDocumentReady = true
                            )
                        }
                        _navigationEvent.send(NavigationEvent.DocumentLoaded(documentId))
                    }
                    else -> throw IllegalStateException("Unsupported format: ${document.format}")
                }
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error loading document pages: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load document: ${e.message}",
                        isDocumentReady = false
                    )
                }
                _navigationEvent.send(NavigationEvent.DocumentLoadFailed(e.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun loadImagePages(document: Document, context: Context) {
        for (page in document.pages) {
            yield() // Allow cancellation

            val uri = page.imageUri.toUri()
            val cacheKey = "${document.id}:${page.order - 1}"

            if (getCachedBitmap(cacheKey) == null) {
                try {
                    val bitmap = loadOptimizedBitmap(uri, context.contentResolver)
                    bitmap?.let {
                        if (!it.isRecycled) {
                            cacheBitmap(cacheKey, it)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DocumentViewModel", "Error loading image page: ${e.message}")
                }
            }
        }
    }

    private suspend fun loadOptimizedBitmap(uri: Uri, contentResolver: ContentResolver): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = when (uri.scheme) {
                    "file" -> File(uri.path!!).inputStream()
                    "content" -> contentResolver.openInputStream(uri)
                    else -> null
                } ?: return@withContext null

                inputStream.use { stream ->
                    // First, decode with inJustDecodeBounds=true to check dimensions
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(stream, null, options)

                    // Calculate inSampleSize for memory optimization
                    options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
                    options.inJustDecodeBounds = false
                    options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory

                    // Decode the actual bitmap
                    when (uri.scheme) {
                        "file" -> File(uri.path!!).inputStream()
                        "content" -> contentResolver.openInputStream(uri)
                        else -> null
                    }?.use { newStream ->
                        BitmapFactory.decodeStream(newStream, null, options)
                    }
                }
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error loading optimized bitmap: ${e.message}")
                null
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private suspend fun extractBitmapsFromUri(
        uri: Uri,
        contentResolver: ContentResolver,
        optimizeForPdf: Boolean = false
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val mimeType = contentResolver.getType(uri) ?: ""

        when {
            mimeType == "application/pdf" || uri.toString().endsWith(".pdf") -> {
                extractBitmapsFromPdf(uri, contentResolver, optimizeForPdf)
            }
            mimeType.startsWith("image/") || isImageUri(uri) -> {
                val bitmap = if (optimizeForPdf) {
                    loadOptimizedBitmap(uri, contentResolver)
                } else {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                bitmap?.let {
                    if (!it.isRecycled) listOf(it) else emptyList()
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun isImageUri(uri: Uri): Boolean {
        val uriString = uri.toString().lowercase()
        return uriString.endsWith(".jpg") ||
                uriString.endsWith(".jpeg") ||
                uriString.endsWith(".png") ||
                uriString.endsWith(".webp")
    }

    private suspend fun extractBitmapsFromPdf(
        uri: Uri,
        contentResolver: ContentResolver,
        optimizeForPdf: Boolean
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()

        try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (pageIndex in 0 until renderer.pageCount) {
                        yield() // Allow cancellation

                        renderer.openPage(pageIndex).use { page ->
                            val width = if (optimizeForPdf) min(page.width, 1024) else page.width
                            val height = if (optimizeForPdf) min(page.height, 1024) else page.height

                            val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            if (!bitmap.isRecycled) {
                                bitmaps.add(bitmap)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DocumentViewModel", "Error extracting PDF bitmaps: ${e.message}")
        }

        bitmaps
    }

    private fun addDocumentFromFile(filePath: String, pageUris: List<Uri>? = null) {
        val file = File(filePath)
        val documentId = file.nameWithoutExtension
        val extension = file.extension.lowercase(Locale.ROOT)
        val timestamp = System.currentTimeMillis()

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

        _uiState.update { it.copy(documents = it.documents + document) }
    }

    private fun clearCacheForDocument(fileName: String) {
        val keysToRemove = mutableListOf<String>()
        bitmapCache.snapshot().keys.forEach { key ->
            if (key.contains(fileName)) {
                keysToRemove.add(key)
            }
        }
        keysToRemove.forEach { key ->
            bitmapCache.remove(key)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancelChildren()

        // Safely clear the cache
        try {
            bitmapCache.evictAll()
        } catch (e: Exception) {
            Log.e("DocumentViewModel", "Error clearing bitmap cache: ${e.message}")
        }
    }
}