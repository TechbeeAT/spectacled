package at.techbee.spectacled


import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.account.presentation.AccountListScreenRoot
import at.techbee.spectacled.screens.account.presentation.AccountListViewModel
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.koin.sharedModule
import at.techbee.spectacled.screens.details.presentation.DetailsScreenRoot
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
import at.techbee.spectacled.screens.list.presentation.ListScreenRoot
import at.techbee.spectacled.screens.list.presentation.ListViewModel
import at.techbee.spectacled.theme.AppTheme
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.time.ExperimentalTime


var isNapierInitialized = false

fun doInitKoin(spectacledVariant: SpectacledVariant) {
    if (!isNapierInitialized) {
        Napier.base(DebugAntilog())
        isNapierInitialized = true
    }

    if (KoinPlatform.getKoinOrNull() == null) {
        startKoin {
            modules(
                module { single { spectacledVariant } },
                sharedModule
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun SpectacledApp(
    spectacledVariant: SpectacledVariant,
    onCloseApp: () -> Unit = {}
) {
    doInitKoin(spectacledVariant)

    val syncTrigger = koinInject<PlatformSyncTrigger>()
    val userAppPreferencesStore = koinInject<PlatformUserAppPreferencesStore>()
    val calendarRepository = koinInject<CalendarRepository>()

    val accountListViewModel = koinViewModel<AccountListViewModel>()
    val listViewModel = koinViewModel<ListViewModel>()
    val detailsViewModel: DetailsViewModel = koinViewModel<DetailsViewModel>()


    AppTheme(
        spectacledVariant = spectacledVariant
    ) {

        //TODO: Check https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html#support-for-browser-navigation-in-web-apps for wasm
        val navController = rememberNavController()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

            NavHost(
                navController = navController,
                startDestination = Route.HomeGraph
            ) {
                navigation<Route.HomeGraph>(Route.AccountsList) {

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
                        val calendarId = args.toRoute<Route.IcalEntryDetails>().newIcalEntryCalendarId
                        val initialDescription = args.toRoute<Route.IcalEntryDetails>().newIcalEntryInitialDescription

                        LaunchedEffect(icalEntryId, calendarId, initialDescription) {
                            if (initialDescription != null || icalEntryId == 0L)
                                detailsViewModel.loadNew(calendarId = calendarId, initialDescription = initialDescription)
                            else if(detailsViewModel.state.value.icalEntry.id != icalEntryId)
                                detailsViewModel.load(icalEntryId)
                        }

                        // BoxWithConstraints observes the window size and will trigger a recomposition
                        // whenever the orientation or size changes.
                        BoxWithConstraints {
                            val isLandscape = (maxWidth > maxHeight) || maxWidth > 700.dp  // large tablets have enough space to always show landscape layout

                            Row(modifier = Modifier.fillMaxSize()) {

                                val listState by listViewModel.state.collectAsState()
                                val detailsState by detailsViewModel.state.collectAsState()

                                LaunchedEffect(detailsState.calendar) {
                                    detailsState.calendar?.let {
                                        if(listState.calendar.id != it.id)   // calendar was loaded, load also in list if different
                                            listViewModel.load(it.id)
                                    }
                                }

                                // show list for landscape mode only and only when initialized
                                if (isLandscape && listState.isInitialized && listState.calendar.id == detailsState.calendar?.id) {

                                    ListScreenRoot(
                                        listViewModel = listViewModel,
                                        onNavigate = { route ->
                                            try { navController.navigate(route) { launchSingleTop = true } } catch (_: IllegalStateException) { }
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

        LaunchedEffect(Unit) {
            syncTrigger.schedulePeriodic()
            syncTrigger.requestImmediate()
        }

        LaunchedEffect(Unit) {
            //Only move to last used calendar if NO deep link was ever seen
            if (DeepLinkHandler.deepLinkData.isEmpty()) {
                userAppPreferencesStore.lastUsedCalendarId?.let { calendarId ->
                    // Only navigate if the calendar still exists. It may have been deleted
                    // (account removed/unsubscribed, or dropped by a sync) since it was last
                    // used, in which case we must not open the list view of a phantom calendar.
                    if (calendarRepository.getCalendarById(calendarId) != null)
                        navController.navigate(Route.IcalEntryList(calendarId))
                    else
                        userAppPreferencesStore.lastUsedCalendarId = null
                }
            }
        }

        LaunchedEffect(DeepLinkHandler.deepLinkData) {
            val deepLinkData = DeepLinkHandler.deepLinkData

            // Handle reactive navigation for already open app (or late-arriving deep links on iOS)
            val newRoute = if (!deepLinkData.isEmpty()) {
                if (deepLinkData.initialIcalEntryId != null) {
                    if (deepLinkData.initialIcalEntryId == 0L) {
                        Route.IcalEntryDetails(
                            icalEntryId = 0L,
                            newIcalEntryCalendarId = deepLinkData.initialCalendarId ?: 0L,
                            newIcalEntryInitialDescription = deepLinkData.initialIcalEntryDescription
                        )
                    } else {
                        Route.IcalEntryDetails(deepLinkData.initialIcalEntryId)
                    }
                } else if (deepLinkData.initialCalendarId != null) {
                    Route.IcalEntryList(deepLinkData.initialCalendarId)
                } else null
            } else null

            newRoute?.let {
                navController.navigate(it)
                DeepLinkHandler.consume()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
@Preview
private fun SpectacledApp_Preview() {
    val spectacledVariant = SpectacledVariant.NOTES
    AppTheme(spectacledVariant = spectacledVariant) {
        SpectacledApp(spectacledVariant = spectacledVariant)
    }
}
