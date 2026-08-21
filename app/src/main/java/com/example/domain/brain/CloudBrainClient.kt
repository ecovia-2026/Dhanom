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
        userMessage: String
    ): String? {
        return try {
            val payload = JSONObject()
                .put("model", model)
                .put("temperature", 0.3)
                .put("max_tokens", 700)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", userMessage)))

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
                val content = choices.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                if (content.isNullOrBlank()) null else content.trim()
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** Ready-made provider presets (endpoint + model). Free-tier friendly. */
        val presets = listOf(
            Triple("Groq · Llama 3.3 70B (free, fastest)", "https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile"),
            Triple("Google · Gemini 2.5 Flash (free)", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "gemini-2.5-flash"),
            Triple("OpenRouter · Llama 3.3 70B :free", "https://openrouter.ai/api/v1/chat/completions", "meta-llama/llama-3.3-70b-instruct:free"),
            Triple("OpenRouter · Gemini Flash :free", "https://openrouter.ai/api/v1/chat/completions", "google/gemini-2.0-flash-exp:free"),
            Triple("OpenRouter · DeepSeek R1 :free", "https://openrouter.ai/api/v1/chat/completions", "deepseek/deepseek-r1:free"),
            Triple("Mistral · free tier", "https://api.mistral.ai/v1/chat/completions", "mistral-small-latest"),
            Triple("DeepSeek · V3 (cheap)", "https://api.deepseek.com/chat/completions", "deepseek-chat"),
            Triple("OpenAI · GPT-4o mini", "https://api.openai.com/v1/chat/completions", "gpt-4o-mini"),
            Triple("OpenAI · GPT-4o", "https://api.openai.com/v1/chat/completions", "gpt-4o")
        )
    }
}
