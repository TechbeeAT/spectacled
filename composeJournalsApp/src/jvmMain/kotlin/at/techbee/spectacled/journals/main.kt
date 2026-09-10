package at.techbee.spectacled.journals

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.techbee.spectacled.DeepLinkHandler
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.parseArgs
import at.techbee.spectacled.setupDesktopHandler
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.composejournalsapp.generated.resources.Res
import spectacled.composejournalsapp.generated.resources.icon_journals_png

fun main(args: Array<String>) {
    DeepLinkHandler.setupDesktopHandler(SpectacledVariant.JOURNALS)
    DeepLinkHandler.parseArgs(args, SpectacledVariant.JOURNALS)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(SpectacledVariant.JOURNALS.appNameStringRes),
            // Without this the window and taskbar show the default Java icon. The
            // shared logo is a white silhouette meant for a coloured backdrop, so
            // this uses the same artwork the installers use as the app icon.
            icon = painterResource(Res.drawable.icon_journals_png),
        ) {
            JournalsApp()
        }
    }
}