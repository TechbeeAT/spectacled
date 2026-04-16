package at.techbee.spectacled.screens.about.data

import at.techbee.spectacled.screens.about.domain.GitHubRelease
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("name") var name: String,   //release nane
    @SerialName("body") var body: String?,   // release text
    @SerialName("prerelease") var prerelease: Boolean, // prerelease
    @SerialName("html_url") var url: String
) {

    fun toGitHubRelease(): GitHubRelease {
        return GitHubRelease(
            releaseName = name,
            releaseText = body,
            prerelease = prerelease,
            githubUrl = url
        )
    }
}
