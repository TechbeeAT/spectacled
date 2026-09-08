package at.techbee.spectacled.screens.core.data

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import eu.anifantakis.lib.ksafe.awaitCacheReady
import io.ktor.http.Url

/**
 * Keeps the CalDAV credentials in the browser - or, in a private session, only in memory.
 *
 * KSafe encrypts what it writes, but on the web that buys nothing against the next person at the
 * same computer: the key is a non-extractable WebCrypto key in the same origin's IndexedDB, so the
 * same browser profile decrypts it automatically. Not writing at all is the only thing that
 * actually keeps a shared machine clean, which is what the in-memory branch is for.
 */
actual class PlatformCredentialStore : CredentialStore {

    private val ksafe =
        if (getLocalDataPolicy().current == LocalDataPersistence.SESSION_ONLY) null
        else KSafe(CREDENTIALS_FILE_NAME)

    /** Only used while [ksafe] is `null`; lives and dies with the page. */
    private val sessionCredentials = mutableMapOf<String, Credentials>()

    actual override suspend fun save(credentials: Credentials) {
        val server = credentials.server.toString()
        if (ksafe == null) sessionCredentials[server] = credentials
        else ksafe.put(server, credentials, KSafeWriteMode.Encrypted())
    }

    actual override suspend fun load(server: Url): Credentials? =
        if (ksafe == null) sessionCredentials[server.toString()]
        else ksafe.get(server.toString(), null)

    actual override suspend fun clear(server: Url) {
        if (ksafe == null) sessionCredentials.remove(server.toString())
        else ksafe.delete(server.toString())
    }

    override suspend fun awaitReady() {
        ksafe?.awaitCacheReady()
    }
}
