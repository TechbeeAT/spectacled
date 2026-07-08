package at.techbee.spectacled.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import org.koin.compose.koinInject


@Composable
fun getColorSchemeForSeedColor(color: Color?): ColorScheme {

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

    return if (color == null)
        MaterialTheme.colorScheme
    else
        dynamicColorScheme(
            primary = color,
            isDark = overrideIsDark ?: isSystemInDarkTheme(),
            style = themePaletteStyle,
            isAmoled = themeAmoledBoolean,
            specVersion = ColorSpec.SpecVersion.SPEC_2025
        )
}
