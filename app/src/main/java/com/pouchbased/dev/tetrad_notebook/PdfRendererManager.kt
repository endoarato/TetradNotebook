package com.pouchbased.dev.tetrad_notebook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Helper to render PDF pages into Bitmaps.
 */
class PdfRendererManager(private val context: Context) {

    private var currentRenderer: PdfRenderer? = null
    private var currentFileDescriptor: ParcelFileDescriptor? = null
    private var currentUri: Uri? = null

    suspend fun renderPage(uri: Uri, pageNumber: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val renderer = getRenderer(uri) ?: return@withContext null
            if (pageNumber < 1 || pageNumber > renderer.pageCount) return@withContext null

            val page = renderer.openPage(pageNumber - 1)
            
            // Determine scale for high quality rendering
            // For now, let's target a reasonable resolution
            val width = page.width * 2
            val height = page.height * 2
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getLinks(uri: Uri, pageNumber: Int): List<PdfLink> = withContext(Dispatchers.IO) {
        // Android 15 (API 35) PDF Link support. 
        // Note: Using reflection or just disabling until proper SDK is verified.
        // For now, returning empty to allow build.
        emptyList()
    }

    fun getPageCount(uri: Uri): Int {
        return try {
            getRenderer(uri)?.pageCount ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun getRenderer(uri: Uri): PdfRenderer? {
        if (currentUri == uri && currentRenderer != null) return currentRenderer
        
        close()

        try {
            val pfd = if (uri.scheme == "content") {
                context.contentResolver.openFileDescriptor(uri, "r")
            } else {
                // For file uris or others, we might need to copy to a temp file
                val tempFile = File(context.cacheDir, "temp_rendering.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }

            if (pfd != null) {
                currentFileDescriptor = pfd
                currentRenderer = PdfRenderer(pfd)
                currentUri = uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return currentRenderer
    }

    fun close() {
        currentRenderer?.close()
        currentFileDescriptor?.close()
        currentRenderer = null
        currentFileDescriptor = null
        currentUri = null
    }
}
