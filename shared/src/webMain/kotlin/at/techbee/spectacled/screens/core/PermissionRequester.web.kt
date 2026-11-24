package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import at.techbee.spectacled.screens.core.alarm.WebNotifications

/**
 * Notifications are asked through the browser's prompt. Some browsers only show it in response to
 * a click (the "Allow" hint is one), and none show it again after a refusal - the status is then
 * reported back unchanged.
 */
@Composable
actual fun rememberPermissionRequester(
    onResult: (AppPermission, PermissionStatus) -> Unit
): PermissionRequester {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        PermissionRequester { permission ->
            when (permission) {
                AppPermission.NOTIFICATIONS ->
                    if (WebNotifications.canPrompt())
                        WebNotifications.requestPermission { status -> currentOnResult(permission, status) }
                    else
                        currentOnResult(permission, WebNotifications.permissionStatus())

                AppPermission.LOCAL_NETWORK, AppPermission.EXACT_ALARMS ->
                    currentOnResult(permission, PermissionStatus.NOT_APPLICABLE)
            }
        }
    }
}
