package com.example.pocketscanner.presentation.viewmodels



import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketscanner.domain.model.Document
import com.example.pocketscanner.domain.model.Page
import com.example.pocketscanner.domain.usecase.GetDocumentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentViewModel(
    private val getDocumentsUseCase: GetDocumentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        getDocumentsUseCase().onEach { documents ->
            _uiState.value = DocumentsUiState(
                documents = documents,
                isLoading = false
            )
        }.launchIn(viewModelScope)
    }

    fun startScan() {
        _uiState.value = _uiState.value.copy(isScanning = true)
    }

    fun onScanComplete() {
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    suspend fun saveImageToFile(
        imageUri: Uri,
        contentResolver: ContentResolver,
        filesDir: File,
        fileName: String,
        format: String
    ): String? {
        // Determine file extension and mime type
        val extension = when (format) {
            "jpg" -> "jpg"
            "png" -> "png"
            "pdf" -> "pdf"
            else -> "jpg"
        }

        val targetFile = File(filesDir, "$fileName.$extension")

        contentResolver.openInputStream(imageUri)?.use { inputStream ->
            targetFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return targetFile.absolutePath
    }


    fun addDocumentFromFile(filePath: String) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val documentId = timestamp.toString()

            val page = Page(
                id = "page_$timestamp",
                imageUri = filePath,
                order = 1
            )

            val document = Document(
                id = documentId,
                title = "Scanned Document $timestamp",
                createdAt = timestamp,
                pages = listOf(page)
            )

            _uiState.value = _uiState.value.copy(
                documents = _uiState.value.documents + document
            )
        }
    }


}

data class DocumentsUiState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val error: String? = null
)