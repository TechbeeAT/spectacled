package at.techbee.spectacled.screens.core

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/**
 * The iOS-specific implementation of ShareManager.
 * It uses UIActivityViewController to present the native share sheet.
 */
actual class PlatformShareManager(): ShareManager {
    actual override fun share(content: ShareContent) {

        val items = mutableListOf<Any>(content.body)
        content.subject.let { items.add(it) }
        val activityController = UIActivityViewController(
            activityItems = items,
            applicationActivities = null
        )
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootController?.presentViewController(activityController, animated = true, completion = null)
    }
}
