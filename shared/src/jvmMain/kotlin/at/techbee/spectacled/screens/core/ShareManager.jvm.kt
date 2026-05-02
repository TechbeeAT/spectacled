package at.techbee.spectacled.screens.core

import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

/**
 * The Desktop-specific implementation of ShareManager.
 * It uses an Intent to trigger the system's share sheet.
 */
actual class PlatformShareManager : ShareManager {
    actual override fun share(content: ShareContent) {
        val uri = URI("mailto:?subject=${content.subject.urlEncode()}&body=${content.body.urlEncode()}")
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().mail(uri)
        }
    }
}


private fun String.urlEncode(): String =
    URLEncoder.encode(this, "UTF-8").replace("+", "%20")

