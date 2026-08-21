package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** AI brain configuration (survives restarts). The brain is Gemma 4 E4B on-device. */
data class AiSettings(
    val gemmaModelUrl: String = DEFAULT_GEMMA_MODEL_URL,
    val serverPort: Int = 8080,
    val serverToken: String = "",
    val serverApiKey: String = "",
    val cloudEnabled: Boolean = false,
    val cloudEndpoint: String = "https://api.groq.com/openai/v1/chat/completions",
    val cloudApiKey: String = "",
    val cloudModel: String = "llama-3.3-70b-versatile"
) {
    companion object {
        /** Official, publicly downloadable Gemma 4 E4B (LiteRT-LM format). */
        const val DEFAULT_GEMMA_MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm"
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

    private val _aiSettings = MutableStateFlow(loadAi())
    val aiSettings: StateFlow<AiSettings> = _aiSettings.asStateFlow()

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _themeId = MutableStateFlow(prefs.getString("theme_id", "purple") ?: "purple")
    val themeId: StateFlow<String> = _themeId.asStateFlow()

    private val _chatDraft = MutableStateFlow(prefs.getString("chat_draft", "") ?: "")
    val chatDraft: StateFlow<String> = _chatDraft.asStateFlow()

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

    /** Last unsent text in the chat composer — survives process death and app close. */
    fun saveChatDraft(text: String) {
        prefs.edit().putString("chat_draft", text).apply()
        _chatDraft.value = text
    }

    fun loadLastTab(): String = prefs.getString("last_tab", "") ?: ""

    fun saveLastTab(tab: String) {
        prefs.edit().putString("last_tab", tab).apply()
    }

    private fun loadAi(): AiSettings = AiSettings(
        gemmaModelUrl = prefs.getString("gemma_model_url", AiSettings.DEFAULT_GEMMA_MODEL_URL)
            ?: AiSettings.DEFAULT_GEMMA_MODEL_URL,
        serverPort = prefs.getInt("server_port", 8080),
        serverToken = prefs.getString("server_token", "") ?: "",
        serverApiKey = prefs.getString("server_api_key", "") ?: "",
        cloudEnabled = prefs.getBoolean("cloud_enabled", false),
        cloudEndpoint = prefs.getString("cloud_endpoint", "https://api.groq.com/openai/v1/chat/completions")
            ?: "https://api.groq.com/openai/v1/chat/completions",
        cloudApiKey = prefs.getString("cloud_api_key", "") ?: "",
        cloudModel = prefs.getString("cloud_model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile"
    )

    private fun loadProfile(): UserProfile = UserProfile(
        name = prefs.getString("user_name", "User") ?: "User",
        currency = "INR",
        welcomeVoice = prefs.getBoolean("welcome_voice", true),
        smsTracking = prefs.getBoolean("sms_tracking", false),
        panNumber = prefs.getString("pan_number", "") ?: ""
    )
}
