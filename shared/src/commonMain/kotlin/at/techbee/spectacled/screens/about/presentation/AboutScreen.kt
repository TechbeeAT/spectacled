package at.techbee.spectacled.screens.about.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: AboutViewModel,
    modifier: Modifier = Modifier
) {

    val screens = listOf(
        AboutTabDestination.Jtx,
        AboutTabDestination.Releasenotes,
        AboutTabDestination.Contributors,
        AboutTabDestination.Libraries
    )

    val pagerState = rememberPagerState(initialPage = screens.indexOf(AboutTabDestination.Jtx), pageCount = { screens.size })
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
                screens.indexOf(AboutTabDestination.Jtx) -> AboutApp(viewModel.spectacledVariant)
                screens.indexOf(AboutTabDestination.Releasenotes) -> GitHubReleasesRoot(viewModel)
                screens.indexOf(AboutTabDestination.Contributors) -> GitHubContributorsRoot(viewModel)
                screens.indexOf(AboutTabDestination.Libraries) -> AboutLibrariesRoot(viewModel)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AboutScreen_Preview() {
    AboutScreen(
        AboutViewModel(
            spectacledVariant = SpectacledVariant.JOURNALS)
    )
}



