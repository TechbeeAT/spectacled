package at.techbee.spectacled.journals

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.techbee.spectacled.DeepLinkHandler
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.parseArgs
import at.techbee.spectacled.setupDesktopHandler
import org.jetbrains.compose.resources.stringResource

fun main(args: Array<String>) {
    DeepLinkHandler.setupDesktopHandler(SpectacledVariant.JOURNALS)
    DeepLinkHandler.parseArgs(args, SpectacledVariant.JOURNALS)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(SpectacledVariant.JOURNALS.appNameStringRes),
        ) {
            JournalsApp()
        }
    }
}