package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.alarm.IosNotifications
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/**
 * iOS gates local network access from iOS 14 on, but on its own terms: the consent prompt is
 * raised implicitly by the first connection to a LAN address, and there is no public API to read
 * the current state or to ask ahead of time. So [status] reports [PermissionStatus.UNKNOWN] and
 * the prompt is made legible instead by the NSLocalNetworkUsageDescription string in each app's
 * Info.plist. [openAppSettings] is the only way the user can revisit the decision once made.
 *
 * Notifications report the status last read by [IosNotifications], which refreshes it whenever the
 * app becomes active.
 */
actual class PlatformPermissionChecker : PermissionChecker {

    actual override fun status(permission: AppPermission): PermissionStatus = when (permission) {
        AppPermission.LOCAL_NETWORK -> PermissionStatus.UNKNOWN
        AppPermission.NOTIFICATIONS -> IosNotifications.permissionStatus()
        AppPermission.EXACT_ALARMS -> PermissionStatus.NOT_APPLICABLE
    }

    actual override fun openAppSettings() {
        NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
}
