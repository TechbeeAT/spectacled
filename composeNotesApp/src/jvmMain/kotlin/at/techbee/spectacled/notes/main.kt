package at.techbee.spectacled.notes

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.techbee.spectacled.DeepLinkHandler
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.parseArgs
import at.techbee.spectacled.setupDesktopHandler

fun main(args: Array<String>) {
    DeepLinkHandler.setupDesktopHandler(SpectacledVariant.NOTES)
    DeepLinkHandler.parseArgs(args, SpectacledVariant.NOTES)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "spectacled Notes",
        ) {
            NotesApp()
        }
    }
}