package at.techbee.spectacled.screens

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.backhandler.BackHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import at.techbee.spectacled.screens.account.presentation.AccountListScreenRoot
import at.techbee.spectacled.screens.account.presentation.AccountListViewModel
import at.techbee.spectacled.screens.core.navigation.AppNavigator
import at.techbee.spectacled.screens.details.presentation.DetailsScreenRoot
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
import at.techbee.spectacled.screens.list.presentation.ListScreenRoot
import at.techbee.spectacled.screens.list.presentation.ListViewModel

/**
 * Single-pane presentation for tall windows. This is the only place that knows
 * about `NavController`: the controller is a pure projection of
 * [appNavigator]'s back stack (the single writer), so switching to/from the
 * landscape layout never loses the open screen. View models are loaded centrally
 * by the caller, so the destinations here only render the shared instances.
 */
@Composable
fun PortraitLayout(
    appNavigator: AppNavigator,
    accountListViewModel: AccountListViewModel,
    listViewModel: ListViewModel,
    detailsViewModel: DetailsViewModel,
    onNavigateUp: () -> Unit
) {
    val navController = rememberNavController()

    // System back is routed through the shared AppNavigator so it stays the sole
    // writer; the NavController is reconciled to match below. Disabled at the root
    // so the platform default (e.g. finishing the app) still applies.
    BackHandler(enabled = appNavigator.canNavigateUp) { onNavigateUp() }

    // Project the AppNavigator back stack onto the NavController. The stack is
    // shallow, so only the delta is applied (pop the divergent tail, push the
    // rest). On (re)entering the portrait layout the NavController starts at the
    // NavHost start destination, so `applied` starts there too and the whole
    // stack is rebuilt from the AppNavigator.
    LaunchedEffect(navController) {
        var applied = listOf<Route>(Route.AccountsList)
        snapshotFlow { appNavigator.backStack.toList() }.collect { target ->
            val common = applied.zip(target).takeWhile { (a, b) -> a == b }.size
            repeat((applied.size - common).coerceAtLeast(0)) { navController.popBackStack() }
            target.drop(common).forEach { navController.navigate(it) }
            applied = target
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.HomeGraph
    ) {
        navigation<Route.HomeGraph>(startDestination = Route.AccountsList) {

            composable<Route.AccountsList> {
                AccountListScreenRoot(
                    viewModel = accountListViewModel,
                    onNavigate = appNavigator::navigate
                )
            }

            composable<Route.IcalEntryList>(
                enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
                exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth } },
                popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth } },
                popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } }
            ) {
                ListScreenRoot(
                    listViewModel = listViewModel,
                    onNavigate = appNavigator::navigate,
                    onNavigateUp = onNavigateUp
                )
            }

            composable<Route.IcalEntryDetails> {
                DetailsScreenRoot(
                    detailsViewModel = detailsViewModel,
                    onNavigate = appNavigator::navigate,
                    onNavigateUp = onNavigateUp
                )
            }

            composable<Route.AddICalEntry> {
                DetailsScreenRoot(
                    detailsViewModel = detailsViewModel,
                    onNavigate = appNavigator::navigate,
                    onNavigateUp = onNavigateUp
                )
            }
        }
    }
}
