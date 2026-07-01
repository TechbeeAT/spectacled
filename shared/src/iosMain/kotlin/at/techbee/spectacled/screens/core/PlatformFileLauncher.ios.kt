package at.techbee.spectacled.screens.core

import platform.Foundation.NSURL
import platform.UIKit.*
import platform.darwin.NSObject

actual class PlatformFileLauncher : FileLauncher {
    actual override fun openFile(path: String, mimeType: String?) {
        val url = NSURL.fileURLWithPath(path)
        val controller = UIDocumentInteractionController.interactionControllerWithURL(url)
        
        val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
        val rootViewController = window?.rootViewController
        
        if (rootViewController != null) {
            controller.delegate = object : NSObject(), UIDocumentInteractionControllerDelegateProtocol {
                override fun documentInteractionControllerViewControllerForPreview(controller: UIDocumentInteractionController): UIViewController {
                    return rootViewController
                }
            }
            controller.presentPreviewAnimated(true)
        }
    }

    actual override fun openFile(bytes: ByteArray, fileName: String, mimeType: String?) {
        val path = PlatformFileManager().saveAttachment(fileName, bytes)
        openFile(path, mimeType)
    }
}
