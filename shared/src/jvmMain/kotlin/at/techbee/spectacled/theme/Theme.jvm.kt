package at.techbee.spectacled.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import at.techbee.spectacled.SpectacledVariant

@Composable
actual fun AppTheme(
    themeOption: ThemeOption,
    isSystemInDarkTheme: Boolean,
    dynamicColor: Boolean,
    spectacledVariant: SpectacledVariant,
    content: @Composable (() -> Unit)
) {

    val applyDarkTheme = themeOption == ThemeOption.DARK || (themeOption == ThemeOption.SYSTEM && isSystemInDarkTheme)
    val colorScheme = if (applyDarkTheme) spectacledVariant.darkColorScheme else spectacledVariant.lightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}