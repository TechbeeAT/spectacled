package at.techbee.spectacled.screens.core.data

const val AI_SETTINGS_FILE_NAME = "ai_settings"
const val ANTHROPIC_API_KEY = "anthropic_api_key"

interface AiSettingsStore {
    suspend fun saveAnthropicApiKey(apiKey: String?)
    suspend fun loadAnthropicApiKey(): String?
}

expect class PlatformAiSettingsStore : AiSettingsStore {
    override suspend fun saveAnthropicApiKey(apiKey: String?)
    override suspend fun loadAnthropicApiKey(): String?
}
