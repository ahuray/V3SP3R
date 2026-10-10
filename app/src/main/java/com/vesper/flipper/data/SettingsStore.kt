package com.vesper.flipper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.vesper.flipper.ai.LlmProvider
import com.vesper.flipper.ai.LlmSelection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vesper_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val PROVIDER = stringPreferencesKey("llm_provider")

    val selectedProvider: Flow<LlmProvider> = context.dataStore.data.map { preferences ->
        LlmProvider.fromId(preferences[PROVIDER])
    }

    suspend fun setProvider(provider: LlmProvider) {
        context.dataStore.edit { preferences ->
            preferences[PROVIDER] = provider.id
        }
    }

    /**
     * API key for the active provider. The OpenRouter key stays in
     * "openrouter_api_key" so existing installs keep working.
     */
    val apiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        val provider = LlmProvider.fromId(preferences[PROVIDER])
        preferences[stringPreferencesKey(provider.apiKeyPreference)]?.takeIf { it.isNotBlank() }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { preferences ->
            val provider = LlmProvider.fromId(preferences[PROVIDER])
            val preference = stringPreferencesKey(provider.apiKeyPreference)
            val trimmed = key.trim()
            if (trimmed.isEmpty()) {
                preferences.remove(preference)
            } else {
                preferences[preference] = trimmed
            }
        }
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        val provider = LlmProvider.fromId(preferences[PROVIDER])
        preferences[stringPreferencesKey(provider.modelPreference)] ?: provider.defaultModel
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            val provider = LlmProvider.fromId(preferences[PROVIDER])
            preferences[stringPreferencesKey(provider.modelPreference)] = model
        }
    }

    suspend fun readLlmSelection(): LlmSelection {
        val preferences = context.dataStore.data.first()
        val provider = LlmProvider.fromId(preferences[PROVIDER])
        val apiKey = preferences[stringPreferencesKey(provider.apiKeyPreference)]?.takeIf { it.isNotBlank() }
        val model = preferences[stringPreferencesKey(provider.modelPreference)] ?: provider.defaultModel
        return LlmSelection(provider, apiKey, model)
    }

    // Last Connected Device
    private val LAST_DEVICE_ADDRESS = stringPreferencesKey("last_device_address")
    private val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
    private val LAST_CHAT_SESSION_ID = stringPreferencesKey("last_chat_session_id")

    val lastDeviceAddress: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_DEVICE_ADDRESS]
    }

    val lastDeviceName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_DEVICE_NAME]
    }

    suspend fun setLastDevice(address: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_DEVICE_ADDRESS] = address
            preferences[LAST_DEVICE_NAME] = name
        }
    }

    val lastChatSessionId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_CHAT_SESSION_ID]
    }

    suspend fun setLastChatSessionId(sessionId: String?) {
        context.dataStore.edit { preferences ->
            if (sessionId.isNullOrBlank()) {
                preferences.remove(LAST_CHAT_SESSION_ID)
            } else {
                preferences[LAST_CHAT_SESSION_ID] = sessionId
            }
        }
    }

    // Auto-connect setting
    private val AUTO_CONNECT = booleanPreferencesKey("auto_connect")

    val autoConnect: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_CONNECT] ?: true
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CONNECT] = enabled
        }
    }

    // Default project path on Flipper
    private val DEFAULT_PROJECT_PATH = stringPreferencesKey("default_project_path")

    val defaultProjectPath: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_PROJECT_PATH] ?: "/ext/apps_data/vesper"
    }

    suspend fun setDefaultProjectPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PROJECT_PATH] = path
        }
    }

    // Permission auto-grant duration (milliseconds)
    private val PERMISSION_DURATION = longPreferencesKey("permission_duration")

    val permissionDuration: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PERMISSION_DURATION] ?: 15 * 60 * 1000L // 15 minutes default
    }

    suspend fun setPermissionDuration(durationMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PERMISSION_DURATION] = durationMs
        }
    }

    // Auto-approve by risk tier
    private val AUTO_APPROVE_MEDIUM = booleanPreferencesKey("auto_approve_medium")
    private val AUTO_APPROVE_HIGH = booleanPreferencesKey("auto_approve_high")

    val autoApproveMedium: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_APPROVE_MEDIUM] ?: false
    }

    suspend fun setAutoApproveMedium(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_APPROVE_MEDIUM] = enabled
        }
    }

    val autoApproveHigh: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_APPROVE_HIGH] ?: false
    }

    suspend fun setAutoApproveHigh(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_APPROVE_HIGH] = enabled
        }
    }

    // Haptic feedback
    private val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")

    val hapticFeedback: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAPTIC_FEEDBACK] ?: true
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK] = enabled
        }
    }

    // Dark mode
    private val DARK_MODE = booleanPreferencesKey("dark_mode")

    val darkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }

    // Audit log retention (days)
    private val AUDIT_RETENTION_DAYS = intPreferencesKey("audit_retention_days")

    val auditRetentionDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[AUDIT_RETENTION_DAYS] ?: 30
    }

    suspend fun setAuditRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUDIT_RETENTION_DAYS] = days
        }
    }

    // AI agent max model/tool loop iterations
    private val AI_MAX_ITERATIONS = intPreferencesKey("ai_max_iterations")

    val aiMaxIterations: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[AI_MAX_ITERATIONS] ?: DEFAULT_AI_MAX_ITERATIONS)
            .coerceIn(MIN_AI_MAX_ITERATIONS, MAX_AI_MAX_ITERATIONS)
    }

    suspend fun setAiMaxIterations(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[AI_MAX_ITERATIONS] = value.coerceIn(MIN_AI_MAX_ITERATIONS, MAX_AI_MAX_ITERATIONS)
        }
    }

    // TTS (routed through OpenRouter — no separate key needed)
    private val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    private val TTS_VOICE_ID = stringPreferencesKey("tts_voice_id")
    private val TTS_AUTO_SPEAK = booleanPreferencesKey("tts_auto_speak")

    val ttsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TTS_ENABLED] ?: false
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TTS_ENABLED] = enabled
        }
    }

    val ttsVoiceId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TTS_VOICE_ID] ?: DEFAULT_TTS_VOICE
    }

    suspend fun setTtsVoiceId(voiceId: String) {
        context.dataStore.edit { preferences ->
            preferences[TTS_VOICE_ID] = voiceId
        }
    }

    val ttsAutoSpeak: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TTS_AUTO_SPEAK] ?: false
    }

    suspend fun setTtsAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TTS_AUTO_SPEAK] = enabled
        }
    }

    // Smart Glasses Bridge
    private val GLASSES_ENABLED = booleanPreferencesKey("glasses_enabled")
    private val GLASSES_BRIDGE_URL = stringPreferencesKey("glasses_bridge_url")
    private val GLASSES_AUTO_SEND = booleanPreferencesKey("glasses_auto_send")
    private val GLASSES_AUTO_CONNECT = booleanPreferencesKey("glasses_auto_connect")
    private val GLASSES_SAILOR_MOUTH = booleanPreferencesKey("glasses_sailor_mouth")
    private val GLASSES_MUTED = booleanPreferencesKey("glasses_muted")

    val glassesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_ENABLED] ?: false
    }

    suspend fun setGlassesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSES_ENABLED] = enabled
        }
    }

    val glassesBridgeUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_BRIDGE_URL]
    }

    suspend fun setGlassesBridgeUrl(url: String?) {
        context.dataStore.edit { preferences ->
            if (url.isNullOrBlank()) {
                preferences.remove(GLASSES_BRIDGE_URL)
            } else {
                preferences[GLASSES_BRIDGE_URL] = url
            }
        }
    }

    val glassesAutoSend: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_AUTO_SEND] ?: true
    }

    suspend fun setGlassesAutoSend(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSES_AUTO_SEND] = enabled
        }
    }

    val glassesAutoConnect: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_AUTO_CONNECT] ?: false
    }

    suspend fun setGlassesAutoConnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSES_AUTO_CONNECT] = enabled
        }
    }

    val glassesSailorMouth: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_SAILOR_MOUTH] ?: false
    }

    suspend fun setGlassesSailorMouth(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSES_SAILOR_MOUTH] = enabled
        }
    }

    val glassesMuted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_MUTED] ?: false
    }

    suspend fun setGlassesMuted(muted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSES_MUTED] = muted
        }
    }

    companion object {
        val DEFAULT_MODEL: String = LlmProvider.OPENROUTER.defaultModel
        const val DEFAULT_TTS_VOICE = "shimmer"
        const val DEFAULT_AI_MAX_ITERATIONS = 10
        const val MIN_AI_MAX_ITERATIONS = 4
        const val MAX_AI_MAX_ITERATIONS = 20

        val FALLBACK_MODELS: List<ModelInfo> = LlmProvider.OPENROUTER.models

        fun getModelDisplayName(
            modelId: String,
            availableModels: List<ModelInfo> = FALLBACK_MODELS
        ): String {
            return availableModels.find { it.id == modelId }?.displayName ?: modelId
        }
    }
}

/**
 * Model information for display in settings
 */
data class ModelInfo(
    val id: String,           // OpenRouter model ID
    val displayName: String,  // User-friendly name
    val description: String   // Short description
)
