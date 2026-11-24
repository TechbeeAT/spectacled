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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.Locale


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

    // Bumped by the setter, so that a selection is emitted even if it causes no configuration change
    private val languageChanged = MutableStateFlow(0)

    override val languageSelectionSupported: Boolean get() = localeManager != null

    override var languageOption: LanguageOption
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                localeManager?.applicationLocales?.get(0)?.let { locale ->
                    // Full tag first (e.g. pt-BR), then the language only (e.g. de for de-AT)
                    return LanguageOption.entries.find { it.tag == locale.toLanguageTag() }
                        ?: LanguageOption.entries.find { it.tag == locale.language }
                        ?: LanguageOption.SYSTEM
                }

            return LanguageOption.SYSTEM
        }
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                localeManager?.let { localeManager ->
                    val locales = value.tag?.let { tag ->
                        val chosen = Locale.forLanguageTag(tag)
                        // Keep the region of the system, so that dates and times stay in the user's
                        // format (English on an Austrian device: en-AT, not the US formats of a bare "en")
                        val systemRegion = localeManager.systemLocales.get(0)?.country.orEmpty()
                        if (chosen.country.isEmpty() && systemRegion.isNotEmpty())
                            LocaleList(Locale.Builder().setLocale(chosen).setRegion(systemRegion).build())
                        else
                            LocaleList(chosen)
                    } ?: LocaleList.getEmptyLocaleList()   // an empty list means "follow the system"

                    localeManager.applicationLocales = locales
                }
            languageChanged.update { it + 1 }
        }

    // Every collection and every selection reads the current system value
    override fun getLanguageOptionAsFlow(): Flow<LanguageOption> = languageChanged.map { languageOption }

    actual override fun save(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Plain)
    actual override fun saveEncrypted(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Encrypted())
    actual override fun load(key: String): String? = ksafe.getDirect(key, null)
    actual override fun loadAsFlow(key: String): Flow<String?> = ksafe.getFlow(key, null)
    actual override fun remove(key: String) = ksafe.deleteDirect(key)
}
