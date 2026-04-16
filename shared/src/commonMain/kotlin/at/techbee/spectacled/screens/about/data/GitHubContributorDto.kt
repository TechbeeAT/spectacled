package at.techbee.spectacled.screens.about.data

import at.techbee.spectacled.screens.about.domain.GitHubContributor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubContributorDto(
    @SerialName("login") var login: String,
    @SerialName("url") var url: String?,
    @SerialName("avatar_url") var avatarUrl: String?
) {

    fun toGitHubContributor(): GitHubContributor {
        return GitHubContributor(
            login = login,
            url = url,
            avatarUrl = avatarUrl
        )
    }
}
