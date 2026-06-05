package at.techbee.spectacled.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import org.koin.compose.koinInject

@Composable
actual fun AppTheme(
    spectacledVariant: SpectacledVariant,
    content: @Composable (() -> Unit)
) {

    // Simplify to make previews work
    if(LocalInspectionMode.current) {
        MaterialTheme(
            colorScheme = getThemeForSeedColor(spectacledVariant.themeSeedColor),
            typography = AppTypography,
            content = content
        )
    } else {

        val userAppPreferencesStore = koinInject<PlatformUserAppPreferencesStore>()
        val themeOptionState by userAppPreferencesStore.getThemeOptionAsFlow().collectAsState(ThemeOption.SYSTEM.name)
        val themeDynamicColorsEnabledState by userAppPreferencesStore.getThemeDynamicColorsEnabledAsFlow().collectAsState(null)

        val dynamicColorsEnabled = themeDynamicColorsEnabledState?.toBooleanStrictOrNull() ?: false
        val themeOption = themeOptionState?.let { ThemeOption.entries.find { themeOption -> themeOption.name == it } } ?: ThemeOption.SYSTEM

        val colorScheme = if (dynamicColorsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            val isDark = when (themeOption) {
                ThemeOption.SYSTEM -> isSystemInDarkTheme()
                ThemeOption.LIGHT -> false
                ThemeOption.DARK -> true
            }
            if (isDark)
                dynamicDarkColorScheme(context)
            else
                dynamicLightColorScheme(context)
        } else {
            getThemeForSeedColor(spectacledVariant.themeSeedColor)
        }



        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
