package at.techbee.spectacled.screens.core.data

import android.content.Context
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import io.ktor.http.Url

actual class PlatformCredentialStore(context: Context): CredentialStore {

    private val ksafe = KSafe(context.applicationContext, CREDENTIALS_FILE_NAME)

    actual override suspend fun save(credentials: Credentials) {
        ksafe.put(credentials.server.toString(), credentials, KSafeWriteMode.Encrypted())
    }

    actual override suspend fun load(server: Url): Credentials? = ksafe.get(server.toString(), null)

    actual override suspend fun clear(server: Url) {
        ksafe.delete(server.toString())
    }
}
