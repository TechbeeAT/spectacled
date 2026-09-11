package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Desktop has no permission model for any of [AppPermission], so every call is inert. */
@Composable
actual fun rememberPermissionRequester(
    onResult: (AppPermission, PermissionStatus) -> Unit
): PermissionRequester = remember(onResult) {
    object : PermissionRequester {

        override fun status(permission: AppPermission): PermissionStatus = PermissionStatus.NOT_APPLICABLE

        override fun request(permission: AppPermission) {
            onResult(permission, PermissionStatus.NOT_APPLICABLE)
        }

        override fun openAppSettings() {
            // No per-app permission page to open.
        }
    }
}
