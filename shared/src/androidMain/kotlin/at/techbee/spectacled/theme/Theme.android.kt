package at.techbee.spectacled.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.journals.darkJournalsScheme
import at.techbee.spectacled.theme.journals.lightJournalsScheme
import at.techbee.spectacled.theme.notes.darkNotesScheme
import at.techbee.spectacled.theme.notes.lightNotesScheme
import at.techbee.spectacled.theme.tasks.darkTasksScheme
import at.techbee.spectacled.theme.tasks.lightTasksScheme

@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    spectacledVariant: SpectacledVariant,
    content: @Composable (() -> Unit)
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when(spectacledVariant) {
            SpectacledVariant.JOURNALS -> if (darkTheme) darkJournalsScheme else lightJournalsScheme
            SpectacledVariant.NOTES -> if (darkTheme) darkNotesScheme else lightNotesScheme
            SpectacledVariant.TASKS -> if (darkTheme) darkTasksScheme else lightTasksScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}