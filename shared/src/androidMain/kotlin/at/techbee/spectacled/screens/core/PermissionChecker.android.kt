package at.techbee.spectacled.screens.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * The permission string rather than `Manifest.permission.ACCESS_LOCAL_NETWORK`, so the shared
 * module keeps compiling if the compileSdk is rolled back below 37.
 */
private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

/** First OS version that enforces the local network permission (Android 17). */
private const val SDK_LOCAL_NETWORK_ENFORCED = 37

/** The Android permission behind this one, or null where this OS version does not gate it. */
internal fun AppPermission.manifestPermission(): String? = when (this) {
    AppPermission.LOCAL_NETWORK -> ACCESS_LOCAL_NETWORK.takeIf { Build.VERSION.SDK_INT >= SDK_LOCAL_NETWORK_ENFORCED }
}

/** The Activity this Context is hosted by, unwrapping the wrappers Compose may hand over. */
internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

internal fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).apply {
        // The injected Context is the Application, which needs its own task to start an Activity.
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

actual class PlatformPermissionChecker(private val context: Context) : PermissionChecker {

    actual override fun status(permission: AppPermission): PermissionStatus {
        val manifestPermission = permission.manifestPermission() ?: return PermissionStatus.NOT_APPLICABLE

        return if (ContextCompat.checkSelfPermission(context, manifestPermission) == PackageManager.PERMISSION_GRANTED)
            PermissionStatus.GRANTED
        else
            PermissionStatus.DENIED
    }

    actual override fun openAppSettings() = context.openAppSettings()
}
