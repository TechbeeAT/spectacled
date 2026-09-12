package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The browser grants nothing and asks nothing here: the web build reaches CalDAV servers through
 * the CORS proxy (see `HttpClientFactory`), so it never opens a local network socket itself.
 */
@Composable
actual fun rememberPermissionRequester(
    onResult: (AppPermission, PermissionStatus) -> Unit
): PermissionRequester = remember(onResult) {
    object : PermissionRequester {
        override fun status(permission: AppPermission): PermissionStatus = PermissionStatus.NOT_APPLICABLE
        override fun request(permission: AppPermission) { onResult(permission, PermissionStatus.NOT_APPLICABLE) }
        override fun openAppSettings() {  /* No per-app permission page to open. */  }
    }
}
