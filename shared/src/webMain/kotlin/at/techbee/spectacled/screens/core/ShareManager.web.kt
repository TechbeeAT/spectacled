package at.techbee.spectacled.screens.core

import kotlinx.browser.window
import kotlin.js.JsName

/**
 * The Desktop-specific implementation of ShareManager.
 * It uses an Intent to trigger the system's share sheet.
 */
actual class PlatformShareManager : ShareManager {
    actual override fun share(content: ShareContent) {
        val url = "mailto:?subject=${encodeURIComponent(content.subject)}&body=${encodeURIComponent(content.body)}"
        window.open(url, target = "_blank")
    }
}


// external helper
@JsName("encodeURIComponent")
external fun encodeURIComponent(str: String): String
