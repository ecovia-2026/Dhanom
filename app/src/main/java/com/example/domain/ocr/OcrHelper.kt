package com.example.domain.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * On-device OCR (Google ML Kit, standalone). Extracts text from images and
 * from PDF pages (PDF pages are rasterized with android.graphics.pdf.PdfRenderer
 * and then OCR'd — works even for scanned/image-only PDFs with no text layer).
 */
object OcrHelper {

    suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { v -> if (cont.isActive) cont.resume(v.text ?: "") }
                .addOnFailureListener { if (cont.isActive) cont.resume("") }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume("")
        }
    }

    /** OCR an image (JPEG/PNG bytes). */
    suspend fun ocrImage(bytes: ByteArray): String = withContext(Dispatchers.IO) {
        try {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext ""
            val text = recognize(bmp)
            bmp.recycle()
            text
        } catch (e: Exception) {
            ""
        }
    }

    /** OCR a PDF by rendering each page to a bitmap (max 12 pages). */
    suspend fun ocrPdf(pdf: File, maxPages: Int = 12): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        try {
            val fd = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val pages = minOf(renderer.pageCount, maxPages)
            for (i in 0 until pages) {
                try {
                    val page = renderer.openPage(i)
                    val scale = 2f
                    val w = (page.width * scale).toInt().coerceAtLeast(1)
                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    sb.append(recognize(bmp)).append('\n')
                    bmp.recycle()
                } catch (e: Exception) {
                    // skip a page that fails to render
                }
            }
            renderer.close()
            fd.close()
        } catch (e: Exception) {
            // ignore
        }
        sb.toString().trim()
    }
}
