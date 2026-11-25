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
            icon = painterResource(Res.drawable.icon_journals_png),  // sets the icon for window and taskbar
            ) {
            JournalsApp()
        }
    }
}