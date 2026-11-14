package at.techbee.spectacled.theme

import android.app.Application
import android.app.LocaleManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import at.techbee.spectacled.screens.core.data.LANGUAGE_OPTION
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.widget.SpectacledWidget
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.Locale

/**
 * Applies the user's [LanguageOption] to the whole process, so that Compose, widgets,
 * shortcuts and notifications all use the same language.
 *
 * - Android 13+: the system per-app language (LocaleManager) is the source of truth. It can
 *   also be changed in the system settings, the stored preference mirrors it.
 * - Below Android 13: the stored preference is the source of truth, it is applied as the
 *   default locale (re-applied after every configuration change, as the framework resets it)
 *   and at the root of the composition (see ProvideAppLanguage).
 */
object AppLanguage : KoinComponent {

    private var lastLanguage: String? = null

    /** Call once from Application.onCreate after Koin has been started. */
    fun init(application: Application) {
        val store = get<PlatformUserAppPreferencesStore>()

        syncWithSystem(application, store)
        lastLanguage = Locale.getDefault().language

        application.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                syncWithSystem(application, store)
                refreshIfLanguageChanged(application)
            }
            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = Unit
        })
    }

    /** Applies a newly selected [option]. */
    fun apply(context: Context, option: LanguageOption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val locales = option.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
            if (localeManager.applicationLocales != locales)
                localeManager.applicationLocales = locales   // triggers onConfigurationChanged
        } else {
            applyDefaultLocale(option)
            refreshIfLanguageChanged(context)
        }
    }

    private fun syncWithSystem(context: Context, store: PlatformUserAppPreferencesStore) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Mirror the system per-app language, it may have been changed in the system settings.
            val fromSystem = fromSystem(context)
            if (fromSystem != store.languageOption)
                store.save(LANGUAGE_OPTION, fromSystem.name)   // not the setter, that would apply it again
        } else {
            // The framework resets the default locale on configuration changes.
            applyDefaultLocale(store.languageOption)
        }
    }

    internal fun localeFor(option: LanguageOption): Locale =
        option.tag?.let { Locale.forLanguageTag(it) } ?: Resources.getSystem().configuration.locales[0]

    internal fun applyDefaultLocale(option: LanguageOption) {
        val locale = localeFor(option)
        if (Locale.getDefault() != locale)
            Locale.setDefault(locale)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun fromSystem(context: Context): LanguageOption {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        if (locales.isEmpty) return LanguageOption.SYSTEM
        val language = locales[0].language
        return LanguageOption.entries.find { it.tag == language } ?: LanguageOption.SYSTEM
    }

    /** Strings outside of Compose (widgets) are only re-read on their next update. */
    private fun refreshIfLanguageChanged(context: Context) {
        val language = Locale.getDefault().language
        if (language == lastLanguage) return
        lastLanguage = language
        SpectacledWidget.updateAll(context.applicationContext)
    }
}
