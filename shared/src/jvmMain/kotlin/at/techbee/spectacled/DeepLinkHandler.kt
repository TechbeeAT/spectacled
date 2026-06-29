package at.techbee.spectacled

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.awt.Desktop
import java.net.URI
import java.net.URLDecoder

actual object DeepLinkHandler {
    actual var initialCalendarId by mutableStateOf<Long?>(null)
    actual var initialIcalEntryId by mutableStateOf<Long?>(null)
    actual var initialIcalEntryDescription by mutableStateOf<String?>(null)

    actual fun onDeepLinkReceived(calendarId: Long?, entryId: Long?, description: String?) {
        initialCalendarId = calendarId
        initialIcalEntryId = entryId
        initialIcalEntryDescription = description
    }

    actual fun setupDesktopHandler() {
        try {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
                    desktop.setOpenURIHandler { event ->
                        handleDesktopUri(event.uri)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore if not supported on the current platform
        }
    }

    actual fun parseArgs(args: Array<String>) {
        args.forEach { arg ->
            if (arg.startsWith("spectacled-")) {
                try {
                    handleDesktopUri(URI(arg))
                } catch (e: Exception) {
                    // Ignore invalid URIs
                }
            }
        }
    }

    private fun handleDesktopUri(uri: URI) {
        if (uri.host == "add") {
            val query = uri.query ?: ""
            val params = query.split("&")
                .filter { it.contains("=") }
                .associate {
                    val parts = it.split("=")
                    val key = parts[0]
                    val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
                    key to value
                }
            val description = params["description"]
            onDeepLinkReceived(null, 0L, description)
        }
    }
}
