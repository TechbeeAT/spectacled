package at.techbee.spectacled.screens.about.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.about.domain.GitHubContributor
import at.techbee.spectacled.screens.core.presentation.components.SpecialRoundedCard
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.about_contributors
import spectacled.shared.generated.resources.more_on_github


//TODO: Change for Release!!!
private const val GITHUB_CONTRIBUTORS_URL = "https://github.com/TechbeeAT/jtxBoard/graphs/contributors"

@Composable
fun GitHubContributorsRoot(
    viewModel: AboutViewModel
) {
    GitHubContributors(viewModel.state.gitHubContributors)
}

@Composable
fun GitHubContributors(
    contributors: List<GitHubContributor>
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(Res.string.about_contributors),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {

            itemsIndexed(contributors) { index, contributor ->
                SpecialRoundedCard(
                    isFirst = index == 0,
                    isLast = index == contributors.lastIndex,
                    onClick = {
                        try {
                            contributor.url?.let { uriHandler.openUri(it) }
                        } catch (e: Exception) {
                            println(e.stackTraceToString())
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        AsyncImage(
                            model = contributor.avatarUrl,
                            contentDescription = null,
                            placeholder = rememberVectorPainter(Icons.Outlined.Person),
                            error = rememberVectorPainter(Icons.Outlined.Person),
                            fallback = rememberVectorPainter(Icons.Outlined.Person),
                            modifier = Modifier.size(80.dp).clip(CircleShape).shadow(2.dp)
                        )

                        Text(
                            text = contributor.login,
                            style = MaterialTheme.typography.titleSmall
                        )

                    }
                }
            }

            item {
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(GITHUB_CONTRIBUTORS_URL)
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
private fun GitHubContributors_Preview() {

    val contributors = mutableListOf<GitHubContributor>().apply {
        add(GitHubContributor.getSample())
        add(GitHubContributor.getSample())
        add(GitHubContributor.getSample())
        add(GitHubContributor.getSample())
        add(GitHubContributor.getSample())
        add(GitHubContributor.getSample())
    }
    GitHubContributors(contributors)
}