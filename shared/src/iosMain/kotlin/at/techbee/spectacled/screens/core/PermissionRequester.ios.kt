package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/**
 * iOS gates local network access from iOS 14 on, but on its own terms: the consent prompt is
 * raised implicitly by the first connection to a LAN address, and there is no public API to read
 * the current state or to ask ahead of time. So [PermissionRequester.status] reports
 * [PermissionStatus.UNKNOWN] and [PermissionRequester.request] does nothing - what makes the
 * prompt legible to the user is the `NSLocalNetworkUsageDescription` string in each app's
 * Info.plist.
 *
 * [PermissionRequester.openAppSettings] is real, and is the only way the user can revisit the
 * decision once made.
 */
@Composable
actual fun rememberPermissionRequester(
    onResult: (AppPermission, PermissionStatus) -> Unit
): PermissionRequester = remember(onResult) {
    object : PermissionRequester {

        override fun status(permission: AppPermission): PermissionStatus = when (permission) {
            AppPermission.LOCAL_NETWORK -> PermissionStatus.UNKNOWN
        }

        override fun request(permission: AppPermission) {
            // Nothing to ask for - report back so callers keep a single code path.
            onResult(permission, status(permission))
        }

        override fun openAppSettings() {
            val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
            // openURL:options:completionHandler: rather than the plain openURL:, which has been
            // deprecated since iOS 10.
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
}
