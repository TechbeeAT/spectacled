package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import at.techbee.spectacled.screens.core.data.CredentialStore
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import org.koin.mp.KoinPlatform

@Composable
actual fun SecureStorageReadyGate(content: @Composable () -> Unit) {
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
