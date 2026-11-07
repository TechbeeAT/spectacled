package at.techbee.spectacled.screens.about.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutScreenRoot(
    viewModel: AboutViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    AboutScreen(
        state = state,
        spectacledVariant = viewModel.spectacledVariant,
        modifier = modifier
    )

}


@Composable
fun AboutScreen(
    state: AboutState,
    spectacledVariant: SpectacledVariant,
    modifier: Modifier = Modifier
) {
    val screens = listOf(
        AboutTabDestination.General,
        AboutTabDestination.Sponsor,
        AboutTabDestination.ReleaseNotes,
        AboutTabDestination.Contributors,
        AboutTabDestination.Libraries
    )

    val pagerState = rememberPagerState(initialPage = screens.indexOf(AboutTabDestination.General), pageCount = { screens.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            screens.forEachIndexed { index, screen ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Icon(
                            screen.icon,
                            stringResource(screen.titleResource)
                        )
                    })
            }
        }

        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.weight(1f).padding(8.dp)
        ) { page ->
            when (page) {
                screens.indexOf(AboutTabDestination.General) -> AboutApp(spectacledVariant)
                screens.indexOf(AboutTabDestination.Sponsor) -> Donate()
                screens.indexOf(AboutTabDestination.ReleaseNotes) -> GitHubReleases(state.gitHubReleases)
                screens.indexOf(AboutTabDestination.Contributors) -> GitHubContributors(state.gitHubContributors)
                screens.indexOf(AboutTabDestination.Libraries) -> AboutLibraries(state.libraries)
            }
        }
    }
}


@Preview
@Composable
private fun AboutScreen_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            AboutScreen(
                state = AboutState(),
                spectacledVariant = SpectacledVariant.JOURNALS
            )
        }
    }
}



