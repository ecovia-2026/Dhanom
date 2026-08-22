package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Speaks the welcome line on launch with a natural, warm voice.
 * Picks the best available English(India) voice, falls back to the default,
 * and tunes pitch/rate for a smoother, more human tone.
 */
class WelcomeVoice(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val t = tts ?: return
        // Prefer a natural en-IN voice when available.
        runCatching {
            val voices = t.voices ?: emptySet()
            val best = voices.firstOrNull { it.locale?.language == "en" && it.locale?.country == "IN" && !it.isNetworkConnectionRequired }
                ?: voices.firstOrNull { it.locale?.language == "en" && !it.isNetworkConnectionRequired }
            if (best != null) t.voice = best
        }
        t.language = Locale.forLanguageTag("en-IN")
        t.setSpeechRate(0.9f)
        t.setPitch(1.05f)
        t.speak(
            "Welcome to Dhan Om, your personal AI.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "dhanom-welcome"
        )
    }

    fun shutdown() {
        try { tts?.stop() } catch (_: Throwable) {}
        try { tts?.shutdown() } catch (_: Throwable) {}
    }
}
