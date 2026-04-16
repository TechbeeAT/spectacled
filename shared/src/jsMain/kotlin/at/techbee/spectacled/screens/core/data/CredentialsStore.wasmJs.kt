package at.techbee.spectacled.screens.core.data

import io.ktor.http.Url

actual class PlatformCredentialStore(): CredentialStore {

    private var cached: Credentials? = null

    actual override suspend fun save(credentials: Credentials) {
        cached = credentials
    }

    actual override suspend fun load(server: Url) = cached

    actual override suspend fun clear(server: Url) {
        cached = null
    }
}