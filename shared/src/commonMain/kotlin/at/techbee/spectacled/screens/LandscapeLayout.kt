package at.techbee.spectacled.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import at.techbee.spectacled.screens.account.presentation.AccountListScreenRoot
import at.techbee.spectacled.screens.account.presentation.AccountListViewModel
import at.techbee.spectacled.screens.details.presentation.DetailsScreenRoot
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
import at.techbee.spectacled.screens.list.presentation.ListScreenRoot
import at.techbee.spectacled.screens.list.presentation.ListViewModel

/**
 * Multi-pane presentation for wide windows. Renders accounts, list and details
 * side by side from the shared view models. It holds no navigation state itself
 * and knows nothing about `NavController`: navigation flows through [onNavigate]
 * / [onNavigateUp] into the shared `AppNavigator`, and the list / details panes
 * simply appear once their view model reports `isInitialized`.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Row(modifier = modifier) {
        AccountListScreenRoot(
            viewModel = accountListViewModel,
            onNavigate = onNavigate,
            modifier = Modifier.weight(0.2f)
        )

        VerticalDivider()

        if (listState.isInitialized) {
            ListScreenRoot(
                listViewModel = listViewModel,
                onNavigate = onNavigate,
                // Closing the list from this pane returns to just the accounts
                // pane, which also clears any open details.
                onNavigateUp = { onNavigate(Route.AccountsList) },
                modifier = Modifier.weight(0.4f)
            )
        }

        VerticalDivider()

        if (detailsState.isInitialized) {
            DetailsScreenRoot(
                detailsViewModel = detailsViewModel,
                onNavigate = onNavigate,
                onNavigateUp = onNavigateUp,
                modifier = Modifier.weight(0.4f)
            )
        }
    }
}
