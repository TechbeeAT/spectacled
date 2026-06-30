package at.techbee.spectacled

import java.awt.Desktop
import java.net.URI
import java.net.URLDecoder

fun DeepLinkHandler.setupDesktopHandler(variant: SpectacledVariant) {
    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
                desktop.setOpenURIHandler { event ->
                    handleDesktopUri(event.uri, variant)
                }
            }
        }
    } catch (e: Exception) {
        // Ignore if not supported on the current platform
    }
}

fun DeepLinkHandler.parseArgs(args: Array<String>, variant: SpectacledVariant) {
    args.forEach { arg ->
        val uriSchemePrefix = "${variant.deeplinkUriScheme}://"
        val webUrlPrefix = "${variant.deeplinkWebUri.lowercase()}/add"
        
        if (arg.startsWith(uriSchemePrefix) || arg.startsWith(webUrlPrefix)) {
            try {
                handleDesktopUri(URI(arg), variant)
            } catch (e: Exception) {
                // Ignore invalid URIs
            }
        }
    }
}

private fun handleDesktopUri(uri: URI, variant: SpectacledVariant) {
    val isSchemeMatch = uri.scheme == variant.deeplinkUriScheme && uri.host == DeepLinkData.DEEPLINK_ADD_HOST
    val isWebUrlMatch = uri.scheme == "https" && 
                       uri.host == "spectacled.techbee.at" && 
                       uri.path == "/${variant.name.lowercase()}/add"

    if (isSchemeMatch || isWebUrlMatch) {
        val query = uri.query ?: ""
        val params = query.split("&")
            .filter { it.contains("=") }
            .associate {
                val parts = it.split("=")
                val key = parts[0]
                val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
                key to value
            }
        val description = params[DeepLinkData.DEEPLINK_DESCRIPTION_PARAM]
        DeepLinkHandler.onDeepLinkReceived(null, 0L, description)
    }
}
