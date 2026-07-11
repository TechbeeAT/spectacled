package at.techbee.spectacled.tasks

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.SecureStorageReadyGate

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        SecureStorageReadyGate(SpectacledVariant.TASKS) {
            TasksApp()
        }
    }
}
