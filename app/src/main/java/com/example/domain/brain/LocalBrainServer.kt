package com.example.domain.brain

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local OpenAI-compatible HTTP bridge so OTHER Android apps on this phone — and
 * other phones on the same Wi-Fi — can use Dhan-OM's on-device Gemma brain.
 *
 *   GET  /health, /
 *   GET  /v1/models
 *   POST /v1/chat/completions   (OpenAI-compatible JSON, optional Bearer token)
 *
 * Bind to 0.0.0.0 to be reachable across the local network. Runs while the app
 * process is alive (keep Dhan-OM open to serve other devices).
 */
class LocalBrainServer(
    private val port: Int,
    private val generate: suspend (String) -> String,
    private val token: String? = null
) : NanoHTTPD(port) {

    private val running = AtomicBoolean(true)

    override fun serve(session: IHTTPSession): Response {
        if (!running.get()) {
            return json(503, JSONObject().put("error", "server stopping"))
        }
        return try {
            when {
                session.uri == "/health" || session.uri == "/" ->
                    json(200, JSONObject().put("status", "ok").put("model", "gemma-4-e4b").put("framework", "dhanom"))

                session.uri == "/v1/models" || session.uri == "/models" ->
                    json(200, JSONObject().put("data", JSONArray().put(
                        JSONObject().put("id", "gemma-4-e4b").put("object", "model").put("owned_by", "dhanom")
                    )))

                session.uri == "/v1/chat/completions" || session.uri == "/chat/completions" -> handleChat(session)

                else -> json(404, JSONObject().put("error", "not found"))
            }
        } catch (e: Exception) {
            json(500, JSONObject().put("error", e.message ?: "internal error"))
        }
    }

    private fun handleChat(session: IHTTPSession): Response {
        if (!token.isNullOrBlank()) {
            val auth = header(session, "authorization")
            if (auth == null || auth.trim() != "Bearer $token") {
                return json(401, JSONObject().put("error", "invalid or missing Bearer token"))
            }
        }
        val body = readBody(session)
        if (body.isBlank()) return json(400, JSONObject().put("error", "empty body"))
        val json = JSONObject(body)
        val messages = json.optJSONArray("messages") ?: return json(400, JSONObject().put("error", "'messages' required"))
        val userText = (messages.length() - 1 downTo 0)
            .firstNotNullOfOrNull { i ->
                messages.optJSONObject(i)?.optString("content")?.takeIf { it.isNotBlank() }
            }
            ?: return json(400, JSONObject().put("error", "no message content"))

        val reply = runBlocking { generate(userText) }

        val resp = JSONObject()
            .put("id", "chatcmpl-dhanom-" + System.currentTimeMillis())
            .put("object", "chat.completion")
            .put("created", System.currentTimeMillis() / 1000)
            .put("model", "gemma-4-e4b")
            .put("choices", JSONArray().put(
                JSONObject()
                    .put("index", 0)
                    .put("message", JSONObject().put("role", "assistant").put("content", reply))
                    .put("finish_reason", "stop")
            ))
        return json(200, resp)
    }

    fun shutdown() {
        running.set(false)
        try { stop() } catch (_: Throwable) {}
    }

    private fun readBody(session: IHTTPSession): String {
        val files = HashMap<String, String>()
        return try {
            session.parseBody(files)
            files["postData"] ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun header(session: IHTTPSession, name: String): String? =
        session.headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    private fun json(code: Int, obj: JSONObject): Response =
        newFixedLengthResponse(Response.Status.lookup(code), "application/json; charset=utf-8", obj.toString())
}
