package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable

/**
 * Asks the user for a permission.
 *
 * This one is not injected, unlike [PermissionChecker]: on Android the prompt goes through an
 * `ActivityResultLauncher`, which has to be registered against the Activity before it reaches
 * STARTED and is torn down with it. A Koin singleton holds the Application and so can never own
 * one - hence the `remember`, the same shape `rememberImagePicker` and `rememberFilePicker` use
 * for the same reason.
 */
fun interface PermissionRequester {

    /**
     * Asks the user, if this platform has a way to. The outcome arrives through the `onResult`
     * callback passed to [rememberPermissionRequester] - always, including on the platforms where
     * this call does nothing, so callers can treat it as a single code path.
     */
    fun request(permission: AppPermission)
}

@Composable
expect fun rememberPermissionRequester(
    onResult: (AppPermission, PermissionStatus) -> Unit
): PermissionRequester
