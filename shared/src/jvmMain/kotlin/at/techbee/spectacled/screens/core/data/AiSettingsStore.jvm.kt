package at.techbee.spectacled.screens.core.data

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode

actual class PlatformAiSettingsStore : AiSettingsStore {

    private val ksafe = KSafe(AI_SETTINGS_FILE_NAME)

    actual override suspend fun saveAnthropicApiKey(apiKey: String?) {
        if (apiKey.isNullOrBlank()) ksafe.delete(ANTHROPIC_API_KEY)
        else ksafe.put(ANTHROPIC_API_KEY, apiKey, KSafeWriteMode.Encrypted())
    }

    actual override suspend fun loadAnthropicApiKey(): String? = ksafe.get(ANTHROPIC_API_KEY, null)
}
