package com.example.pocketscanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.graphics.createBitmap

class Utils {
    fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int): Bitmap? {
        val contentResolver = context.contentResolver
        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
        val renderer = PdfRenderer(fileDescriptor)

        if (pageIndex >= renderer.pageCount) {
            renderer.close()
            return null
        }

        val page = renderer.openPage(pageIndex)
        val bitmap = createBitmap(page.width, page.height)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()

        return bitmap
    }

}