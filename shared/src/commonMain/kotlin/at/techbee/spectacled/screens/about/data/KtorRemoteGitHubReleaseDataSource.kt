package at.techbee.spectacled.screens.about.data

import at.techbee.spectacled.screens.about.domain.GitHubRelease
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private const val BASE_URL = "https://api.github.com/repos/TechbeeAT/jtxBoard/releases?per_page=100"

class KtorRemoteGitHubReleaseDataSource(
    val client: HttpClient
) {

    suspend fun getReleases(): List<GitHubRelease> {
        return try {
            val response = client.get(BASE_URL)
            response.body<List<GitHubReleaseDto>>().map { it.toGitHubRelease() }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            println("Error: ${e.stackTraceToString()}")
            emptyList()
        }
    }
}