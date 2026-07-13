package at.techbee.spectacled


import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.screens.LandscapeLayout
import at.techbee.spectacled.screens.PortraitLayout
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.account.presentation.AccountListViewModel
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.koin.sharedModule
import at.techbee.spectacled.screens.core.navigation.AppNavigator
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
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

        // Shared, config-change-surviving navigation state and screen view models.
        // Both layouts use these exact instances, so switching between portrait and
        // landscape (e.g. on rotation) keeps the open screen instead of resetting.
        val appNavigator = koinViewModel<AppNavigator>()
        val accountListViewModel = koinViewModel<AccountListViewModel>()
        val listViewModel = koinViewModel<ListViewModel>()
        val detailsViewModel = koinViewModel<DetailsViewModel>()

        // Seed the initial deep link once onto the shared stack (both layouts honour it).
        LaunchedEffect(Unit) {
            val data = DeepLinkHandler.deepLinkData
            if (!data.consumed) {
                val deepLinkRoute: Route? = when {
                    data.initialIcalEntryId != null ->
                        if (data.initialIcalEntryId == 0L)
                            Route.AddICalEntry(calendarId = data.initialCalendarId ?: 0L, initialDescription = data.initialIcalEntryDescription)
                        else
                            Route.IcalEntryDetails(data.initialIcalEntryId)
                    data.initialCalendarId != null -> Route.IcalEntryList(data.initialCalendarId)
                    else -> null
                }
                deepLinkRoute?.let { appNavigator.navigate(it) }
                DeepLinkHandler.consume()
            }
        }

        // Single place that binds the current location to the shared view models,
        // used identically by both layouts (replaces the old per-destination loads
        // and the landscape applyRoute()).
        val selectedCalendarId = appNavigator.backStack
            .filterIsInstance<Route.IcalEntryList>().lastOrNull()?.calendarId
        val detailTarget = appNavigator.backStack
            .lastOrNull { it is Route.IcalEntryDetails || it is Route.AddICalEntry }

        LaunchedEffect(selectedCalendarId) {
            selectedCalendarId?.let { listViewModel.load(it) } ?: listViewModel.reset()
        }
        LaunchedEffect(detailTarget) {
            when (val target = detailTarget) {
                is Route.IcalEntryDetails -> detailsViewModel.load(target.icalEntryId)
                is Route.AddICalEntry -> when {
                    target.copyFromId != null -> detailsViewModel.loadCopy(target.copyFromId)
                    target.calendarId != 0L -> detailsViewModel.loadNew(target.calendarId, target.initialDescription)
                    else -> detailsViewModel.prepareNew(target.initialDescription)
                }
                else -> detailsViewModel.reset()
            }
        }

        val onNavigateUp: () -> Unit = {
            if (!appNavigator.navigateUp()) onCloseApp()
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

            // BoxWithConstraints observes the window size and recomposes whenever the
            // orientation or size changes. Only the renderer is swapped here — the
            // navigation state and view models above survive the switch.
            BoxWithConstraints {
                if (maxWidth > maxHeight) {
                    LandscapeLayout(
                        accountListViewModel = accountListViewModel,
                        listViewModel = listViewModel,
                        detailsViewModel = detailsViewModel,
                        onNavigate = appNavigator::navigate,
                        onNavigateUp = onNavigateUp
                    )
                } else {
                    PortraitLayout(
                        appNavigator = appNavigator,
                        accountListViewModel = accountListViewModel,
                        listViewModel = listViewModel,
                        detailsViewModel = detailsViewModel,
                        onNavigateUp = onNavigateUp
                    )
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
