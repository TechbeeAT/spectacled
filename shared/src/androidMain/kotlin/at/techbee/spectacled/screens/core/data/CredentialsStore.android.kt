package at.techbee.spectacled.screens.core.data

import android.content.Context
import at.techbee.spectacled.screens.core.ioDispatcher
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import io.ktor.http.Url
import kotlinx.coroutines.withContext

actual class PlatformCredentialStore(context: Context): CredentialStore {

    private val ksafe = KSafe(context.applicationContext, CREDENTIALS_FILE_NAME)

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
}
