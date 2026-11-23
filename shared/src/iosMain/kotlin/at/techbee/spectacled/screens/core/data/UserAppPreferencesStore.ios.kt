package at.techbee.spectacled.screens.core.data

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.alarm.IosNotifications
import at.techbee.spectacled.theme.LanguageOption
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

private const val APPLE_LANGUAGES = "AppleLanguages"

actual class PlatformUserAppPreferencesStore(
    actual override val variant: SpectacledVariant
): UserAppPreferencesStore {

    private val ksafe = KSafe(APP_PREFERENCES_FILE_NAME)

    // The per-app language list in the app's own user defaults is the only source of truth. iOS
    // reads it for the whole app and also writes it from the app's page in the Settings app.
    private val userDefaults = NSUserDefaults.standardUserDefaults

    // Bumped by the setter: on iOS nothing else tells the UI that the language has changed
    private val languageChanged = MutableStateFlow(0)

    override val languageSelectionSupported: Boolean get() = true

    override var languageOption: LanguageOption
        get() {
            // Only the app's own domain: standardUserDefaults would fall back to the system-wide list
            val appDefaults = NSBundle.mainBundle.bundleIdentifier?.let { userDefaults.persistentDomainForName(it) }
            val tag = (appDefaults?.get(APPLE_LANGUAGES) as? List<*>)?.firstOrNull() as? String
                ?: return LanguageOption.SYSTEM

            // Full tag first (e.g. pt-BR), then the language only (e.g. de for de-AT)
            return LanguageOption.entries.find { it.tag == tag }
                ?: LanguageOption.entries.find { it.tag == tag.substringBefore('-') }
                ?: LanguageOption.SYSTEM
        }
        set(value) {
            if (value.tag == null)
                userDefaults.removeObjectForKey(APPLE_LANGUAGES)
            else
                userDefaults.setObject(listOf(value.tag), APPLE_LANGUAGES)
            languageChanged.update { it + 1 }
            IosNotifications.reloadTexts()   // otherwise reminders keep the old language until the next start
        }

    override fun getLanguageOptionAsFlow(): Flow<LanguageOption> = languageChanged.map { languageOption }

    actual override fun save(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Plain)
    actual override fun saveEncrypted(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Encrypted())
    actual override fun load(key: String): String? = ksafe.getDirect(key, null)
    actual override fun loadAsFlow(key: String): Flow<String?> = ksafe.getFlow(key, null)
    actual override fun remove(key: String) = ksafe.deleteDirect(key)
}
