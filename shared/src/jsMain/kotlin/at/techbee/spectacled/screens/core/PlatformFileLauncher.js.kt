package at.techbee.spectacled.screens.core

import kotlinx.browser.window
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

actual class PlatformFileLauncher : FileLauncher {
    actual override fun openFile(path: String, mimeType: String?) {
        // Local path opening not supported in browser
    }

    actual override fun openFile(bytes: ByteArray, fileName: String, mimeType: String?) {
        val uint8Array = org.khronos.webgl.Uint8Array(bytes.size)
        bytes.forEachIndexed { index, byte -> uint8Array.asDynamic()[index] = byte.toInt() }
        
        val blob = Blob(arrayOf(uint8Array), BlobPropertyBag(type = mimeType ?: "application/octet-stream"))
        
        val url = URL.createObjectURL(blob)
        window.open(url, "_blank")
    }
}
