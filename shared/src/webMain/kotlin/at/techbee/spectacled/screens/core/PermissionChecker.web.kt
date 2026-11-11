package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.alarm.WebNotifications

/**
 * Only notifications have a browser permission. Local network doesn't apply: the web build reaches
 * CalDAV servers through the CORS proxy (see `HttpClientFactory`), so it never opens a local
 * network socket itself. A refused notification permission can only be changed in the browser's
 * site settings, which no page can open.
 */
actual class PlatformPermissionChecker : PermissionChecker {
    actual override fun status(permission: AppPermission): PermissionStatus = when (permission) {
        AppPermission.NOTIFICATIONS -> WebNotifications.permissionStatus()
        AppPermission.LOCAL_NETWORK, AppPermission.EXACT_ALARMS -> PermissionStatus.NOT_APPLICABLE
    }
    actual override fun openAppSettings() {/* No per-app permission page to open. */ }
}
