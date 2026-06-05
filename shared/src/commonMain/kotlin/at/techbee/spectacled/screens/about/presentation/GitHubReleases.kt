package at.techbee.spectacled.screens.about.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.about.domain.GitHubRelease
import at.techbee.spectacled.screens.core.presentation.components.SpecialRoundedCard
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.about_release_notes
import spectacled.shared.generated.resources.more_on_github


//TODO: Change for Release!!!
private const val GITHUB_RELEASES_URL = "https://github.com/TechbeeAT/jtxBoard/releases"

@Composable
fun GitHubReleasesRoot(
    viewModel: AboutViewModel
) {

    GitHubReleases(viewModel.state.gitHubReleases)
}

@Composable
fun GitHubReleases(
    releases: List<GitHubRelease>
) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize()) {

        Text(
            text = stringResource(Res.string.about_release_notes),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp).fillMaxSize()
        ) {

            itemsIndexed(releases) { index, release ->
                SpecialRoundedCard(
                    isFirst = index == 0,
                    isLast = index == releases.lastIndex,
                    onClick = {
                        try {
                            uriHandler.openUri(release.githubUrl)
                        } catch (e: Exception) {
                            println(e.stackTraceToString())
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = release.releaseName,
                            style = MaterialTheme.typography.titleSmall
                        )
                        release.releaseText?.let {
                            Text(
                                text = it,
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(GITHUB_RELEASES_URL)
                        } catch (e: Exception) {
                            println(e.stackTraceToString())
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(Res.string.more_on_github))
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null)
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun GitHubReleases_Preview() {

    val releases = listOf(
        GitHubRelease(
            releaseName = "Release 1",
            releaseText = "Release 1 Text",
            prerelease = false,
            githubUrl = "https://github.com"
        ),
        GitHubRelease(
            releaseName = "Release 2",
            releaseText = "Release 2 Text",
            prerelease = false,
            githubUrl = "https://github.com"
        ),
        GitHubRelease(
            releaseName = "Release 3",
            releaseText = "Release 3 Text",
            prerelease = true,
            githubUrl = "https://github.com"
        ),
    )

    GitHubReleases(releases)
}