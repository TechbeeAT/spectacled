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
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.about.domain.GitHubRelease
import at.techbee.spectacled.screens.core.buildIsPrerelease
import at.techbee.spectacled.screens.core.presentation.components.SpecialRoundedCard
import at.techbee.spectacled.theme.AppTheme
import com.mikepenz.markdown.m3.Markdown
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.about_release_notes
import spectacled.shared.generated.resources.more_on_github


private const val GITHUB_RELEASES_URL = "https://github.com/TechbeeAT/spectacled/releases"



@Composable
fun GitHubReleases(
    releases: List<GitHubRelease>,
    buildIsPreRelese: Boolean = buildIsPrerelease()
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

            itemsIndexed(releases.filter { if(!buildIsPreRelese) !it.prerelease else true }.take(5)) { index, release ->

                var isExpanded by remember { mutableStateOf(index == 0)}
                
                SpecialRoundedCard(
                    isFirst = index == 0,
                    isLast = index == releases.lastIndex,
                    onClick = {
                        isExpanded = !isExpanded
                        /*
                        try {
                            uriHandler.openUri(release.githubUrl)
                        } catch (e: Exception) {
                            Napier.w(e.stackTraceToString())
                        }

                         */
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Badge {
                                Text(text = release.releaseName)
                            }
                            if(release.prerelease)
                                Badge {
                                    Text("Pre-Release")
                                }
                        }

                        /*
                        Text(
                            text = release.releaseName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                         */
                        release.releaseText?.let {
                            if(isExpanded)
                                Markdown(content = it)
                            else
                                Text(text = it, maxLines = 5)
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
                            Napier.w(e.stackTraceToString())
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

    val sampleReleaseNotes = "## Welcome to Spectacled\n" +
            "\n" +
            "Spectacled is a family of open, standards-based apps for journals, notes and tasks. Your entries are stored as iCalendar and synced over CalDAV with a server you choose, such as Nextcloud, Radicale or any other CalDAV server. There is no proprietary backend and no account to create: your data stays on your own server.\n" +
            "\n" +
            "### Three apps, one family\n" +
            "\n" +
            "- **Spectacled Journals**: dated journal entries\n" +
            "- **Spectacled Notes**: free-form notes\n" +
            "- **Spectacled Tasks**: to-dos with status, priority and due dates\n"

    val releases = List(20) { index ->
        GitHubRelease(
            releaseName = "v1.0.$index",
            releaseText = sampleReleaseNotes,
            prerelease = index%3 > 0,
            githubUrl = "https://github.com"
        ) // every second and third is a prerelease
    }

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            GitHubReleases(
                releases = releases,
                buildIsPreRelese = false
            )
        }
    }
}


@Preview
@Composable
private fun GitHubReleases_with_Prereleases_Preview() {

    val sampleReleaseNotes = "## Welcome to Spectacled\n" +
            "\n" +
            "Spectacled is a family of open, standards-based apps for journals, notes and tasks. Your entries are stored as iCalendar and synced over CalDAV with a server you choose, such as Nextcloud, Radicale or any other CalDAV server. There is no proprietary backend and no account to create: your data stays on your own server.\n" +
            "\n" +
            "### Three apps, one family\n" +
            "\n" +
            "- **Spectacled Journals**: dated journal entries\n" +
            "- **Spectacled Notes**: free-form notes\n" +
            "- **Spectacled Tasks**: to-dos with status, priority and due dates\n\n" +
            "This is a beta release. Feedback is very welcome: open an [issue](https://github.com/TechbeeAT/spectacled/issues/new/choose) or write to [spectacled@techbee.at](mailto:spectacled@techbee.at).\n"

    val releases = List(20) { index ->
        GitHubRelease(
            releaseName = "v1.0.$index",
            releaseText = sampleReleaseNotes,
            prerelease = index%3 > 0,
            githubUrl = "https://github.com"
        )  // every second and third is a prerelease
    }

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            GitHubReleases(
                releases = releases,
                buildIsPreRelese = true
            )
        }
    }
}