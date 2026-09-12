package at.techbee.spectacled.screens.core

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPermissionRequester(
    onResult: (AppPermission, PermissionStatus) -> Unit
): PermissionRequester {
    val context = LocalContext.current

    // The returned object is remembered across recompositions, so it must not capture the callback
    // it was first built with - by the time a result arrives, the caller's lambda has been recreated.
    val currentOnResult by rememberUpdatedState(onResult)

    // Which permission the in-flight launcher is for: the contract only reports a boolean back.
    var requested by remember { mutableStateOf<AppPermission?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        requested?.let { permission ->
            currentOnResult(permission, if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED)

            // Android stops offering the dialog once the user has refused twice, and from then on
            // launch() returns denied immediately without showing anything ("No requestable
            // permission in the request." in logcat), which leaves the button looking dead. A
            // rationale the system will no longer show is how that state announces itself, so fall
            // back to the settings page, where the grant can still be changed.
            val manifestPermission = permission.manifestPermission()
            val activity = context.findActivity()
            if (!granted && manifestPermission != null &&
                activity?.shouldShowRequestPermissionRationale(manifestPermission) == false
            ) {
                context.openAppSettings()
            }
        }
        requested = null
    }

    return remember(context) {
        object : PermissionRequester {
            override fun request(permission: AppPermission) {
                val manifestPermission = permission.manifestPermission()
                if (manifestPermission == null) {
                    currentOnResult(permission, PermissionStatus.NOT_APPLICABLE)
                    return
                }
                requested = permission
                launcher.launch(manifestPermission)
            }
        }
    }
}
