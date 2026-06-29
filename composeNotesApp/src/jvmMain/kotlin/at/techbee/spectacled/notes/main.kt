package at.techbee.spectacled.notes

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.techbee.spectacled.DeepLinkHandler

fun main(args: Array<String>) {
    DeepLinkHandler.setupDesktopHandler()
    DeepLinkHandler.parseArgs(args)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "spectacled Notes",
        ) {
            NotesApp()
        }
    }
}