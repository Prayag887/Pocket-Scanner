package com.prayag.pocketscanner.scanner.utils

import android.net.Uri
import androidx.core.net.toUri
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import java.io.File
import java.util.UUID

class FileUtils {

    companion object {
        private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png")
        private val SUPPORTED_EXTENSIONS = listOf("jpg", "jpeg", "png", "pdf")
    }

    fun isPdf(file: File): Boolean {
        return file.extension.equals("pdf", ignoreCase = true)
    }

    fun isImage(file: File): Boolean {
        return IMAGE_EXTENSIONS.contains(file.extension.lowercase())
    }

    fun isSupportedFile(file: File): Boolean {
        return SUPPORTED_EXTENSIONS.contains(file.extension.lowercase())
    }

    fun shouldSkipFile(file: File): Boolean {
        return file.name.startsWith("temp") || file.name.startsWith(".")
    }

    fun createDocumentFromFile(file: File, pages: List<Page>): Document? {
        if (pages.isEmpty()) return null

        val thumbnailUri = if (isPdf(file)) {
            "${file.absolutePath}#page=0".toUri()
        } else {
            Uri.fromFile(file)
        }

        return Document(
            id = file.nameWithoutExtension,
            title = file.nameWithoutExtension,
            createdAt = file.lastModified(),
            pages = pages,
            tags = listOf(),
            score = 0,
            format = file.extension.lowercase(),
            thumbnail = thumbnailUri
        )
    }

    fun createImagePage(file: File): Page {
        return Page(
            id = UUID.randomUUID().toString(),
            imageUri = file.absolutePath,
            order = 0
        )
    }
}