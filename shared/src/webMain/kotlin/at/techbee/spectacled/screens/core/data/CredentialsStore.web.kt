package at.techbee.spectacled.screens.core.data

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import io.ktor.http.Url

actual class PlatformCredentialStore(): CredentialStore {

    private val ksafe = KSafe(CREDENTIALS_FILE_NAME)

    actual override suspend fun save(credentials: Credentials) {
        ksafe.put(credentials.server.toString(), credentials, KSafeWriteMode.Encrypted())
    }

    actual override suspend fun load(server: Url): Credentials? = ksafe.get(server.toString(), null)

    actual override suspend fun clear(server: Url) {
        ksafe.delete(server.toString())
    }

    override suspend fun awaitReady() = ksafe.awaitCacheReady()
}