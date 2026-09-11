package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable

/**
 * A permission the app may have to ask the operating system for.
 *
 * Deliberately an enum of app-level concepts rather than platform permission strings: the same
 * entry maps to a different mechanism per target (a runtime permission on Android, an implicit
 * consent prompt on iOS, nothing at all on Desktop and Web). Adding a permission means adding a
 * constant here and a branch in the Android actual.
 */
enum class AppPermission {
    /**
     * Reaching hosts on the user's own network - a self-hosted CalDAV server, or one of the
     * OpenAI-compatible AI endpoints.
     *
     * Android 17 (API 37) gates this behind `android.permission.ACCESS_LOCAL_NETWORK`; before
     * that it came for free with `INTERNET`. Denied TCP connects do not fail fast, they time
     * out, so an app that never asks looks broken rather than blocked.
     */
    LOCAL_NETWORK
}

enum class PermissionStatus {
    GRANTED,

    /** Refused, or never asked for - either way the app cannot act until [PermissionRequester.request]. */
    DENIED,

    /**
     * The platform gates access but offers no way to read the current state (iOS). Distinct from
     * [DENIED] so the UI can say "we cannot tell" instead of claiming a refusal that may not exist.
     */
    UNKNOWN,

    /** Nothing to ask for on this platform or OS version. Callers show no permission UI at all. */
    NOT_APPLICABLE
}

interface PermissionRequester {

    fun status(permission: AppPermission): PermissionStatus

    /**
     * Asks the user, if this platform has a way to. The outcome arrives through the
     * `onResult` callback passed to [rememberPermissionRequester] - always, including on the
     * platforms where this call does nothing, so callers can treat it as a single code path.
     */
    fun request(permission: AppPermission)

    /**
     * Opens the OS page where the user can review or revoke what they granted.
     *
     * Takes no [AppPermission]: Android and iOS both only expose a per-app settings page, so a
     * parameter here would promise a precision neither platform delivers.
     */
    fun openAppSettings()
}

@Composable
expect fun rememberPermissionRequester(
    onResult: (AppPermission, PermissionStatus) -> Unit
): PermissionRequester
