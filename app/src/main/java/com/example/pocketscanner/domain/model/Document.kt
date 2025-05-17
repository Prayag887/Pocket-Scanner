package com.example.pocketscanner.domain.model

data class Document(
    val id: String,
    val title: String,
    val createdAt: Long,
    val pages: List<Page>,
    val tags: List<String> = emptyList(),
    val score: Int = 0
)

data class Page(
    val id: String,
    val imageUri: String,
    val order: Int,
    val isEnhanced: Boolean = false
)