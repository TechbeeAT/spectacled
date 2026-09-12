package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Nothing to ask through on iOS - report back so callers keep a single code path. */
@Composable
actual fun rememberPermissionRequester(
    onResult: (AppPermission, PermissionStatus) -> Unit
): PermissionRequester = remember(onResult) {
    PermissionRequester { permission -> onResult(permission, PermissionStatus.UNKNOWN) }
}
