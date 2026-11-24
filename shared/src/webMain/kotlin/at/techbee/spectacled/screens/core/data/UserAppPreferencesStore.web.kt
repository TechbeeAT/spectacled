package at.techbee.spectacled.screens.core.data

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.LanguageOption
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import eu.anifantakis.lib.ksafe.awaitCacheReady
import kotlinx.coroutines.flow.Flow

/**
 * Makes the browser report [tag] as its language (null = the real browser language again).
 * The string resources and the date formatting both ask the browser (navigator.languages /
 * navigator.language), and the browser offers no way to set that per page, so the two getters are
 * replaced once and consult the override first.
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(tag) => {
        const g = globalThis;
        if (!g.__spectacledLanguagePatched) {
            const proto = Navigator.prototype;
            const language = Object.getOwnPropertyDescriptor(proto, 'language');
            const languages = Object.getOwnPropertyDescriptor(proto, 'languages');
            Object.defineProperty(proto, 'language', Object.assign({}, language, {
                get: function () { return g.__spectacledLanguage || language.get.call(this); }
            }));
            Object.defineProperty(proto, 'languages', Object.assign({}, languages, {
                get: function () { return g.__spectacledLanguage ? [g.__spectacledLanguage] : languages.get.call(this); }
            }));
            g.__spectacledLanguagePatched = true;
        }
        g.__spectacledLanguage = tag;
    }"""
)
private external fun overrideBrowserLanguage(tag: String?)

actual class PlatformUserAppPreferencesStore(
    actual override val variant: SpectacledVariant
): UserAppPreferencesStore {

    private val ksafe = KSafe(APP_PREFERENCES_FILE_NAME)

    actual override fun save(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Plain)
    actual override fun saveEncrypted(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Encrypted())
    actual override fun load(key: String): String? = ksafe.getDirect(key, null)
    actual override fun loadAsFlow(key: String): Flow<String?> = ksafe.getFlow(key, null)
    actual override fun remove(key: String) = ksafe.deleteDirect(key)

    // The browser has no per-site language setting, so the stored preference is the source of truth.
    override val languageSelectionSupported: Boolean get() = true

    override var languageOption: LanguageOption
        get() = super.languageOption
        set(value) {
            overrideBrowserLanguage(value.tag)   // before saving: the save makes AppTheme rebuild the UI
            super.languageOption = value
        }

    override suspend fun awaitReady() {
        ksafe.awaitCacheReady()
        // The stored choice can only be read now. SecureStorageReadyGate holds the UI back until then.
        overrideBrowserLanguage(languageOption.tag)
    }
}
