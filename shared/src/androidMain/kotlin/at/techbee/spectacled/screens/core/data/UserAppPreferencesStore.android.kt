package at.techbee.spectacled.screens.core.data

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.LanguageOption
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


actual class PlatformUserAppPreferencesStore(
    context: Context,
    actual override val variant: SpectacledVariant
): UserAppPreferencesStore {

    private val appContext = context.applicationContext
    private val ksafe = KSafe(appContext, APP_PREFERENCES_FILE_NAME)

    // Android 13+: the system's per-app language is the only source of truth. It applies the
    // language to the whole process and can also be changed in the system settings.
    private val localeManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) appContext.getSystemService(LocaleManager::class.java) else null

    override val languageSelectionSupported: Boolean get() = localeManager != null

    override var languageOption: LanguageOption
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                localeManager?.applicationLocales?.get(0)?.language?.let { language ->
                    return LanguageOption.entries.find { it.tag == language } ?: LanguageOption.SYSTEM
                }

            return LanguageOption.SYSTEM
        }
        set(value) {
            // An empty list means "follow the system"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                localeManager?.applicationLocales = LocaleList.forLanguageTags(value.tag.orEmpty())
        }

    // Cold flow, so that every collection reads the current system value
    override fun getLanguageOptionAsFlow(): Flow<LanguageOption> = flow { emit(languageOption) }

    actual override fun save(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Plain)
    actual override fun saveEncrypted(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Encrypted())
    actual override fun load(key: String): String? = ksafe.getDirect(key, null)
    actual override fun loadAsFlow(key: String): Flow<String?> = ksafe.getFlow(key, null)
    actual override fun remove(key: String) = ksafe.deleteDirect(key)
}
