package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.doInitKoin
import at.techbee.spectacled.screens.core.data.CredentialStore
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import org.koin.mp.KoinPlatform

/**
 * Web-only wrapper for each app's ComposeViewport content. Gates [content] until KSafe's
 * async key setup has completed for every KSafe-backed store, so a synchronous read of an
 * encrypted value right after startup (e.g. UserAppPreferencesStore.claudeUserApiKey, read
 * through KSafe's getDirect) can't race the key becoming available and silently return the
 * default. See SEC-9.
 *
 * Android/iOS/Desktop don't need this - KSafe's key setup there isn't async - so this lives
 * only in webMain instead of as a no-op wrapper on every other platform.
 *
 * Calls doInitKoin itself so the stores can be resolved before SpectacledApp (inside
 * [content]) would otherwise start Koin; doInitKoin is idempotent (guarded internally), so
 * SpectacledApp calling it again afterwards is a no-op.
 */
@Composable
fun SecureStorageReadyGate(spectacledVariant: SpectacledVariant, content: @Composable () -> Unit) {
    doInitKoin(spectacledVariant)

    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Resolved through Koin (not a fresh KSafe(...)) so this awaits the exact same
        // instances the rest of the app reads from - per KSafe's own docs, two separate
        // instances on the same fileName still diverge caches on web.
        val koin = KoinPlatform.getKoin()
        koin.get<CredentialStore>().awaitReady()
        koin.get<UserAppPreferencesStore>().awaitReady()
        ready = true
    }
    if (ready) content()
}
