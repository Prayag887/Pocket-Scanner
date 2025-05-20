package com.prayag.pocketscanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap

class Utils {
    fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int): Bitmap? {
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null

        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            renderer = PdfRenderer(fileDescriptor)

            if (pageIndex >= renderer.pageCount) {
                return null
            }

            page = renderer.openPage(pageIndex)
            val bitmap = createBitmap(page.width, page.height)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            page?.close()
            renderer?.close()
            fileDescriptor?.close()
        }
    }

}