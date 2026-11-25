package at.techbee.spectacled.journals

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledApp
import at.techbee.spectacled.SpectacledVariant


@Composable
@Preview
fun JournalsApp(
    onCloseApp: () -> Unit = {}
) = SpectacledApp(
    spectacledVariant = SpectacledVariant.JOURNALS,
    onCloseApp = onCloseApp
)
