package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import at.techbee.spectacled.screens.core.alarm.IosNotifications
import org.koin.compose.koinInject

/**
 * Notifications are asked through UNUserNotificationCenter. iOS shows its prompt only once; after
 * a refusal the user can only change it in the Settings app, so that is opened instead.
 * Local network has no API to ask through (see PlatformPermissionChecker), and exact alarms don't
 * exist on iOS.
 */
@Composable
actual fun rememberPermissionRequester(
    onResult: (AppPermission, PermissionStatus) -> Unit
): PermissionRequester {
    val permissionChecker: PermissionChecker = koinInject()
    // The returned object outlives recompositions; don't let it capture a stale callback.
    val currentOnResult by rememberUpdatedState(onResult)

    return remember(permissionChecker) {
        PermissionRequester { permission ->
            when (permission) {
                AppPermission.NOTIFICATIONS ->
                    if (IosNotifications.canPrompt()) {
                        IosNotifications.requestAuthorization { status -> currentOnResult(permission, status) }
                    } else {
                        // Refused before: the prompt won't show again. Read back on return (didBecomeActive).
                        if (permissionChecker.status(permission) == PermissionStatus.DENIED)
                            permissionChecker.openAppSettings()
                        currentOnResult(permission, permissionChecker.status(permission))
                    }
                AppPermission.EXACT_ALARMS -> currentOnResult(permission, PermissionStatus.NOT_APPLICABLE)
                AppPermission.LOCAL_NETWORK -> currentOnResult(permission, PermissionStatus.UNKNOWN)
            }
        }
    }
}
