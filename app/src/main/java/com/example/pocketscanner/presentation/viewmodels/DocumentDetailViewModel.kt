package com.example.pocketscanner.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketscanner.domain.model.Document
import com.example.pocketscanner.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DocumentDetailViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _document = MutableStateFlow<Document?>(null)
    val document: StateFlow<Document?> = _document

    fun loadDocumentById(id: String, desiredFormat: String) {
        viewModelScope.launch {
            _document.value = repository.getDocumentById(id, desiredFormat)
        }
    }

}
