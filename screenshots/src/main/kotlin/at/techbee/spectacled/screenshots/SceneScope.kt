package at.techbee.spectacled.screenshots

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.account.presentation.AccountListAction
import at.techbee.spectacled.screens.account.presentation.AccountListScreenRoot
import at.techbee.spectacled.screens.account.presentation.AccountListViewModel
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import at.techbee.spectacled.screens.details.presentation.DetailsScreenRoot
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
import at.techbee.spectacled.screens.list.presentation.ListAction
import at.techbee.spectacled.screens.list.presentation.ListScreenRoot
import at.techbee.spectacled.screens.list.presentation.ListViewModel
import at.techbee.spectacled.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.Koin
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * What a [Scene] can do: open one of the app's screens with real ViewModels on the seeded
 * database, then send them actions. Fresh ViewModels per scene, so nothing leaks between shots.
 */
internal class SceneScope(
    koin: Koin,
    private val variant: SpectacledVariant,
    private val device: Device,
    private val sampleData: SeededSampleData
) {
    private enum class Screen { NONE, ACCOUNTS, LIST, DETAILS }

    private var screen by mutableStateOf(Screen.NONE)

    private val viewModelStore = ViewModelStore()
    val accountListViewModel: AccountListViewModel = koin.get<AccountListViewModel>().also { viewModelStore.put("accounts", it) }
    val listViewModel: ListViewModel = koin.get<ListViewModel>().also { viewModelStore.put("list", it) }
    val detailsViewModel: DetailsViewModel = koin.get<DetailsViewModel>().also { viewModelStore.put("details", it) }

    /** The account and calendar overview. On wide devices the app shows it next to the list. */
    suspend fun openAccounts() {
        if (device.isWide)
            loadList()
        awaitUntil("the accounts to load") { accountListViewModel.state.value.calendars.isNotEmpty() }
        screen = Screen.ACCOUNTS
    }

    /** The entry list of the sample calendar. */
    suspend fun openList() {
        loadList()
        if (device.isWide)
            awaitUntil("the accounts to load") { accountListViewModel.state.value.calendars.isNotEmpty() }
        screen = Screen.LIST
    }

    /** The details of the entry with [uid]. On wide devices the app shows the list next to it. */
    suspend fun openDetails(uid: String) {
        val icalEntryId = sampleData.icalEntryIdsByUid[uid]
            ?: error("No sample entry with uid '$uid' for $variant, see sample-data/screenshot_entries.sql")
        if (device.isWide)
            loadList()
        detailsViewModel.load(icalEntryId)
        awaitUntil("entry '$uid' to load") {
            detailsViewModel.state.value.let { !it.isLoading && it.icalEntry.id == icalEntryId }
        }
        screen = Screen.DETAILS
    }

    fun accounts(action: AccountListAction) = accountListViewModel.onAction(action)
    fun list(action: ListAction) = listViewModel.onAction(action)
    fun details(action: DetailsAction) = detailsViewModel.onAction(action)

    fun clear() = viewModelStore.clear()

    private suspend fun loadList() {
        listViewModel.load(sampleData.calendarId)
        awaitUntil("the list to load") {
            listViewModel.state.value.let { it.calendar.id == sampleData.calendarId && !it.isRefreshing }
        }
    }

    private suspend fun awaitUntil(what: String, condition: () -> Boolean) {
        withTimeoutOrNull(20.seconds) {
            while (!condition())
                delay(20.milliseconds)
        } ?: error("Timed out waiting for $what")
    }

    /** The same arrangement SpectacledApp uses, without navigation: nothing here navigates. */
    @Composable
    fun Content() {
        AppTheme(spectacledVariant = variant) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val wide = device.isWide
                Row(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.NONE -> Unit
                        Screen.ACCOUNTS, Screen.LIST -> {
                            if (wide || screen == Screen.ACCOUNTS) {
                                AccountListScreenRoot(
                                    viewModel = accountListViewModel,
                                    onNavigate = { },
                                    removeSafeAreaPaddingValues = wide,
                                    modifier = Modifier.weight(if (wide) 0.4f else 1f)
                                )
                            }
                            if (wide)
                                VerticalDivider()
                            if (wide || screen == Screen.LIST) {
                                ListScreenRoot(
                                    listViewModel = listViewModel,
                                    onNavigate = { },
                                    onNavigateUp = { },
                                    removeSafeAreaPaddingValues = wide,
                                    modifier = Modifier.weight(if (wide) 0.6f else 1f)
                                )
                            }
                        }
                        Screen.DETAILS -> {
                            if (wide) {
                                ListScreenRoot(
                                    listViewModel = listViewModel,
                                    onNavigate = { },
                                    onNavigateUp = { },
                                    removeSafeAreaPaddingValues = true,
                                    modifier = Modifier.weight(0.4f)
                                )
                                VerticalDivider()
                            }
                            DetailsScreenRoot(
                                detailsViewModel = detailsViewModel,
                                onNavigate = { },
                                onNavigateUp = { },
                                removeSafeAreaPaddingValues = wide,
                                modifier = Modifier.weight(if (wide) 0.6f else 1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
