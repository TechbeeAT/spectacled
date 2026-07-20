package at.techbee.spectacled

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.account.presentation.AccountListScreenRoot
import at.techbee.spectacled.screens.account.presentation.AccountListViewModel
import at.techbee.spectacled.screens.details.presentation.DetailsScreenRoot
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
import at.techbee.spectacled.screens.list.presentation.ListScreenRoot
import at.techbee.spectacled.screens.list.presentation.ListViewModel


@Composable
fun LandscapeLayout(
    accountListViewModel: AccountListViewModel,
    listViewModel: ListViewModel,
    detailsViewModel: DetailsViewModel,
    onNavigate: (Route) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState by listViewModel.state.collectAsState()
    val detailsState by detailsViewModel.state.collectAsState()

    // reset the details state if the calendar was changed
    LaunchedEffect(listState.calendar) {
        if(detailsState.icalEntry.calendarId != listState.calendar.id)
            detailsViewModel.reset()
    }

    // The panes drop the horizontal safe-area insets via removeSafeAreaPaddingValues so they can
    // fill the sides edge-to-edge. This must stay explicit per screen: a single
    // consumeWindowInsets() on this Row does not work reliably on iOS, because there every
    // WindowInsets read creates a fresh instance, which resets Scaffold's
    // remember(contentWindowInsets) state on each recomposition of a pane while the consumption
    // callback only fires again when the consumed insets change - the safe-area padding then
    // reappears as side gaps (observed on iOS; Android caches the instances and is unaffected).
    Row(modifier = modifier) {
        if(!detailsState.isInitialized) {
            AccountListScreenRoot(
                viewModel = accountListViewModel,
                onNavigate = onNavigate,
                removeSafeAreaPaddingValues = true,
                modifier = Modifier.weight(0.4f)
            )
        }

        VerticalDivider()

        if(listState.isInitialized) {
            ListScreenRoot(
                listViewModel = listViewModel,
                onNavigate = onNavigate,
                onNavigateUp = onNavigateUp,
                removeSafeAreaPaddingValues = true,
                modifier = Modifier.weight(if(detailsState.isInitialized) 0.4f else 0.6f)

            )
        }

        VerticalDivider()

        if(detailsState.isInitialized) {
            DetailsScreenRoot(
                detailsViewModel = detailsViewModel,
                onNavigate = onNavigate,
                onNavigateUp = onNavigateUp,
                removeSafeAreaPaddingValues = true,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}
