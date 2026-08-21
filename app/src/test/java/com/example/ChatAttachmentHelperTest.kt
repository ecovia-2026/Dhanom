package com.example

import com.example.data.export.ChatAttachmentHelper
import org.junit.Assert.*
import org.junit.Test

class ChatAttachmentHelperTest {

    @Test
    fun maxUploadIs500Mb() {
        assertEquals(500L * 1024 * 1024, ChatAttachmentHelper.MAX_UPLOAD_BYTES)
    }

    @Test
    fun detectsJpegByMagicBytesEvenWithoutMime() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertTrue(ChatAttachmentHelper.isImage("photo", "", jpeg))
        assertTrue(ChatAttachmentHelper.isImage("IMG_0001", "application/octet-stream", jpeg))
    }

    @Test
    fun detectsPngByNameAndHeader() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertTrue(ChatAttachmentHelper.isImage("shot.PNG", "application/octet-stream", ByteArray(0)))
        assertTrue(ChatAttachmentHelper.isImage("untitled", "", png))
    }

    @Test
    fun doesNotTreatPdfAsImage() {
        val pdf = "%PDF-1.4".toByteArray()
        assertFalse(ChatAttachmentHelper.isImage("statement.pdf", "application/pdf", pdf))
    }

    @Test
    fun textDetectionRejectsBinaryNulls() {
        assertTrue(ChatAttachmentHelper.isProbablyText("hello world\ncsv,ok".toByteArray()))
        assertFalse(ChatAttachmentHelper.isProbablyText(byteArrayOf(0, 1, 2, 3, 4, 5)))
    }

    @Test
    fun formatSizeAndSanitize() {
        assertEquals("500.0 MB", ChatAttachmentHelper.formatSize(ChatAttachmentHelper.MAX_UPLOAD_BYTES))
        assertEquals("photo.jpg", ChatAttachmentHelper.sanitizeFileName("photo.jpg"))
        assertEquals("bad_name.png", ChatAttachmentHelper.sanitizeFileName("bad/name.png"))
    }
}
