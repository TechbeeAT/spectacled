package at.techbee.spectacled.tasks

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.techbee.spectacled.DeepLinkHandler
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.parseArgs
import at.techbee.spectacled.setupDesktopHandler
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.composetasksapp.generated.resources.Res
import spectacled.composetasksapp.generated.resources.icon_tasks_png

fun main(args: Array<String>) {
    DeepLinkHandler.setupDesktopHandler(SpectacledVariant.TASKS)
    DeepLinkHandler.parseArgs(args, SpectacledVariant.TASKS)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(SpectacledVariant.TASKS.appNameStringRes),
            icon = painterResource(Res.drawable.icon_tasks_png),  // sets the icon for window and taskbar
        ) {
            TasksApp()
        }
    }
}