package com.example.domain.brain

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device Gemma hook.
 *
 * LiteRT-LM ships 4 KB-aligned native .so files. Android 15+ phones that use
 * 16 KB memory pages **refuse to install** any APK that contains those
 * libraries (INSTALL_FAILED_INVALID_APK / "App not installed").
 *
 * This class therefore does **not** load LiteRT. Accurate answers come from
 * the Cloud Brain (Groq / Gemini / OpenRouter) plus the on-device number
 * engine. The optional .litertlm file can still be stored on the 300 GB of
 * free space for a future 16 KB-aligned runtime.
 */
class GemmaBrainEngine(private val context: Context) {

    fun modelFile(): File = File(context.filesDir, "models/gemma-4-E4B-it.litertlm")

    fun isModelAvailable(): Boolean = false

    fun modelSizeLabel(): String = "Cloud Brain (install-safe)"

    suspend fun preload() = withContext(Dispatchers.IO) { /* no native engine */ }

    suspend fun generate(@Suppress("UNUSED_PARAMETER") prompt: String): String? = null

    fun close() {}
}
