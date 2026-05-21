package at.techbee.spectacled.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (applyDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> if (applyDarkTheme) spectacledVariant.darkColorScheme else spectacledVariant.lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}