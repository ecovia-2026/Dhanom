package com.example.domain.brain

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible cloud LLM client — the "more capable backend brain".
 * Works with OpenAI, Groq, OpenRouter, DeepSeek, Google Gemini (OpenAI-compat
 * endpoint), or any /v1/chat/completions provider. Used for high-accuracy
 * financial reasoning; the on-device Gemma model remains the offline fallback.
 */
class CloudBrainClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun generate(
        endpoint: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userMessage: String,
        imageBase64: String? = null,
        imageMime: String = "image/jpeg"
    ): String? {
        return try {
            val userContent = if (imageBase64 != null) {
                // Vision: send text + image as a multimodal message (OpenRouter/OpenAI format)
                JSONArray()
                    .put(JSONObject().put("type", "text").put("text", userMessage))
                    .put(JSONObject().put("type", "image_url")
                        .put("image_url", JSONObject().put("url", "data:$imageMime;base64,$imageBase64")))
            } else {
                userMessage
            }

            val payload = JSONObject()
                .put("model", model)
                .put("temperature", 0.3)
                .put("max_tokens", 700)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", userContent)))

            val req = Request.Builder()
                .url(endpoint.trim())
                .addHeader("Authorization", "Bearer $apiKey")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                val choices = json.optJSONArray("choices") ?: return null
                val msg = choices.optJSONObject(0)?.optJSONObject("message") ?: return null
                // content may be a string or an array (multimodal reply)
                val content: String? = when (val c = msg.opt("content")) {
                    is String -> c
                    is org.json.JSONArray -> {
                        val sb = StringBuilder()
                        for (i in 0 until c.length()) {
                            val part = c.optJSONObject(i)
                            val t = part?.optString("text")
                            if (!t.isNullOrBlank()) sb.append(t)
                        }
                        sb.toString()
                    }
                    else -> null
                }
                if (content.isNullOrBlank()) null else content.trim()
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** Ready-made provider presets (endpoint + model). Free-tier friendly. */
        val presets = listOf(
            // — Free OpenRouter models (no credit card) —
            Triple("FREE · Nemotron 3 Ultra 550B (best)", "https://openrouter.ai/api/v1/chat/completions", "nvidia/nemotron-3-ultra-550b-a55b:free"),
            Triple("FREE · Qwen3 Coder (code/agent)", "https://openrouter.ai/api/v1/chat/completions", "qwen/qwen3-coder:free"),
            Triple("FREE · DeepSeek V4 Flash", "https://openrouter.ai/api/v1/chat/completions", "deepseek/deepseek-v4-flash:free"),
            Triple("FREE · Qwen3 Next 80B", "https://openrouter.ai/api/v1/chat/completions", "qwen/qwen3-next-80b-a3b-instruct:free"),
            Triple("FREE · OpenAI GPT-OSS 120B", "https://openrouter.ai/api/v1/chat/completions", "openai/gpt-oss-120b:free"),
            Triple("FREE · Gemma 4 31B (multimodal)", "https://openrouter.ai/api/v1/chat/completions", "google/gemma-4-31b-it:free"),
            Triple("FREE · Nemotron 3 Super 120B", "https://openrouter.ai/api/v1/chat/completions", "nvidia/nemotron-3-super-120b-a12b:free"),
            Triple("FREE · Llama 3.3 70B", "https://openrouter.ai/api/v1/chat/completions", "meta-llama/llama-3.3-70b-instruct:free"),
            Triple("FREE · Auto router (openrouter/free)", "https://openrouter.ai/api/v1/chat/completions", "openrouter/free"),
            // — Paid / premium (higher limits & quality) —
            Triple("PAID · Claude Sonnet 4.6 (best overall)", "https://openrouter.ai/api/v1/chat/completions", "anthropic/claude-sonnet-4.6"),
            Triple("PAID · Claude Opus 4.7", "https://openrouter.ai/api/v1/chat/completions", "anthropic/claude-opus-4-7"),
            Triple("PAID · DeepSeek V3.2 (cheap)", "https://openrouter.ai/api/v1/chat/completions", "deepseek/deepseek-v3.2"),
            Triple("PAID · Gemini 3 Flash", "https://openrouter.ai/api/v1/chat/completions", "google/gemini-3-flash-preview"),
            // — Other providers —
            Triple("Google · Gemini 2.5 Flash (free tier)", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "gemini-2.5-flash"),
            Triple("Groq · Llama 3.3 70B (free tier)", "https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile"),
            Triple("Mistral · free tier", "https://api.mistral.ai/v1/chat/completions", "mistral-small-latest"),
            Triple("DeepSeek · direct", "https://api.deepseek.com/chat/completions", "deepseek-chat")
        )
    }
}
