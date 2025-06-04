package com.prayag.pocketscanner.scanner.utils

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.prayag.pocketscanner.scanner.domain.repository.PdfMetadata
import com.prayag.pocketscanner.scanner.presentation.states.cloudstorage.CloudProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class PdfUtils(private val context: Context) {

    companion object {
        private const val TAG = "PdfUtils"
        private const val CACHE_DIR = "external_pdfs"
    }

    fun isValidPdf(file: File): Boolean {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).close()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Invalid PDF: ${e.message}")
            false
        }
    }

    fun isValidPdf(uri: Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).close()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Invalid PDF URI: ${e.message}")
            false
        }
    }

    fun extractPagesFromPdf(file: File): List<Page> {
        val pages = mutableListOf<Page>()
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        pages.add(
                            Page(
                                id = UUID.randomUUID().toString(),
                                imageUri = "${file.absolutePath}#page=$i",
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

    fun extractPagesFromPdf(uri: Uri): List<Page> {
        val pages = mutableListOf<Page>()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        pages.add(
                            Page(
                                id = UUID.randomUUID().toString(),
                                imageUri = "$uri#page=$i",
                                order = i
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading PDF pages from URI: ${e.message}", e)
        }
        return pages
    }

    fun getPdfMetadata(uri: Uri): PdfMetadata? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    var title: String? = null
                    var size: Long = 0

                    cursor?.use {
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                            if (nameIndex != -1) title = cursor.getString(nameIndex)
                            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                        }
                    }

                    PdfMetadata(
                        title = title,
                        author = null, // PDF metadata extraction would require additional library
                        creationDate = null,
                        pageCount = renderer.pageCount,
                        fileSize = size,
                        isPasswordProtected = false // Would need additional check
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF metadata: ${e.message}")
            null
        }
    }

    fun cachePdfLocally(uri: Uri, fileName: String): String? {
        return try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val cachedFile = File(cacheDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(cachedFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            cachedFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error caching PDF: ${e.message}")
            null
        }
    }

    fun detectCloudProvider(uri: Uri): CloudProvider? {
        val authority = uri.authority ?: return null

        return when {
            authority.contains("com.google.android.apps.docs.storage") -> CloudProvider.GOOGLE_DRIVE
            authority.contains("dropbox") -> CloudProvider.DROPBOX
            authority.contains("onedrive") -> CloudProvider.ONEDRIVE
            authority.contains("icloud") -> CloudProvider.ICLOUD
            else -> null
        }
    }
}