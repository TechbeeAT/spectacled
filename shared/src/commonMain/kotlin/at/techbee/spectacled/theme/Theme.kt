package at.techbee.spectacled.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.theme_dark
import spectacled.shared.generated.resources.theme_light
import spectacled.shared.generated.resources.theme_system

enum class ThemeOption(
    val stringRes: StringResource
) {
    SYSTEM(Res.string.theme_system),
    LIGHT(Res.string.theme_light),
    DARK(Res.string.theme_dark);

    companion object {
        fun fromString(value: String?) = entries.find { it.name == value } ?: SYSTEM
    }
}


@Composable
fun AppTheme(
    spectacledVariant: SpectacledVariant,
    content: @Composable () -> Unit
) {

    // Simplify to make previews work
    if(LocalInspectionMode.current) {
        MaterialTheme(
            colorScheme = getColorSchemeForSeedColor(spectacledVariant.themeSeedColor),
            typography = DefaultAppTypography,
            content = content
        )
    } else {

        val userAppPreferencesStore = koinInject<PlatformUserAppPreferencesStore>()
        val themeOption by userAppPreferencesStore.getThemeOptionAsFlow().collectAsState(userAppPreferencesStore.themeOption)
        val themeDynamicColorsEnabled by userAppPreferencesStore.getThemeDynamicColorsEnabledAsFlow().collectAsState(userAppPreferencesStore.themeDynamicColorsEnabled)

        val currentColorScheme = if (themeDynamicColorsEnabled) {    // Android only
            getAndroidDynamicColorScheme(themeOption) ?: getColorSchemeForSeedColor(spectacledVariant.themeSeedColor)
        } else {
            getColorSchemeForSeedColor(spectacledVariant.themeSeedColor)
        }

        val currentFont by userAppPreferencesStore.getThemeFontAsFlow().collectAsState(userAppPreferencesStore.themeFont)

        MaterialTheme(
            colorScheme = currentColorScheme,
            typography = currentFont.getTypography(),
            content = content
        )
    }
}

@Composable
expect fun getAndroidDynamicColorScheme(themeOption: ThemeOption): ColorScheme?

