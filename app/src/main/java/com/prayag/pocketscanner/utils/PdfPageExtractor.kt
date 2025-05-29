package com.prayag.pocketscanner.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.prayag.pocketscanner.scanner.domain.model.Document
import com.prayag.pocketscanner.scanner.domain.model.Page
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

class PdfPageExtractor {

    fun getPageCount(context: Context, uri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
            val document = PDDocument.load(inputStream)
            val pageCount = document.numberOfPages
            document.close()
            pageCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun extractPageImage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        scale: Double = 1.0 // Ignored here since we are not generating images
    ): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val document = PDDocument.load(inputStream)

            if (pageIndex >= document.numberOfPages) {
                document.close()
                return null
            }

            val outputDoc = PDDocument()
            val page = document.getPage(pageIndex)
            outputDoc.addPage(page)

            val outputFile = File(context.cacheDir, "page_$pageIndex.pdf")
            outputDoc.save(outputFile)

            outputDoc.close()
            document.close()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createDocumentFromExternalPdf(
        context: Context,
        pdfUri: Uri,
        fileName: String
    ): Document? {
        return try {
            val inputStream = context.contentResolver.openInputStream(pdfUri) ?: return null
            val document = PDDocument.load(inputStream)
            val pages = mutableListOf<Page>()

            for (i in 0 until document.numberOfPages) {
                val pdfFile = extractPageImage(context, pdfUri, i)
                pdfFile?.let {
                    pages.add(
                        Page(
                            id = "ext_page_$i",
                            imageUri = "${it.absolutePath}#page=$i".toUri().toString(),
                            order = i
                        )
                    )
                }
            }

            document.close()

            Document(
                id = "ext_${System.currentTimeMillis()}",
                title = fileName,
                pages = pages.sortedBy { it.order },
                createdAt = System.currentTimeMillis(),
                format = "pdf"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
