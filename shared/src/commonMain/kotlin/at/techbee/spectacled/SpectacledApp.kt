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
import at.techbee.spectacled.screens.note.presentation.notedetails.NoteDetailsScreenRoot
import at.techbee.spectacled.screens.note.presentation.notedetails.NoteDetailsViewModel
import at.techbee.spectacled.screens.note.presentation.notelist.NoteListScreenRoot
import at.techbee.spectacled.screens.note.presentation.notelist.NoteListViewModel
import at.techbee.spectacled.theme.AppTheme
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import kotlin.time.ExperimentalTime


enum class SpectacledVariant(val dbName: String) {
    JOURNALS("spectacled_journals.db"),
    NOTES("spectacled_notes.db"),
    TASKS("spectacled_tasks.db");
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

            val noteListViewModel = koinViewModel<NoteListViewModel>()
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

                    composable<Route.NoteList>(
                        enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
                        exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth } },
                        popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth } },
                        popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } }
                    ) { args ->

                        val calendarId = args.toRoute<Route.NoteList>().calendarId

                        LaunchedEffect(calendarId) {
                            noteListViewModel.load(calendarId)
                        }

                        NoteListScreenRoot(
                            noteListViewModel = noteListViewModel,
                            onNavigate = { route -> navController.navigate(route) },
                            onNavigateUp = { navController.popBackStack() }
                        )
                    }

                    composable<Route.NoteDetails> { args ->
                        val noteId = args.toRoute<Route.NoteDetails>().noteId
                        val noteDetailsViewModel: NoteDetailsViewModel = koinViewModel<NoteDetailsViewModel>()

                        LaunchedEffect(noteId) {
                            noteDetailsViewModel.load(noteId)
                        }

                        NoteDetailsScreenRoot(
                            noteDetailsViewModel = noteDetailsViewModel,
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

                    composable<Route.AddNote> { args ->
                        val copyFromId = args.toRoute<Route.AddNote>().copyFromId
                        val calendarId = args.toRoute<Route.AddNote>().calendarId

                        val noteDetailsViewModel: NoteDetailsViewModel = koinViewModel<NoteDetailsViewModel>()

                        LaunchedEffect(copyFromId, calendarId) {
                            if (copyFromId != null)
                                noteDetailsViewModel.loadCopy(copyFromId)
                            else
                                noteDetailsViewModel.loadNew(calendarId)
                        }

                        NoteDetailsScreenRoot(
                            noteDetailsViewModel = noteDetailsViewModel,
                            onNavigateUp = { navController.popBackStack() }
                            //onNavigate = { navController.navigate(it) }
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                appPreferences.lastUsedCalendarId?.let { lastUsedCalendarId ->
                    navController.navigate(Route.NoteList(lastUsedCalendarId))
                }
            }
        }
    }
}
