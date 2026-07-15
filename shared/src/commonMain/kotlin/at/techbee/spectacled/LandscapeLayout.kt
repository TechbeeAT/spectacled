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

    Row(modifier = modifier) {
        if(!detailsState.isInitialized) {
            AccountListScreenRoot(
                viewModel = accountListViewModel,
                onNavigate = onNavigate,
                keepSafeAreaPaddingValues = false,
                modifier = Modifier.weight(0.4f)
            )
        }

        VerticalDivider()

        if(listState.isInitialized) {
            ListScreenRoot(
                listViewModel = listViewModel,
                onNavigate = onNavigate,
                onNavigateUp = onNavigateUp,
                keepSafeAreaPaddingValues = false,
                modifier = Modifier.weight(if(detailsState.isInitialized) 0.4f else 0.6f)

            )
        }

        VerticalDivider()

        if(detailsState.isInitialized) {
            DetailsScreenRoot(
                detailsViewModel = detailsViewModel,
                onNavigate = onNavigate,
                onNavigateUp = onNavigateUp,
                keepSafeAreaPaddingValues = false,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}
