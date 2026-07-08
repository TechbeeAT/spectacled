package at.techbee.spectacled.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import org.koin.compose.koinInject

private data class ResolvedThemeInputs(
    val isDark: Boolean,
    val paletteStyle: PaletteStyle,
    val isAmoled: Boolean
)

@Composable
private fun rememberResolvedThemeInputs(): ResolvedThemeInputs {

    val userAppPreferencesStore =
        if(LocalInspectionMode.current)   // only to make previews work
            UserAppPreferencesStore.getEmptyPreferenceStoreForPreview()
        else
            koinInject<PlatformUserAppPreferencesStore>()

    val themeOption by userAppPreferencesStore.getThemeOptionAsFlow().collectAsState(userAppPreferencesStore.themeOption)
    val overrideIsDark = when(themeOption) {
        ThemeOption.SYSTEM -> null
        ThemeOption.LIGHT -> false
        ThemeOption.DARK -> true
    }

    val themePaletteStyle by userAppPreferencesStore.getThemePaletteStlyeAsFlow().collectAsState(userAppPreferencesStore.themePaletteStlye)
    val themeAmoledBoolean by userAppPreferencesStore.getThemeAmoledAsFlow().collectAsState(userAppPreferencesStore.themeAmoled)

    return ResolvedThemeInputs(
        isDark = overrideIsDark ?: isSystemInDarkTheme(),
        paletteStyle = themePaletteStyle,
        isAmoled = themeAmoledBoolean
    )
}

@Composable
fun getColorSchemeForSeedColor(color: Color?): ColorScheme {
    val themeInputs = rememberResolvedThemeInputs()

    return if (color == null)
        MaterialTheme.colorScheme
    else
        dynamicColorScheme(
            primary = color,
            isDark = themeInputs.isDark,
            style = themeInputs.paletteStyle,
            isAmoled = themeInputs.isAmoled,
            specVersion = ColorSpec.SpecVersion.SPEC_2025
        )
}

/**
 * Like [getColorSchemeForSeedColor], but returns a resolver instead of a single scheme.
 * Use this where many items each need a ColorScheme derived from their own seed color
 * (e.g. a lazy list of notes/tasks) - reading preferences and generating the palette happens
 * once per distinct seed color, memoized, instead of once per item per recomposition.
 */
@Composable
fun rememberColorSchemeResolver(): (Color?) -> ColorScheme {
    val themeInputs = rememberResolvedThemeInputs()
    val fallback = MaterialTheme.colorScheme

    val cache = remember(themeInputs) { mutableMapOf<Color, ColorScheme>() }

    return remember(cache, fallback) {
        { color: Color? ->
            if (color == null)
                fallback
            else
                cache.getOrPut(color) {
                    dynamicColorScheme(
                        primary = color,
                        isDark = themeInputs.isDark,
                        style = themeInputs.paletteStyle,
                        isAmoled = themeInputs.isAmoled,
                        specVersion = ColorSpec.SpecVersion.SPEC_2025
                    )
                }
        }
    }
}
