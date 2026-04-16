package at.techbee.spectacled.screens.about.data

import at.techbee.spectacled.screens.about.domain.GitHubContributor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

//TODO: Change for release!!!
private const val BASE_URL = "https://api.github.com/repos/TechbeeAT/jtxBoard/contributors"

class KtorRemoteGitHubContributorDataSource(
    val client: HttpClient
) {

    suspend fun getContributors(): List<GitHubContributor> {
        return try {
            val response = client.get(BASE_URL)
            response.body<List<GitHubContributorDto>>().map { it.toGitHubContributor() }
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            println("Error: ${e.stackTraceToString()}")
            emptyList()
        }
    }
}