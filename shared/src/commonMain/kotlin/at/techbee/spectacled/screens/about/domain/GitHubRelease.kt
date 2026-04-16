package at.techbee.spectacled.screens.about.domain

data class GitHubRelease(
    var releaseName: String,
    var releaseText: String?,
    var prerelease: Boolean,
    var githubUrl: String
)
