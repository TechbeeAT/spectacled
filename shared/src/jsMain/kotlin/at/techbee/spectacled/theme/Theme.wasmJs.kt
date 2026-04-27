package at.techbee.spectacled.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import at.techbee.spectacled.SpectacledVariant

@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    spectacledVariant: SpectacledVariant,
    content: @Composable (() -> Unit)
) {

    val colorScheme = when(spectacledVariant) {
        SpectacledVariant.JOURNALS -> if (darkTheme) darkJournalsScheme else lightJournalsScheme
        SpectacledVariant.NOTES -> if (darkTheme) darkNotesScheme else lightNotesScheme
        SpectacledVariant.TASKS -> if (darkTheme) darkTasksScheme else lightTasksScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}