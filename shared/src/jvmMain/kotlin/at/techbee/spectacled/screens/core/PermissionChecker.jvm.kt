package at.techbee.spectacled.screens.core

/** Desktop has no permission model for any of [AppPermission], so every call is inert. */
actual class PlatformPermissionChecker : PermissionChecker {
    actual override fun status(permission: AppPermission): PermissionStatus = PermissionStatus.NOT_APPLICABLE
    actual override fun openAppSettings() {/* No per-app permission page to open. */ }
}
