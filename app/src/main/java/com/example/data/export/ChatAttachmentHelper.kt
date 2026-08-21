package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Crash-proof chat attachments.
 *
 * - Accepts **any** file type up to [MAX_UPLOAD_BYTES] (500 MB).
 * - Never loads the whole file into RAM: copies with a 64 KB buffer and
 *   aborts if the running total exceeds the cap (covers providers that
 *   report SIZE=0, which is common for gallery images).
 * - Detects images by MIME, extension **and** magic bytes so a JPEG with
 *   a missing/empty MIME is never decoded as UTF-8 text (the previous
 *   crash: a 12 MP photo became a ~30 MB String and OOM'd the process).
 * - Images are downsampled to a preview JPEG for the bubble; the original
 *   is kept on disk.
 */
object ChatAttachmentHelper {

    const val MAX_UPLOAD_BYTES = 500L * 1024 * 1024
    const val MAX_PARSE_BYTES = 8L * 1024 * 1024
    const val PREVIEW_MAX_DIM = 1280

    data class CopiedFile(
        val file: File,
        val displayName: String,
        val mime: String,
        val size: Long,
        val isImage: Boolean,
        val previewFile: File?
    )

    fun queryDisplayName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst() && !cursor.isNull(idx)) {
                    cursor.getString(idx) ?: ""
                } else ""
            } ?: ""
        } catch (_: Throwable) {
            ""
        }.ifBlank {
            uri.lastPathSegment?.substringAfterLast('/') ?: "attachment"
        }
    }

    fun querySize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && cursor.moveToFirst() && !cursor.isNull(idx)) cursor.getLong(idx) else 0L
            } ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }

    fun queryMime(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.getType(uri) ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun tryTakePersistablePermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Throwable) {
            // GetContent / PickVisualMedia URIs are not persistable — that's fine.
        }
    }

    /**
     * Stream-copy [uri] into app-private storage. Throws if the file exceeds
     * 500 MB or cannot be opened. Never uses `readBytes()` on the source.
     */
    fun copyFromUri(context: Context, uri: Uri): CopiedFile {
        tryTakePersistablePermission(context, uri)

        val name = sanitizeFileName(queryDisplayName(context, uri))
        val mime = queryMime(context, uri)
        val reported = querySize(context, uri)
        if (reported > MAX_UPLOAD_BYTES) {
            throw IllegalArgumentException(
                "File too large (${formatSize(reported)}). Max is ${formatSize(MAX_UPLOAD_BYTES)}."
            )
        }

        val dir = File(context.filesDir, "chat_uploads").apply { mkdirs() }
        val dest = File(dir, "${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}-$name")

        val copied = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().buffered(64 * 1024).use { output ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    var header: ByteArray? = null
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        total += n
                        if (total > MAX_UPLOAD_BYTES) {
                            throw IllegalArgumentException(
                                "File too large (over ${formatSize(MAX_UPLOAD_BYTES)})."
                            )
                        }
                        if (header == null) header = buf.copyOf(n.coerceAtMost(32))
                        output.write(buf, 0, n)
                    }
                    Pair(total, header ?: ByteArray(0))
                }
            } ?: throw IllegalStateException("Could not open the selected file")
        } catch (t: Throwable) {
            dest.delete()
            throw t
        }

        val size = copied.first
        val header = copied.second
        if (size <= 0L) {
            dest.delete()
            throw IllegalStateException("The selected file was empty")
        }

        val image = isImage(name, mime, header)
        val preview = if (image) {
            val previewFile = File(dir, "preview-${dest.nameWithoutExtension}.jpg")
            if (downsampleImage(dest, previewFile)) previewFile else dest
        } else null

        return CopiedFile(
            file = dest,
            displayName = name,
            mime = mime.ifBlank { if (image) "image/*" else "application/octet-stream" },
            size = size,
            isImage = image,
            previewFile = preview
        )
    }

    fun readPrefix(file: File, maxBytes: Long = MAX_PARSE_BYTES): ByteArray {
        val limit = minOf(file.length(), maxBytes).toInt().coerceAtLeast(0)
        if (limit == 0) return ByteArray(0)
        file.inputStream().use { input ->
            val out = ByteArray(limit)
            var offset = 0
            while (offset < limit) {
                val n = input.read(out, offset, limit - offset)
                if (n <= 0) break
                offset += n
            }
            return if (offset == limit) out else out.copyOf(offset)
        }
    }

    fun isImage(name: String, mime: String, header: ByteArray): Boolean {
        val m = mime.lowercase(Locale.US)
        if (m.startsWith("image/")) return true
        val n = name.lowercase(Locale.US)
        if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") ||
            n.endsWith(".webp") || n.endsWith(".gif") || n.endsWith(".bmp") ||
            n.endsWith(".heic") || n.endsWith(".heif") || n.endsWith(".avif") ||
            n.endsWith(".tif") || n.endsWith(".tiff") || n.endsWith(".svg")
        ) return true
        if (header.size >= 3 &&
            header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()
        ) return true // JPEG
        if (header.size >= 8 &&
            header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
            header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()
        ) return true // PNG
        if (header.size >= 6 &&
            header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte()
        ) return true // GIF
        if (header.size >= 12 &&
            header.copyOfRange(0, 4).contentEquals("RIFF".toByteArray()) &&
            header.copyOfRange(8, 12).contentEquals("WEBP".toByteArray())
        ) return true // WEBP
        if (header.size >= 2 && header[0] == 0x42.toByte() && header[1] == 0x4D.toByte()) return true // BMP
        return false
    }

    fun isProbablyText(header: ByteArray): Boolean {
        if (header.isEmpty()) return false
        val sample = header.take(512)
        var weird = 0
        for (b in sample) {
            val u = b.toInt() and 0xFF
            if (u == 0) return false
            if (u < 9 || (u > 13 && u < 32)) weird++
        }
        return weird < sample.size / 10
    }

    fun downsampleImage(src: File, dest: File): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(src.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
            var sample = 1
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxSide / sample > PREVIEW_MAX_DIM) sample *= 2
            val bmp = BitmapFactory.decodeFile(
                src.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return false
            dest.outputStream().buffered().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
            }
            bmp.recycle()
            dest.exists() && dest.length() > 0L
        } catch (_: Throwable) {
            dest.delete()
            false
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        return String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }

    fun sanitizeFileName(raw: String): String {
        val cleaned = raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(80)
        return cleaned.ifBlank { "attachment" }
    }
}
