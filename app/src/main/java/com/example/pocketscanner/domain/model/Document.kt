package com.example.pocketscanner.domain.model

data class Document(
    val id: String = "",
    val title: String = "Untitled Document",
    val createdAt: Long = 0L,
    val pages: List<Page> = emptyList(),
    val format: String = "pdf",
    val tags: List<String> = emptyList(),
    val score: Int = 0
)

data class Page(
    val id: String = "",
    val imageUri: String = "",
    val order: Int = 0,
    val isEnhanced: Boolean = false
)
