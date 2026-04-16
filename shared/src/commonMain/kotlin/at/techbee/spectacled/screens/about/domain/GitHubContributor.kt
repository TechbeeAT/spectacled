package at.techbee.spectacled.screens.about.domain

data class GitHubContributor(
    var login: String,
    var url: String?,
    var avatarUrl: String?
) {
    companion object Companion {
        fun getSample() = GitHubContributor(
            login = "Sample",
            url = "https://github.com/patrickunterwegs",
            avatarUrl = "https://avatars.githubusercontent.com/u/123456789?v=4"
        )
    }
}