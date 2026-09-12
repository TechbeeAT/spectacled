package at.techbee.spectacled.screens.core

/**
 * The browser grants nothing and asks nothing here: the web build reaches CalDAV servers through
 * the CORS proxy (see `HttpClientFactory`), so it never opens a local network socket itself.
 */
actual class PlatformPermissionChecker : PermissionChecker {
    actual override fun status(permission: AppPermission): PermissionStatus = PermissionStatus.NOT_APPLICABLE
    actual override fun openAppSettings() {/* No per-app permission page to open. */ }
}
