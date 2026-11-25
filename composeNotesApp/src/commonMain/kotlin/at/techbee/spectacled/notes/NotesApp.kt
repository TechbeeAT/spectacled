package at.techbee.spectacled.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledApp
import at.techbee.spectacled.SpectacledVariant


@Composable
@Preview
fun NotesApp(
    onCloseApp: () -> Unit = {}
) = SpectacledApp(
    spectacledVariant = SpectacledVariant.NOTES,
    onCloseApp = onCloseApp
)
