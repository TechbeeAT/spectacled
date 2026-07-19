package at.techbee.spectacled


import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.account.presentation.AccountListViewModel
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.koin.sharedModule
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
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

    val accountListViewModel = koinViewModel<AccountListViewModel>()
    val listViewModel = koinViewModel<ListViewModel>()
    val detailsViewModel: DetailsViewModel = koinViewModel<DetailsViewModel>()


    AppTheme(
        spectacledVariant = spectacledVariant
    ) {

        //TODO: Check https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html#support-for-browser-navigation-in-web-apps for wasm
        val navController = rememberNavController()


        fun followRoute(route: Route) {
            when (route) {
                Route.AccountsList, Route.HomeGraph -> {
                    detailsViewModel.reset()
                    listViewModel.reset()
                }
                is Route.AddICalEntry -> {
                    if (route.copyFromId != null)
                        detailsViewModel.loadCopy(route.copyFromId)
                    else if (route.calendarId != 0L)
                        detailsViewModel.loadNew(route.calendarId, route.initialDescription)
                    else
                        detailsViewModel.prepareNew(route.initialDescription)
                }
                is Route.IcalEntryDetails -> {
                    detailsViewModel.load(route.icalEntryId)
                }
                is Route.IcalEntryList -> {
                    listViewModel.load(route.calendarId)
                }
            }

            // only executed when the navController is actually attached (portrait mode)
            try { navController.navigate(route) } catch (_: IllegalStateException) { }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

            // BoxWithConstraints observes the window size and will trigger a recomposition
            // whenever the orientation or size changes.
            BoxWithConstraints {
                val isLandscape = (maxWidth > maxHeight) || maxWidth > 700.dp  // large tablets have enough space to always show landscape layout

                if (isLandscape) {

                    LandscapeLayout(
                        accountListViewModel = accountListViewModel,
                        listViewModel = listViewModel,
                        detailsViewModel = detailsViewModel,
                        onNavigate = { route -> followRoute(route) },
                        onNavigateUp = {
                            if(detailsViewModel.state.value.isInitialized)
                                detailsViewModel.reset()
                            else if (listViewModel.state.value.isInitialized)
                                listViewModel.reset()
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        if (listViewModel.state.value.isInitialized)
                            followRoute(Route.IcalEntryList(listViewModel.state.value.calendar.id))
                        if(detailsViewModel.state.value.isInitialized)
                            followRoute(Route.IcalEntryDetails(detailsViewModel.state.value.icalEntry.id))
                    }

                    PortraitLayout(
                        navController = navController,
                        accountListViewModel = accountListViewModel,
                        listViewModel = listViewModel,
                        detailsViewModel = detailsViewModel,
                        startDestination = Route.AccountsList,
                        onCloseApp = onCloseApp
                    )
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
                userAppPreferencesStore.lastUsedCalendarId?.let { followRoute(Route.IcalEntryList(it)) }
            }
        }

        LaunchedEffect(DeepLinkHandler.deepLinkData) {
            val deepLinkData = DeepLinkHandler.deepLinkData

            // Handle reactive navigation for already open app (or late-arriving deep links on iOS)
            val newRoute = if (!deepLinkData.consumed) {
                if (deepLinkData.initialIcalEntryId != null) {
                    if (deepLinkData.initialIcalEntryId == 0L) {
                        Route.AddICalEntry(
                            calendarId = deepLinkData.initialCalendarId ?: 0L,
                            initialDescription = deepLinkData.initialIcalEntryDescription
                        )
                    } else {
                        Route.IcalEntryDetails(deepLinkData.initialIcalEntryId)
                    }
                } else if (deepLinkData.initialCalendarId != null) {
                    Route.IcalEntryList(deepLinkData.initialCalendarId)
                } else null
            } else null

            newRoute?.let {
                followRoute(it)
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
