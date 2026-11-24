package at.techbee.spectacled.screens.core.data

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.LanguageOption
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.flow.Flow
import java.util.Locale

actual class PlatformUserAppPreferencesStore(
    actual override val variant: SpectacledVariant
): UserAppPreferencesStore {

    private val ksafe = KSafe(APP_PREFERENCES_FILE_NAME)

    // The JVM has no per-app language kept by the system, so the stored preference is the source
    // of truth. It is applied as the JVM default locale, which the string resources and the date
    // formatting read.
    // The default locale before any override: what "Default (System)" goes back to
    private val systemLocale: Locale = Locale.getDefault()

    init {
        applyLocale(languageOption)   // the stored choice, at start
    }

    override var languageOption: LanguageOption
        get() = super.languageOption
        set(value) {
            applyLocale(value)   // before saving: the save makes AppTheme rebuild the UI
            super.languageOption = value
        }

    private fun applyLocale(option: LanguageOption) =
        Locale.setDefault(option.tag?.let { Locale.forLanguageTag(it) } ?: systemLocale)

    actual override fun save(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Plain)
    actual override fun saveEncrypted(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Encrypted())
    actual override fun load(key: String): String? = ksafe.getDirect(key, null)
    actual override fun loadAsFlow(key: String): Flow<String?> = ksafe.getFlow(key, null)
    actual override fun remove(key: String) = ksafe.deleteDirect(key)
}
