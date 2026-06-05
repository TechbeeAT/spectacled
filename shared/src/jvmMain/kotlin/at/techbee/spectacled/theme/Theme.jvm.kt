package at.techbee.spectacled.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import at.techbee.spectacled.SpectacledVariant

@Composable
actual fun AppTheme(
    spectacledVariant: SpectacledVariant,
    content: @Composable (() -> Unit)
) {
    MaterialTheme(
        colorScheme = getThemeForSeedColor(spectacledVariant.themeSeedColor),
        typography = AppTypography,
        content = content
    )
}