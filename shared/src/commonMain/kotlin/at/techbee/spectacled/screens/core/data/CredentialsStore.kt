package at.techbee.spectacled.screens.core.data

import io.ktor.http.Url
import kotlinx.serialization.Serializable


const val CREDENTIALS_FILE_NAME = "credentials"

@Serializable
data class Credentials(
    val server: String,
    val username: String,
    val password: String
)

interface CredentialStore {
    suspend fun save(credentials: Credentials)
    suspend fun load(server: Url): Credentials?
    suspend fun clear(server: Url)
}


expect class PlatformCredentialStore: CredentialStore {
    override suspend fun save(credentials: Credentials)
    override suspend fun load(server: Url): Credentials?
    override suspend fun clear(server: Url)
}