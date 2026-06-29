package at.techbee.spectacled

import java.awt.Desktop
import java.net.URI
import java.net.URLDecoder

fun setupDesktopDeepLinkHandler() {
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

fun handleDesktopUri(uri: URI) {
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
        DeepLinkHandler.onDeepLinkReceived(null, 0L, description)
    }
}

fun parseArgsForDeepLink(args: Array<String>) {
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
