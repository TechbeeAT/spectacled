package at.techbee.spectacled.notes

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.techbee.spectacled.DeepLinkHandler
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.parseArgs
import at.techbee.spectacled.setupDesktopHandler
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.composenotesapp.generated.resources.Res
import spectacled.composenotesapp.generated.resources.icon_notes_png

fun main(args: Array<String>) {
    DeepLinkHandler.setupDesktopHandler(SpectacledVariant.NOTES)
    DeepLinkHandler.parseArgs(args, SpectacledVariant.NOTES)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(SpectacledVariant.NOTES.appNameStringRes),
            icon = painterResource(Res.drawable.icon_notes_png),  // sets the icon for window and taskbar
        ) {
            NotesApp()
        }
    }
}