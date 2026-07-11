package at.techbee.spectacled.journals

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.SecureStorageReadyGate

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        SecureStorageReadyGate(SpectacledVariant.JOURNALS) {
            JournalsApp()
        }
    }
}
