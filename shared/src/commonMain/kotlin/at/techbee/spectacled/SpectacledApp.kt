package at.techbee.spectacled


import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.account.presentation.calendars.CalendarListScreenRoot
import at.techbee.spectacled.screens.account.presentation.calendars.CalendarListViewModel
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.AppPreferences
import at.techbee.spectacled.screens.core.koin.sharedModule
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.IcalEntryDetailsScreenRoot
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.IcalEntryDetailsViewModel
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.IcalEntryListScreenRoot
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.IcalEntryListViewModel
import at.techbee.spectacled.theme.AppTheme
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.app_name_spectacled_journals
import spectacled.shared.generated.resources.app_name_spectacled_notes
import spectacled.shared.generated.resources.app_name_spectacled_tasks
import kotlin.time.ExperimentalTime


enum class SpectacledVariant(val dbName: String, val appNameStringRes: StringResource) {
    JOURNALS("spectacled_journals.db", Res.string.app_name_spectacled_journals),
    NOTES("spectacled_notes.db", Res.string.app_name_spectacled_notes),
    TASKS("spectacled_tasks.db", Res.string.app_name_spectacled_tasks);
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
@Preview
fun SpectacledApp(spectacledVariant: SpectacledVariant = SpectacledVariant.NOTES) {

    Napier.base(DebugAntilog())  // enables Napier logging for all platforms//onNavigate = { navController.navigate(it) }
    //TODO: Check https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html#support-for-browser-navigation-in-web-apps for wasm
    KoinApplication(
        configuration = koinConfiguration(declaration = {
            modules(
                module { single { spectacledVariant } },
                sharedModule,
            )
        })
    ) {

        AppTheme {

            val navController = rememberNavController()
            //TODO: Check https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html#support-for-browser-navigation-in-web-apps for wasm

            val appPreferences = koinInject<AppPreferences>()
            val syncTrigger = koinInject<PlatformSyncTrigger>()

            LaunchedEffect(Unit) {
                syncTrigger.schedulePeriodic()
                syncTrigger.requestImmediate()
            }

            val icalEntryListViewModel = koinViewModel<IcalEntryListViewModel>()
            val calendarListViewModel = koinViewModel<CalendarListViewModel>()

            NavHost(
                navController = navController,
                startDestination = Route.HomeGraph
            ) {
                navigation<Route.HomeGraph>(Route.AccountsList) {

                    composable<Route.AccountsList> {
                        CalendarListScreenRoot(
                            viewModel = calendarListViewModel,
                            onNavigate = { route -> navController.navigate(route) }
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
                            icalEntryListViewModel.load(calendarId)
                        }

                        IcalEntryListScreenRoot(
                            icalEntryListViewModel = icalEntryListViewModel,
                            onNavigate = { route -> navController.navigate(route) },
                            onNavigateUp = { navController.popBackStack() }
                        )
                    }

                    composable<Route.IcalEntryDetails> { args ->
                        val icalEntryId = args.toRoute<Route.IcalEntryDetails>().icalEntryId
                        val icalEntryDetailsViewModel: IcalEntryDetailsViewModel = koinViewModel<IcalEntryDetailsViewModel>()

                        LaunchedEffect(icalEntryId) {
                            icalEntryDetailsViewModel.load(icalEntryId)
                        }

                        IcalEntryDetailsScreenRoot(
                            icalEntryDetailsViewModel = icalEntryDetailsViewModel,
                            onNavigateUp = { navController.popBackStack() }
                            /*
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo<Route.NoteList> {
                                            inclusive = false
                                        }
                                    }
                                }
                                */
                        )
                    }

                    composable<Route.AddICalEntry> { args ->
                        val copyFromId = args.toRoute<Route.AddICalEntry>().copyFromId
                        val calendarId = args.toRoute<Route.AddICalEntry>().calendarId

                        val icalEntryDetailsViewModel: IcalEntryDetailsViewModel = koinViewModel<IcalEntryDetailsViewModel>()

                        LaunchedEffect(copyFromId, calendarId) {
                            if (copyFromId != null)
                                icalEntryDetailsViewModel.loadCopy(copyFromId)
                            else
                                icalEntryDetailsViewModel.loadNew(calendarId)
                        }

                        IcalEntryDetailsScreenRoot(
                            icalEntryDetailsViewModel = icalEntryDetailsViewModel,
                            onNavigateUp = { navController.popBackStack() }
                            //onNavigate = { navController.navigate(it) }
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                appPreferences.lastUsedCalendarId?.let { lastUsedCalendarId ->
                    navController.navigate(Route.IcalEntryList(lastUsedCalendarId))
                }
            }
        }
    }
}
