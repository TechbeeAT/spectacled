package at.techbee.spectacled.screens.core.data

import at.techbee.spectacled.screens.core.ioDispatcher
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import eu.anifantakis.lib.ksafe.awaitCacheReady
import io.ktor.http.Url
import kotlinx.coroutines.withContext

actual class PlatformCredentialStore(): CredentialStore {

    private val ksafe = KSafe(CREDENTIALS_FILE_NAME)

    // On web ioDispatcher is Dispatchers.Default (the single JS thread), so these stay effectively
    // on-thread; the wrapping is kept for parity with the other platforms and the repositories.
    actual override suspend fun save(credentials: Credentials) {
        withContext(ioDispatcher) {
            ksafe.put(credentials.server.toString(), credentials, KSafeWriteMode.Encrypted())
        }
    }

    actual override suspend fun load(server: Url): Credentials? = withContext(ioDispatcher) {
        ksafe.get(server.toString(), null)
    }

    actual override suspend fun clear(server: Url) {
        withContext(ioDispatcher) {
            ksafe.delete(server.toString())
        }
    }

    override suspend fun awaitReady() = ksafe.awaitCacheReady()
}