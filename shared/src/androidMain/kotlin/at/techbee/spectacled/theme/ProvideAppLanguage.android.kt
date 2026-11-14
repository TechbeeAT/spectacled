package at.techbee.spectacled.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun ProvideAppLanguage(languageOption: LanguageOption, content: @Composable () -> Unit) {
    // Android 13+: LocaleManager switches the whole process, the configuration change reaches
    // Compose through LocalConfiguration (MainActivity handles locale changes itself).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        content()
        return
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = AppLanguage.localeFor(languageOption)
    AppLanguage.applyDefaultLocale(languageOption)

    val localizedConfiguration = remember(configuration, locale) {
        Configuration(configuration).apply { setLocale(locale) }
    }
    val localizedContext = remember(context, localizedConfiguration) {
        context.createConfigurationContext(localizedConfiguration)
    }

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext,
    ) {
        key(languageOption) { content() }
    }
}
