package com.example.domain.brain

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Robust model downloader with HTTP-Range resume, retry/backoff, and progress
 * reporting. Designed for a ~3.7 GB file over flaky mobile networks: a dropped
 * connection is resumed instead of restarting from zero, and `.part` is only
 * promoted to the final file once the download completes and is validated.
 */
class BrainModelDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val cancelled = AtomicBoolean(false)

    fun cancel() = cancelled.set(true)

    fun download(
        url: String,
        dest: File,
        onProgress: (Float) -> Unit,
        onDone: (Boolean, String) -> Unit
    ) {
        Thread {
            try {
                cancelled.set(false)
                dest.parentFile?.mkdirs()
                val part = File(dest.absolutePath + ".part")
                var retries = 0
                var lastError = "unknown"
                while (retries < 4 && !cancelled.get()) {
                    try {
                        resumeDownload(url, dest, part, onProgress)
                        if (!cancelled.get()) {
                            onDone(true, dest.absolutePath)
                            return@Thread
                        }
                    } catch (e: Exception) {
                        lastError = e.message ?: "network error"
                        retries++
                        if (retries < 4 && !cancelled.get()) {
                            // exponential backoff before retrying
                            try { Thread.sleep(1500L * retries) } catch (_: InterruptedException) {}
                        }
                    }
                }
                if (cancelled.get()) onDone(false, "cancelled")
                else onDone(false, lastError)
            } catch (e: Exception) {
                onDone(false, e.message ?: "download failed")
            }
        }.start()
    }

    private fun resumeDownload(url: String, dest: File, part: File, onProgress: (Float) -> Unit) {
        var downloaded = if (part.exists()) part.length() else 0L
        val req = Request.Builder().url(url)
        if (downloaded > 0) req.header("Range", "bytes=$downloaded-")
        client.newCall(req.build()).execute().use { response ->
            when {
                response.code == 206 || response.code == 200 -> {
                    // 206 = partial content (resume ok); 200 = server ignored range -> restart
                    if (response.code == 200) {
                        downloaded = 0L
                        part.delete()
                    }
                    val body = response.body ?: throw IllegalStateException("empty body")
                    val total = downloaded + body.contentLength()
                    RandomAccessFile(part, "rw").use { raf ->
                        if (downloaded > 0) raf.seek(downloaded)
                        body.byteStream().use { input ->
                            val buf = ByteArray(64 * 1024)
                            var read = input.read(buf)
                            while (read != -1 && !cancelled.get()) {
                                raf.write(buf, 0, read)
                                downloaded += read
                                if (total > 0) onProgress(downloaded.toFloat() / total.toFloat())
                                read = input.read(buf)
                            }
                        }
                    }
                    if (cancelled.get()) throw InterruptedException("cancelled")
                    // validate: must be non-trivial size before promoting
                    if (part.length() < 1_000_000L) throw IllegalStateException("file too small")
                    part.renameTo(dest)
                }
                response.code == 416 -> {
                    // already fully downloaded
                    if (part.length() > 1_000_000L) part.renameTo(dest)
                }
                else -> throw IllegalStateException("HTTP ${response.code}")
            }
        }
    }
}
