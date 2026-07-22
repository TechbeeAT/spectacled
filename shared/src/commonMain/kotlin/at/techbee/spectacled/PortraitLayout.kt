package at.techbee.spectacled

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
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

                    // BoxWithConstraints observes the window size and will trigger a recomposition
                    // whenever the orientation or size changes.
                    BoxWithConstraints {
                        val isLandscape = (maxWidth > maxHeight) || maxWidth > 700.dp  // large tablets have enough space to always show landscape layout

                        Row(modifier = Modifier.fillMaxSize()) {

                            if(isLandscape) {
                                AccountListScreenRoot(
                                    viewModel = accountListViewModel,
                                    onNavigate = { route -> try { navController.navigate(route) { launchSingleTop = true } } catch (_: IllegalStateException) { } },
                                    removeSafeAreaPaddingValues = isLandscape,
                                    modifier = Modifier.weight(0.4f)
                                )

                                VerticalDivider()
                            }


                            ListScreenRoot(
                                listViewModel = listViewModel,
                                onNavigate = { route -> try { navController.navigate(route) { launchSingleTop = true } } catch (_: IllegalStateException) { } },
                                onNavigateUp = {
                                    if (!navController.popBackStack())
                                        onCloseApp()
                                },
                                removeSafeAreaPaddingValues = isLandscape,
                                modifier = Modifier.weight(if(isLandscape) 0.6f else 1.0f)
                            )
                        }

                    }
                }

                composable<Route.IcalEntryDetails> { args ->
                    val icalEntryId = args.toRoute<Route.IcalEntryDetails>().icalEntryId

                    val copyFromId = args.toRoute<Route.IcalEntryDetails>().newIcalEntryCopyFromId
                    val calendarId = args.toRoute<Route.IcalEntryDetails>().newIcalEntryCalendarId
                    val initialDescription = args.toRoute<Route.IcalEntryDetails>().newIcalEntryInitialDescription

                    LaunchedEffect(icalEntryId, copyFromId, calendarId, initialDescription) {
                        if (copyFromId != null)
                            detailsViewModel.loadCopy(copyFromId)
                        else if (calendarId != 0L)
                            detailsViewModel.loadNew(calendarId, initialDescription)
                        else if (initialDescription != null || icalEntryId == 0L)
                            detailsViewModel.prepareNew(initialDescription)
                        else if(detailsViewModel.state.value.icalEntry.id != icalEntryId)
                            detailsViewModel.load(icalEntryId)
                    }

                    // BoxWithConstraints observes the window size and will trigger a recomposition
                    // whenever the orientation or size changes.
                    BoxWithConstraints {
                        val isLandscape =
                            (maxWidth > maxHeight) || maxWidth > 700.dp  // large tablets have enough space to always show landscape layout

                        Row(modifier = Modifier.fillMaxSize()) {

                            if (isLandscape) {
                                ListScreenRoot(
                                    listViewModel = listViewModel,
                                    onNavigate = { route ->
                                        try {
                                            navController.navigate(route) { launchSingleTop = true }
                                        } catch (_: IllegalStateException) {
                                        }
                                    },
                                    onNavigateUp = {
                                        if (!navController.popBackStack())
                                            onCloseApp()
                                    },
                                    removeSafeAreaPaddingValues = isLandscape,
                                    modifier = Modifier.weight(0.4f)
                                )

                                VerticalDivider()
                            }

                            DetailsScreenRoot(
                                detailsViewModel = detailsViewModel,
                                // No launchSingleTop here: opening a linked entry (subtask) from a detail
                                // view should stack a new detail screen so Back returns to the parent,
                                // rather than replacing it.
                                onNavigate = { route -> navController.navigate(route) },
                                onNavigateUp = {
                                    // close the app if the list was skipped on opening
                                    if (!listViewModel.state.value.isInitialized)
                                        onCloseApp()
                                    else if (!navController.popBackStack())
                                        onCloseApp()
                                },
                                removeSafeAreaPaddingValues = isLandscape,
                                modifier = Modifier.weight(if (isLandscape) 0.6f else 1.0f)
                            )
                        }
                    }
                }
            }
        }
    }
}
