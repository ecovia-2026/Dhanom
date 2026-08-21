package com.example.domain.brain

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device Gemma 4 E4B brain via LiteRT-LM.
 * Tries the GPU backend first (fast), falls back to CPU (compatible).
 */
class GemmaBrainEngine(private val context: Context) {

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    fun modelFile(): File = File(context.filesDir, "models/gemma-4-E4B-it.litertlm")

    fun isModelAvailable(): Boolean = modelFile().exists() && modelFile().length() > 10_000_000L

    fun modelSizeLabel(): String {
        val f = modelFile()
        return if (f.exists()) "%.0f MB".format(f.length() / 1_000_000.0) else "not installed"
    }

    /** Loads the model once in the background (call at startup to remove first-reply lag). */
    suspend fun preload() = withContext(Dispatchers.IO) {
        try { ensureConversation() } catch (_: Throwable) {}
    }

    /** Generates a reply. Returns null if unavailable. */
    suspend fun generate(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val conv = ensureConversation() ?: return@withContext null
            conv.sendMessage(prompt).toString().trim().ifBlank { null }
        } catch (e: Throwable) {
            null
        }
    }

    fun close() {
        try { conversation?.close() } catch (_: Throwable) {}
        try { engine?.close() } catch (_: Throwable) {}
        conversation = null
        engine = null
    }

    private fun ensureConversation(): Conversation? {
        if (!isModelAvailable()) return null
        if (conversation == null) {
            if (engine == null) {
                engine = createEngine(preferGpu = true)
                if (engine == null) engine = createEngine(preferGpu = false)
                if (engine == null) return null
            }
            conversation = engine!!.createConversation(
                ConversationConfig(systemInstruction = Contents.of(listOf(Content.Text(SYSTEM_PROMPT))))
            )
        }
        return conversation
    }

    private fun createEngine(preferGpu: Boolean): Engine? {
        return try {
            val backend = if (preferGpu) Backend.GPU() else Backend.CPU()
            val config = EngineConfig(
                modelPath = modelFile().absolutePath,
                backend = backend,
                // Keep the compiled cache in filesDir so Android does not evict
                // the ~2 GB JIT/KV cache from cacheDir (that is what made replies
                // slow even with 16 GB RAM — the model was being recompiled).
                cacheDir = File(context.filesDir, "gemma_cache").apply { mkdirs() }.path
            )
            Engine(config).also { it.initialize() }
        } catch (e: Throwable) {
            null
        }
    }

    companion object {
        val SYSTEM_PROMPT =
            "You are Dhan-OM, a concise personal finance AI. Reply in the user's language " +
            "(Hindi, Hinglish, Marathi, Tamil, English, or mixed). 2-4 short sentences using ₹. " +
            "Be accurate. Understand Indian numbers (lakh, crore). Never invent balances."
    }
}
