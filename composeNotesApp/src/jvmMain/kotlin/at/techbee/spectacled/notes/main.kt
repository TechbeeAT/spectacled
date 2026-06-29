package at.techbee.spectacled.notes

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.techbee.spectacled.parseArgsForDeepLink
import at.techbee.spectacled.setupDesktopDeepLinkHandler

fun main(args: Array<String>) {
    setupDesktopDeepLinkHandler()
    parseArgsForDeepLink(args)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "spectacled Notes",
        ) {
            NotesApp()
        }
    }
}