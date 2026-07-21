package at.techbee.spectacled

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.hasRoute
import androidx.navigation.toRoute
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.account.presentation.AccountListScreenRoot
import at.techbee.spectacled.screens.account.presentation.AccountListViewModel
import at.techbee.spectacled.screens.details.presentation.DetailsScreenRoot
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
import at.techbee.spectacled.screens.list.presentation.ListScreenRoot
import at.techbee.spectacled.screens.list.presentation.ListViewModel

@Composable
fun PortraitLayout(
    navController: NavHostController,
    accountListViewModel: AccountListViewModel,
    listViewModel: ListViewModel,
    detailsViewModel: DetailsViewModel,
    startDestination: Route,
    onCloseApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.HomeGraph
        ) {
            navigation<Route.HomeGraph>(startDestination) {

                composable<Route.AccountsList> {
                    AccountListScreenRoot(
                        viewModel = accountListViewModel,
                        onNavigate = { route -> try { navController.navigate(route) { launchSingleTop = true } } catch (_: IllegalStateException) { } }
                    )
                }

                composable<Route.IcalEntryList>(
                    enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
                    exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth } },
                    popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth } },
                    popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } }
                ) { args ->

                    val calendarId = args.toRoute<Route.IcalEntryList>().calendarId

                    LaunchedEffect(calendarId) {
                        listViewModel.load(calendarId)
                    }

                    // Reset the list view-model once no list destination remains on the back stack,
                    // i.e. this screen was popped — not merely navigated past (details still above
                    // it) or kept around by a layout switch (the retained navController still lists
                    // it). This keeps the landscape layout, which shows panes from view-model state,
                    // consistent with what portrait's back stack actually holds.
                    DisposableEffect(Unit) {
                        onDispose {
                            if (navController.currentBackStack.value.none { it.destination.hasRoute<Route.IcalEntryList>() })
                                listViewModel.reset()
                        }
                    }

                    ListScreenRoot(
                        listViewModel = listViewModel,
                        onNavigate = { route -> try { navController.navigate(route) { launchSingleTop = true } } catch (_: IllegalStateException) { } },
                        onNavigateUp = {
                            if (!navController.popBackStack())
                                onCloseApp()
                        }
                    )
                }

                composable<Route.IcalEntryDetails> { args ->
                    val icalEntryId = args.toRoute<Route.IcalEntryDetails>().icalEntryId

                    LaunchedEffect(icalEntryId) {
                        if(detailsViewModel.state.value.icalEntry.id != icalEntryId)
                            detailsViewModel.load(icalEntryId)
                    }

                    // Reset the (shared) details view-model once no details/add destination remains
                    // on the back stack — i.e. this screen was popped, not navigated past (a subtask
                    // still above it) or kept by a layout switch. Placed before DetailsScreenRoot so
                    // it disposes after that screen's sync-on-dispose (effects dispose LIFO). Keeps
                    // the landscape layout, which shows panes from view-model state, consistent.
                    DisposableEffect(Unit) {
                        onDispose {
                            if (navController.currentBackStack.value.none {
                                    it.destination.hasRoute<Route.IcalEntryDetails>() || it.destination.hasRoute<Route.AddICalEntry>()
                                })
                                detailsViewModel.reset()
                        }
                    }

                    DetailsScreenRoot(
                        detailsViewModel = detailsViewModel,
                        // No launchSingleTop here: opening a linked entry (subtask) from a detail
                        // view should stack a new detail screen so Back returns to the parent,
                        // rather than replacing it.
                        onNavigate = { route -> navController.navigate(route) },
                        onNavigateUp = {
                            // close the app if the list was skipped on opening
                            if(!listViewModel.state.value.isInitialized)
                                onCloseApp()
                            else if (!navController.popBackStack())
                                onCloseApp()
                        }
                    )
                }

                composable<Route.AddICalEntry> { args ->
                    val copyFromId = args.toRoute<Route.AddICalEntry>().copyFromId
                    val calendarId = args.toRoute<Route.AddICalEntry>().calendarId
                    val initialDescription = args.toRoute<Route.AddICalEntry>().initialDescription

                    LaunchedEffect(copyFromId, calendarId, initialDescription) {
                        if (copyFromId != null)
                            detailsViewModel.loadCopy(copyFromId)
                        else if (calendarId != 0L)
                            detailsViewModel.loadNew(calendarId, initialDescription)
                        else
                            detailsViewModel.prepareNew(initialDescription)
                    }

                    // Reset the (shared) details view-model once no details/add destination remains
                    // on the back stack — i.e. this screen was popped, not navigated past (a subtask
                    // still above it) or kept by a layout switch. Placed before DetailsScreenRoot so
                    // it disposes after that screen's sync-on-dispose (effects dispose LIFO). Keeps
                    // the landscape layout, which shows panes from view-model state, consistent.
                    DisposableEffect(Unit) {
                        onDispose {
                            if (navController.currentBackStack.value.none {
                                    it.destination.hasRoute<Route.IcalEntryDetails>() || it.destination.hasRoute<Route.AddICalEntry>()
                                })
                                detailsViewModel.reset()
                        }
                    }

                    DetailsScreenRoot(
                        detailsViewModel = detailsViewModel,
                        onNavigate = { route -> try { navController.navigate(route) { launchSingleTop = true } } catch (_: IllegalStateException) { } },
                        onNavigateUp = {
                            // close the app if the list was skipped on opening
                            if(!listViewModel.state.value.isInitialized)
                                onCloseApp()
                            else if (!navController.popBackStack())
                                onCloseApp()
                        }
                    )
                }
            }
        }
    }
}
