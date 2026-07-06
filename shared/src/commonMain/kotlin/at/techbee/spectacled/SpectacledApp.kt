package at.techbee.spectacled


import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
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
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.koin.sharedModule
import at.techbee.spectacled.screens.details.presentation.DetailsScreenRoot
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
import at.techbee.spectacled.screens.list.presentation.ListScreenRoot
import at.techbee.spectacled.screens.list.presentation.ListViewModel
import at.techbee.spectacled.theme.AppTheme
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.app_name_spectacled_journals
import spectacled.shared.generated.resources.app_name_spectacled_notes
import spectacled.shared.generated.resources.app_name_spectacled_tasks
import spectacled.shared.generated.resources.logo_spectacled_journals
import spectacled.shared.generated.resources.logo_spectacled_notes
import spectacled.shared.generated.resources.logo_spectacled_tasks
import kotlin.time.ExperimentalTime


enum class SpectacledVariant(
    val dbName: String,
    val appNameStringRes: StringResource,
    val logoDrawableResource: DrawableResource,
    val mainCalendarComponent: CalendarComponent,
    val themeSeedColor: Color,
    val deeplinkUriScheme: String,
    val deeplinkWebUri: String
) {

    JOURNALS(
        "spectacled_journals.db",
        Res.string.app_name_spectacled_journals,
        Res.drawable.logo_spectacled_journals,
        CalendarComponent.VJOURNAL,
        Color(0, 104, 150),
        "spectacled-journals",
        "https://spectacled.techbee.at/journals"
    ),
    NOTES(
        "spectacled_notes.db",
        Res.string.app_name_spectacled_notes,
        Res.drawable.logo_spectacled_notes,
        CalendarComponent.VJOURNAL,
        Color(153, 76, 44),
        "spectacled-notes",
        "https://spectacled.techbee.at/notes"

    ),
    TASKS(
        "spectacled_tasks.db",
        Res.string.app_name_spectacled_tasks,
        Res.drawable.logo_spectacled_tasks,
        CalendarComponent.VTODO,
        Color(41, 111, 35),
        "spectacled-tasks",
        "https://spectacled.techbee.at/tasks"
    );
}

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

    AppTheme(
        spectacledVariant = spectacledVariant
    ) {

        //TODO: Check https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html#support-for-browser-navigation-in-web-apps for wasm
        val navController = rememberNavController()

        // 1. Stable start destination for the graph life-cycle
        val initialData = remember { DeepLinkHandler.deepLinkData }
        val startDestination = remember(initialData) {
            if (initialData.consumed) {
                Route.AccountsList
            } else if (initialData.initialIcalEntryId != null) {
                if (initialData.initialIcalEntryId == 0L)
                    Route.AddICalEntry(calendarId = initialData.initialCalendarId ?: 0L, initialDescription = initialData.initialIcalEntryDescription)
                else
                    Route.IcalEntryDetails(initialData.initialIcalEntryId)
            } else if (initialData.initialCalendarId != null) {
                Route.IcalEntryList(initialData.initialCalendarId)
            } else {
                Route.AccountsList
            }
        }
        
        // 2. Mark initial deep link as consumed once the graph is set up
        if (startDestination !is Route.AccountsList) {
            LaunchedEffect(Unit) {
                DeepLinkHandler.consume()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = Route.HomeGraph
            ) {
                navigation<Route.HomeGraph>(startDestination) {

                    composable<Route.AccountsList> {
                        AccountListScreenRoot(
                            viewModel = koinViewModel<AccountListViewModel>(),
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }

                    composable<Route.IcalEntryList>(
                        enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
                        exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth } },
                        popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth } },
                        popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } }
                    ) { args ->

                        val listViewModel = koinViewModel<ListViewModel>()
                        val calendarId = args.toRoute<Route.IcalEntryList>().calendarId

                        LaunchedEffect(calendarId) {
                            listViewModel.load(calendarId)
                        }

                        ListScreenRoot(
                            listViewModel = listViewModel,
                            onNavigate = { route -> navController.navigate(route) },
                            onNavigateUp = {
                                if (!navController.popBackStack())
                                    onCloseApp()
                            }
                        )
                    }

                    composable<Route.IcalEntryDetails> { args ->
                        val icalEntryId = args.toRoute<Route.IcalEntryDetails>().icalEntryId
                        val detailsViewModel: DetailsViewModel = koinViewModel<DetailsViewModel>()

                        LaunchedEffect(icalEntryId) {
                            detailsViewModel.load(icalEntryId)
                        }

                        DetailsScreenRoot(
                            detailsViewModel = detailsViewModel,
                            onNavigate = { route -> navController.navigate(route) },
                            onNavigateUp = {
                                if (!navController.popBackStack()) {
                                    onCloseApp()
                                }
                            }
                        )
                    }

                    composable<Route.AddICalEntry> { args ->
                        val copyFromId = args.toRoute<Route.AddICalEntry>().copyFromId
                        val calendarId = args.toRoute<Route.AddICalEntry>().calendarId
                        val initialDescription = args.toRoute<Route.AddICalEntry>().initialDescription

                        val detailsViewModel: DetailsViewModel = koinViewModel<DetailsViewModel>()

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
                            onNavigate = { route -> navController.navigate(route) },
                            onNavigateUp = {
                                if (!navController.popBackStack()) {
                                    onCloseApp()
                                }
                            }
                        )
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            syncTrigger.schedulePeriodic()
            syncTrigger.requestImmediate()

            // 3. Robust auto-navigation: Only move to last used calendar if NO deep link was ever seen
            if (DeepLinkHandler.deepLinkData.isEmpty()) {
                val lastUsedId = userAppPreferencesStore.lastUsedCalendarId
                // Re-check isEmpty() after potential async loading of preferences
                if (lastUsedId != null && DeepLinkHandler.deepLinkData.isEmpty()) {
                    navController.navigate(Route.IcalEntryList(lastUsedId))
                }
            }
        }

        // Handle reactive navigation for already open app (or late-arriving deep links on iOS)
        LaunchedEffect(DeepLinkHandler.deepLinkData) {
            val deepLinkData = DeepLinkHandler.deepLinkData
            if (!deepLinkData.consumed) {
                if (deepLinkData.initialIcalEntryId != null) {
                    if (deepLinkData.initialIcalEntryId == 0L) {
                        navController.navigate(
                            Route.AddICalEntry(
                                calendarId = deepLinkData.initialCalendarId ?: 0L,
                                initialDescription = deepLinkData.initialIcalEntryDescription
                            )
                        )
                    } else {
                        navController.navigate(Route.IcalEntryDetails(deepLinkData.initialIcalEntryId))
                    }
                    DeepLinkHandler.consume()
                } else if (deepLinkData.initialCalendarId != null) {
                    navController.navigate(Route.IcalEntryList(deepLinkData.initialCalendarId))
                    DeepLinkHandler.consume()
                }
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
