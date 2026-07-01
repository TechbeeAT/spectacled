package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject

class IosFilePicker(
    private val onFilePicked: (PickedFile?) -> Unit
) : FilePicker {

    private val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
        @OptIn(ExperimentalForeignApi::class)
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            if (url == null) {
                onFilePicked(null)
                return
            }

            val data = NSData.dataWithContentsOfURL(url)
            if (data == null) {
                onFilePicked(null)
                return
            }

            val bytes = ByteArray(data.length.toInt())
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
            }

            val name = url.lastPathComponent ?: "unknown"
            onFilePicked(PickedFile(name, bytes, null))
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onFilePicked(null)
        }
    }

    override fun pickFile() {
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeData),
            asCopy = true
        )
        picker.delegate = delegate

        val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
        val rootViewController = window?.rootViewController

        rootViewController?.presentViewController(
            picker,
            animated = true,
            completion = null
        )
    }
}

@Composable
actual fun rememberFilePicker(onFilePicked: (PickedFile?) -> Unit): FilePicker {
    return remember { IosFilePicker(onFilePicked) }
}
