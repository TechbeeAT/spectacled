package at.techbee.spectacled.screens.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * The permission string rather than `Manifest.permission.ACCESS_LOCAL_NETWORK`, so the shared
 * module keeps compiling if the compileSdk is rolled back below 37.
 */
private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

/** First OS version that enforces the local network permission (Android 17). */
private const val SDK_LOCAL_NETWORK_ENFORCED = 37

private fun AppPermission.manifestPermission(): String? = when (this) {
    AppPermission.LOCAL_NETWORK -> ACCESS_LOCAL_NETWORK.takeIf { Build.VERSION.SDK_INT >= SDK_LOCAL_NETWORK_ENFORCED }
}

/** The Activity this Context is hosted by, unwrapping the wrappers Compose may hand over. */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).apply {
        // The Context here may be the Activity, but callers can also reach this from a
        // non-Activity Context, where a new task is required.
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

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

            override fun status(permission: AppPermission): PermissionStatus {
                val manifestPermission = permission.manifestPermission() ?: return PermissionStatus.NOT_APPLICABLE

                return if (ContextCompat.checkSelfPermission(context, manifestPermission) == PackageManager.PERMISSION_GRANTED)
                    PermissionStatus.GRANTED
                else
                    PermissionStatus.DENIED
            }

            override fun request(permission: AppPermission) {
                val manifestPermission = permission.manifestPermission()
                if (manifestPermission == null) {
                    currentOnResult(permission, PermissionStatus.NOT_APPLICABLE)
                    return
                }
                requested = permission
                launcher.launch(manifestPermission)
            }

            override fun openAppSettings() = context.openAppSettings()
        }
    }
}
