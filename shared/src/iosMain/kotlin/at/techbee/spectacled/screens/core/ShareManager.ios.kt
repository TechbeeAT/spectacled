package at.techbee.spectacled.screens.core

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/**
 * The iOS-specific implementation of ShareManager. AI supported implementation.
 * It uses UIActivityViewController to present the native share sheet.
 */
actual class PlatformShareManager : ShareManager {
    @OptIn(ExperimentalForeignApi::class)
    actual override fun share(content: ShareContent) {

        // Use a 300ms delay to allow any ongoing transitions (like closing a BottomSheet) to finish.
        // UIKit often silently fails to present if a dismissal is in progress.
        val delay = dispatch_time(DISPATCH_TIME_NOW, 300_000_000L) // 300ms
        dispatch_after(delay, dispatch_get_main_queue()) {

            val items = mutableListOf<Any>()
            items.add(content.body)

            val activityController = UIActivityViewController(
                activityItems = items,
                applicationActivities = null
            )

            // Modern way to find the active window in Scene-based apps
            val window = UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
                ?.windows
                ?.filterIsInstance<UIWindow>()
                ?.firstOrNull { it.isKeyWindow() }
                ?: UIApplication.sharedApplication.keyWindow
                ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow

            var topController = window?.rootViewController
            while (topController?.presentedViewController != null && 
                   topController.presentedViewController?.isBeingDismissed() == false) {
                topController = topController.presentedViewController
            }

            if (topController == null)
                return@dispatch_after

            // iPad support: Must use popover
            if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
                activityController.popoverPresentationController?.apply {
                    sourceView = topController.view
                    sourceRect = topController.view.bounds
                }
            }

            topController.presentViewController(
                viewControllerToPresent = activityController,
                animated = true,
                completion = {
                    //println("PlatformShareManager: Presentation complete callback triggered") //debugging
                }
            )
        }
    }
}
