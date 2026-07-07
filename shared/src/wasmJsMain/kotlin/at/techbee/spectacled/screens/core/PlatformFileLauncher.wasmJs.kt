package at.techbee.spectacled.screens.core

import kotlinx.browser.window
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.js.JsAny
import kotlin.js.JsNumber
import kotlin.js.toJsNumber

actual class PlatformFileLauncher : FileLauncher {
    actual override fun openFile(path: String, mimeType: String?) {
        // Local path opening not supported in browser
    }

    actual override fun openFile(bytes: ByteArray, fileName: String, mimeType: String?) {
        val uint8Array = org.khronos.webgl.Uint8Array(bytes.size)
        val jsArray = JsArray<JsNumber>()
        bytes.forEachIndexed { index, byte ->
            jsArray.set(index, byte.toInt().toJsNumber())
        }
        uint8Array.set(jsArray, 0)
        
        val blob = Blob(JsArray<JsAny?>().apply {
            set(0, uint8Array.unsafeCast<JsAny>())
        }, BlobPropertyBag(type = mimeType ?: "application/octet-stream"))
        
        val url = URL.createObjectURL(blob)
        window.open(url, "_blank")
    }
}
