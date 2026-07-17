package at.techbee.spectacled.screens.about.data

import at.techbee.spectacled.screens.about.domain.GitHubRelease
import at.techbee.spectacled.screens.core.ioDispatcher
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val BASE_URL = "https://api.github.com/repos/TechbeeAT/jtxBoard/releases?per_page=100"

class KtorRemoteGitHubReleaseDataSource(
    val client: HttpClient
) {

    // Dispatches its own network + JSON work onto ioDispatcher so callers don't have to.
    suspend fun getReleases(): List<GitHubRelease> = withContext(ioDispatcher) {
        try {
            val response = client.get(BASE_URL)
            response.body<List<GitHubReleaseDto>>().map { it.toGitHubRelease() }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Napier.w("Error: ${e.stackTraceToString()}")
            emptyList()
        }
    }
}