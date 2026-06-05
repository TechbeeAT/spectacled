package at.techbee.spectacled.screens.about.presentation

import at.techbee.spectacled.screens.about.domain.GitHubContributor
import at.techbee.spectacled.screens.about.domain.GitHubRelease
import com.mikepenz.aboutlibraries.Libs

data class AboutState(
    val gitHubContributors: List<GitHubContributor> = emptyList(),
    val gitHubReleases: List<GitHubRelease> = emptyList(),
    val libraries: Libs = Libs(emptyList(), emptySet())
)
