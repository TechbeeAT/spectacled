package at.techbee.spectacled.tasks

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "spectacled Tasks",
    ) {
        TasksApp()
    }
}