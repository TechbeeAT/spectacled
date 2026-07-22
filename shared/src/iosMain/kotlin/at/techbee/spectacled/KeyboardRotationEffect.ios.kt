package at.techbee.spectacled

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIDeviceOrientationDidChangeNotification

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun DismissKeyboardOnRotationEffect() {
    DisposableEffect(Unit) {
        val device = UIDevice.currentDevice
        device.beginGeneratingDeviceOrientationNotifications()

        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIDeviceOrientationDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            // Only react to real interface rotations, not face-up/face-down/unknown, so laying
            // the device flat does not close the keyboard.
            if (device.orientation.isValidInterfaceOrientation()) {
                // Resign the first responder now, while the text field is still active, so UIKit
                // posts a clean UIKeyboardWillHideNotification before the rotation reframes the
                // keyboard. Otherwise the IME window-inset that drives imePadding() can freeze at
                // its pre-rotation height and leave a phantom gap at the bottom of the screen.
                UIApplication.sharedApplication.sendAction(
                    NSSelectorFromString("resignFirstResponder"),
                    to = null,
                    from = null,
                    forEvent = null
                )
            }
        }

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            device.endGeneratingDeviceOrientationNotifications()
        }
    }
}

private fun UIDeviceOrientation.isValidInterfaceOrientation(): Boolean =
    this == UIDeviceOrientation.UIDeviceOrientationPortrait ||
        this == UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ||
        this == UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ||
        this == UIDeviceOrientation.UIDeviceOrientationLandscapeRight
