package com.example.data.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Saves generated files directly to the device's Downloads folder (visible in
 * the Files app) and returns a human-readable location. Uses MediaStore on
 * Android 10+ (scoped storage, no permission needed); falls back to app
 * external files dir on older devices.
 */
object FileSaver {

    data class SaveResult(val ok: Boolean, val location: String, val file: File?)

    fun saveToDownloads(context: Context, source: File, displayName: String, mime: String): SaveResult {
        return try {
            val name = sanitize(displayName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Dhan-OM")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri == null) return SaveResult(false, "Could not access Downloads", null)
                resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                SaveResult(true, "Internal storage/Download/Dhan-OM/$name", null)
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Dhan-OM")
                if (!dir.exists()) dir.mkdirs()
                val dest = File(dir, name)
                source.copyTo(dest, overwrite = true)
                SaveResult(true, dest.absolutePath, dest)
            }
        } catch (e: Exception) {
            SaveResult(false, "Save failed: ${e.message}", null)
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "dhanom_file" }
}
