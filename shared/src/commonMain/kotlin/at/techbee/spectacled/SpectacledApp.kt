package at.techbee.spectacled


import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.getPlatform
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
    val syncCalendarComponent: CalendarComponent,
    val themeSeedColor: Color,
) {

    JOURNALS(
        "spectacled_journals.db",
        Res.string.app_name_spectacled_journals,
        Res.drawable.logo_spectacled_journals,
        CalendarComponent.VJOURNAL,
        Color(0, 104, 150)
    ),
    NOTES(
        "spectacled_notes.db",
        Res.string.app_name_spectacled_notes,
        Res.drawable.logo_spectacled_notes,
        CalendarComponent.VJOURNAL,
        Color(153, 76, 44)
    ),
    TASKS(
        "spectacled_tasks.db",
        Res.string.app_name_spectacled_tasks,
        Res.drawable.logo_spectacled_tasks,
        CalendarComponent.VTODO,
        Color(41, 111, 35)
    );
}

fun doInitKoin(spectacledVariant: SpectacledVariant) {
    if (KoinPlatform.getKoinOrNull() == null) {
        Napier.base(DebugAntilog())
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
    initialCalendarId: Long? = null,
    initialIcalEntryId: Long? = null,
    initialIcalEntryDescription: String? = null,
    onCloseApp: () -> Unit = {}
) {
    doInitKoin(spectacledVariant)

    val syncTrigger = koinInject<PlatformSyncTrigger>()
    val userAppPreferencesStore = koinInject<PlatformUserAppPreferencesStore>()

    AppTheme(
        spectacledVariant = spectacledVariant
    ) {

        val navController = rememberNavController()
        //TODO: Check https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html#support-for-browser-navigation-in-web-apps for wasm

        if(getPlatform().platform == Platforms.IOS || getPlatform().platform == Platforms.DESKTOP) {
            // make sure deeplinks are also handled when they arrive after the app was started (especially for iOS and Desktop)
            val deepLinkCalendarId = DeepLinkHandler.initialCalendarId ?: initialCalendarId
            val deepLinkIcalEntryId = DeepLinkHandler.initialIcalEntryId ?: initialIcalEntryId
            val deepLinkDescription = DeepLinkHandler.initialIcalEntryDescription ?: initialIcalEntryDescription

            LaunchedEffect(deepLinkIcalEntryId, deepLinkCalendarId, deepLinkDescription) {
                if (deepLinkIcalEntryId != null) {
                    if (deepLinkIcalEntryId == 0L) {
                        navController.navigate(
                            Route.AddICalEntry(
                                calendarId = deepLinkCalendarId ?: 0L,
                                initialDescription = deepLinkDescription
                            )
                        )
                    } else {
                        navController.navigate(Route.IcalEntryDetails(deepLinkIcalEntryId))
                    }
                    DeepLinkHandler.onDeepLinkReceived(null, null, null)
                } else if (deepLinkCalendarId != null) {
                    navController.navigate(Route.IcalEntryList(deepLinkCalendarId))
                    DeepLinkHandler.onDeepLinkReceived(null, null, null)
                }
            }
        }


        val startDestination =
            if (initialIcalEntryId != null) {
                if (initialIcalEntryId == 0L)
                    Route.AddICalEntry(calendarId = initialCalendarId ?: 0L, initialDescription = initialIcalEntryDescription)
                else
                    Route.IcalEntryDetails(initialIcalEntryId)
            } else if (initialCalendarId != null) {
                Route.IcalEntryList(initialCalendarId)
            } else {
                Route.AccountsList
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

            if (initialCalendarId == null && initialIcalEntryId == null && DeepLinkHandler.initialIcalEntryId == null) {
                userAppPreferencesStore.lastUsedCalendarId?.let {
                    navController.navigate(Route.IcalEntryList(it))
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