package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** AI brain configuration (survives restarts). The brain is Gemma 4 E2B (fast) on-device. */
data class AiSettings(
    val gemmaModelUrl: String = DEFAULT_GEMMA_MODEL_URL,
    val serverPort: Int = 8080,
    val serverToken: String = "",
    val serverApiKey: String = "",
    val cloudEnabled: Boolean = false,
    val cloudEndpoint: String = "https://api.openai.com/v1/chat/completions",
    val cloudApiKey: String = "",
    val cloudModel: String = "gpt-4o-mini"
) {
    companion object {
        /** Official, publicly downloadable Gemma 4 E2B (fast) (LiteRT-LM format). */
        const val DEFAULT_GEMMA_MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    }
}

data class UserProfile(
    val name: String = "User",
    val currency: String = "INR",
    val welcomeVoice: Boolean = true,
    val smsTracking: Boolean = false,
    val panNumber: String = ""
)

/**
 * Lightweight preferences store (SharedPreferences) for the user profile, the
 * on-device brain connection, and the app theme. Data-heavy finance records
 * live in Room.
 */
class AppPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("dhanom_prefs", Context.MODE_PRIVATE)

    /** The memory summary is stored in a FILE (not SharedPreferences) so it can
     *  grow for the lifetime of the app — it is the brain's full, unbounded
     *  rolling memory. SharedPreferences is only kept as a migration fallback. */
    private val memorySummaryFile: File =
        File(context.applicationContext.filesDir, "memory_summary.txt")

    private val _aiSettings = MutableStateFlow(loadAi())
    val aiSettings: StateFlow<AiSettings> = _aiSettings.asStateFlow()

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _themeId = MutableStateFlow(prefs.getString("theme_id", "purple") ?: "purple")
    val themeId: StateFlow<String> = _themeId.asStateFlow()

    private val _committedPrompt = MutableStateFlow(prefs.getString("committed_prompt", "") ?: "")
    val committedPrompt: StateFlow<String> = _committedPrompt.asStateFlow()

    private val _memorySummary = MutableStateFlow(loadMemorySummary())
    val memorySummary: StateFlow<String> = _memorySummary.asStateFlow()

    private val _profilePhotoPath = MutableStateFlow(prefs.getString("profile_photo_path", "") ?: "")
    val profilePhotoPath: StateFlow<String> = _profilePhotoPath.asStateFlow()

    fun saveCommittedPrompt(prompt: String) {
        prefs.edit().putString("committed_prompt", prompt.trim()).apply()
        _committedPrompt.value = prompt.trim()
    }

    fun saveMemorySummary(summary: String) {
        val trimmed = summary.trim()
        // Write the FULL summary to disk (unbounded), keep a truncated mirror in
        // prefs for older backups/imports to still work.
        try {
            memorySummaryFile.writeText(trimmed)
        } catch (_: Exception) {
        }
        prefs.edit().putString("memory_summary", trimmed.take(2000)).apply()
        _memorySummary.value = trimmed
    }

    private fun loadMemorySummary(): String {
        // Prefer the file (full, unbounded); fall back to the old prefs value.
        return try {
            val fileText = memorySummaryFile.takeIf { it.exists() }?.readText()?.trim()
            if (!fileText.isNullOrBlank()) fileText
            else prefs.getString("memory_summary", "")?.trim() ?: ""
        } catch (_: Exception) {
            prefs.getString("memory_summary", "")?.trim() ?: ""
        }
    }

    fun saveAi(settings: AiSettings) {
        prefs.edit()
            .putString("gemma_model_url", settings.gemmaModelUrl)
            .putInt("server_port", settings.serverPort)
            .putString("server_token", settings.serverToken)
            .putString("server_api_key", settings.serverApiKey)
            .putBoolean("cloud_enabled", settings.cloudEnabled)
            .putString("cloud_endpoint", settings.cloudEndpoint)
            .putString("cloud_api_key", settings.cloudApiKey)
            .putString("cloud_model", settings.cloudModel)
            .apply()
        _aiSettings.value = settings
    }

    fun saveThemeId(id: String) {
        prefs.edit().putString("theme_id", id).apply()
        _themeId.value = id
    }

    fun saveProfileName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        _profile.value = _profile.value.copy(name = name)
    }

    fun saveProfilePhotoPath(path: String) {
        prefs.edit().putString("profile_photo_path", path.trim()).apply()
        _profilePhotoPath.value = path.trim()
    }

    fun saveWelcomeVoice(enabled: Boolean) {
        prefs.edit().putBoolean("welcome_voice", enabled).apply()
        _profile.value = _profile.value.copy(welcomeVoice = enabled)
    }

    fun saveSmsTracking(enabled: Boolean) {
        prefs.edit().putBoolean("sms_tracking", enabled).apply()
        _profile.value = _profile.value.copy(smsTracking = enabled)
    }

    fun savePanNumber(pan: String) {
        prefs.edit().putString("pan_number", pan).apply()
        _profile.value = _profile.value.copy(panNumber = pan)
    }

    private fun loadAi(): AiSettings = AiSettings(
        gemmaModelUrl = prefs.getString("gemma_model_url", AiSettings.DEFAULT_GEMMA_MODEL_URL)
            ?: AiSettings.DEFAULT_GEMMA_MODEL_URL,
        serverPort = prefs.getInt("server_port", 8080),
        serverToken = prefs.getString("server_token", "") ?: "",
        serverApiKey = prefs.getString("server_api_key", "") ?: "",
        cloudEnabled = prefs.getBoolean("cloud_enabled", false),
        cloudEndpoint = prefs.getString("cloud_endpoint", "https://api.openai.com/v1/chat/completions")
            ?: "https://api.openai.com/v1/chat/completions",
        cloudApiKey = prefs.getString("cloud_api_key", "") ?: "",
        cloudModel = prefs.getString("cloud_model", "gpt-4o-mini") ?: "gpt-4o-mini"
    )

    private fun loadProfile(): UserProfile = UserProfile(
        name = prefs.getString("user_name", "User") ?: "User",
        currency = "INR",
        welcomeVoice = prefs.getBoolean("welcome_voice", true),
        smsTracking = prefs.getBoolean("sms_tracking", false),
        panNumber = prefs.getString("pan_number", "") ?: ""
    )
}
