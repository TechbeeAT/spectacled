package at.techbee.spectacled

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
                        onNavigate = { route -> navController.navigateIfNotOnTop(route) }
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

                    ListScreenRoot(
                        listViewModel = listViewModel,
                        onNavigate = { route -> navController.navigateIfNotOnTop(route) },
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

                    DetailsScreenRoot(
                        detailsViewModel = detailsViewModel,
                        onNavigate = { route -> navController.navigateIfNotOnTop(route) },
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

                    DetailsScreenRoot(
                        detailsViewModel = detailsViewModel,
                        onNavigate = { route -> navController.navigateIfNotOnTop(route) },
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
