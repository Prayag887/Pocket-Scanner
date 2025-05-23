package com.prayag.pocketscanner.scanner.domain.model

import android.net.Uri

data class Document(
    val id: String = "",
    val title: String = "",
    val createdAt: Long = 0L,
    val pages: List<Page> = emptyList(),
    val format: String = "pdf",
    val tags: List<String> = emptyList(),
    val score: Int = 0,
    val thumbnail: Uri = Uri.EMPTY
)

data class Page(
    val id: String = "",
    val imageUri: String = "",
    val order: Int = 0,
    val isEnhanced: Boolean = false
)
