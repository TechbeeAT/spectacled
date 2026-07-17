package at.techbee.spectacled.screens.account.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.ioDispatcher
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.webdav.DeleteCalendarResult
import at.techbee.spectacled.screens.core.data.webdav.DiscoverCalendarsResult
import at.techbee.spectacled.screens.core.data.webdav.DiscoverHomeCollectionsResult
import at.techbee.spectacled.screens.core.data.webdav.DiscoverPrincipalsResult
import at.techbee.spectacled.screens.core.data.webdav.UpsertCalendarResult
import at.techbee.spectacled.screens.core.data.webdav.WebDavRemoteCalendarDataSource
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.login_message_forbidden
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi


class AccountListViewModel(
    private val calendarRepository: CalendarRepository,
    private val credentialStore: PlatformCredentialStore,
    private val platformSyncTrigger: PlatformSyncTrigger,
    private val webDavCalendarDataSource: WebDavRemoteCalendarDataSource,
    val spectacledVariant: SpectacledVariant,
    val userAppPreferencesStore: PlatformUserAppPreferencesStore
    ): ViewModel() {

    private val _state = MutableStateFlow(AccountListState())
    val state = _state.asStateFlow()

    private var observationJob: Job? = null

    init {
        load()
    }

    fun load() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            launch { observePrincipals() }
            launch { observeHomeCollections() }
            launch { observeCalendars() }
        }
    }

    private suspend fun observePrincipals() {
        Napier.d("Observing principals")
        calendarRepository.getAllPrincipalsFlow().collect { principals ->
            _state.update { state -> state.copy(
                principals = principals
            ) }
        }
    }

    private suspend fun observeHomeCollections() {
        Napier.d("Observing homeCollections")
        calendarRepository.getAllHomeCollectionsFlow().collect { homeCollections ->
            _state.update { state -> state.copy(
                homeCollections = homeCollections
            ) }
        }
    }

    private suspend fun observeCalendars() {
        Napier.d("Observing calendars")
        calendarRepository.getAllCalendarsFlow().collect { calendars ->
            _state.update { state -> state.copy(
                calendars = calendars
            ) }
        }
    }

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun onAction(action: AccountListAction) {
        when (action) {
            is AccountListAction.OnShowAboutBottomSheet -> { _state.update { it.copy(showAboutBottomSheet = action.show ?: !it.showAboutBottomSheet) } }
            is AccountListAction.OnShowAddPrincipalBottomSheet -> { _state.update { it.copy(showAddPrincipalBottomSheet = action.show ?: !it.showAddPrincipalBottomSheet, processingState = ProcessingState.Idle) } }
            is AccountListAction.OnShowDeleteCalendarDialog -> { _state.update { it.copy(showDeleteCalendarDialog = action) } }
            is AccountListAction.OnShowRemovePrincipalDialog -> { _state.update { it.copy(showRemovePrincipalDialog = action) } }
            is AccountListAction.OnShowCreateOrUpdateCalendarBottomSheet -> { _state.update { it.copy(showAddOrUpdateCalendarBottomSheet = action, processingState = ProcessingState.Idle) } }
            is AccountListAction.OnEditAccountFolders -> { _state.update { it.copy(editFoldersOfPrincipal = action.principal) } }
            is AccountListAction.OnUpdateSnackbar -> { _state.update { it.copy(snackbarText = action.message) } }
            is AccountListAction.OnRemovePrincipal -> { removePrincipal(action.principal) }
            is AccountListAction.OnCalendarClicked -> {} // handled in screen
            is AccountListAction.OnDeleteCalendar -> { deleteCalendar(action.principal, action.calendar) }
            is AccountListAction.OnAddPrincipal -> { runAccountDiscovery(action.credentials) }
            is AccountListAction.OnCreateOrUpdateCalendar -> { createOrUpdateCalendar(action.principal, action.homeCollection, action.calendar) }
            is AccountListAction.OnDismissCreateOrUpdateCalendarBottomSheet -> { _state.update { it.copy(showAddOrUpdateCalendarBottomSheet = null) } }
            is AccountListAction.OnSyncCalendars -> { onSyncCalendars(action.calendars, true) }
            is AccountListAction.OnRerunAccountDiscovery -> { rerunAccountDiscovery(action.principals) }
            is AccountListAction.OnUpdatePrincipalPassword -> { rerunAccountDiscovery(action.principal, action.newPassword) }
            is AccountListAction.OnShowSyncInfoDialog -> { _state.update { it.copy(showSyncInfoDialog = action) } }
            is AccountListAction.OnShowUpdatePrincipalPasswordBottomSheet -> {
                _state.update { it.copy(
                    showUpdatePrincipalPasswordBottomSheet = action,
                    showSyncInfoDialog = null,
                    processingState = ProcessingState.Idle
                ) }
            }
            is AccountListAction.OnDismissSyncInfoDialog -> { _state.update { it.copy(showSyncInfoDialog = null) } }
            AccountListAction.OnDismissUpdatePrincipalPasswordBottomSheet -> { _state.update { it.copy(showUpdatePrincipalPasswordBottomSheet = null) } }
            AccountListAction.OnAddLocalCalendar -> addLocalCalendar()
            is AccountListAction.OnShowSettingsBottomSheet -> { _state.update { it.copy(showSettingsBottomSheet = action.show) } }
            is AccountListAction.OnToggleSyncEnabled -> { onToggleSyncEnabled(action.calendarId, action.enabled)}
        }
    }


    private fun removePrincipal(principal: Principal) {

        _state.update { it.copy(processingState = ProcessingState.Processing) }
        viewModelScope.launch {
            credentialStore.clear(principal.principalUrl)
            calendarRepository.deletePrincipal(principal.id)
            _state.update { it.copy(
                processingState = ProcessingState.Success(message = "Account successfully removed"),
                showRemovePrincipalDialog = null,
                snackbarText = "Account successfully removed"
            ) }
        }
    }


    private fun deleteCalendar(principal: Principal, calendar: Calendar) {
        _state.update { it.copy(processingState = ProcessingState.Processing) }
        viewModelScope.launch(ioDispatcher) {

            try {
                val credentials = credentialStore.load(principal.principalUrl) ?: throw Exception("Credentials not found")

                webDavCalendarDataSource.deleteCalendar(calendar, credentials).let { remoteResult ->

                    when (remoteResult) {
                        is DeleteCalendarResult.SuccessfullyDeleted, is DeleteCalendarResult.AlreadyDeleted -> {
                            calendarRepository.deleteCalendar(calendar.id)
                            _state.update { it.copy(
                                processingState = ProcessingState.Success(message = "Calendar successfully deleted"),
                                showDeleteCalendarDialog = null,
                                snackbarText = "Calendar successfully deleted",
                            ) }
                        }
                        is DeleteCalendarResult.Failed -> _state.update { it.copy(
                            processingState = ProcessingState.Error(message = remoteResult.message, detail = remoteResult.details),
                            snackbarText = remoteResult.message
                        ) }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    processingState = ProcessingState.Error(message = e.message?:"")
                ) }
            }
        }
    }


    private fun addLocalCalendar() {
        Napier.d("Adding local calendar")
        viewModelScope.launch {
            Napier.d("Entered viewModelScope for Adding local calendar")

            val testUrl = Url("https://localhost/${Random.nextInt(100)}")

            calendarRepository.upsertPrincipal(
                Principal(
                    id = 0,
                    principalUrl = testUrl,
                    displayName = "Local Calendar",
                    calendarUserAddressSet = listOf("john@doe.com")
                )
            )

            calendarRepository.upsertHomeCollection(
                HomeCollection(
                    id = 0,
                    principalId = 0,
                    url = testUrl,
                    calDavPrivileges = listOf(CalDavPrivilege.WRITE)
                ),
                testUrl
            )

            calendarRepository.upsertCalendar(
                Calendar(
                    id = 0,
                    homeCollectionId = 0,
                    url = testUrl,
                    displayName = "Local Calendar",
                    calendarDescription = "Description of local calendar",
                    color = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)),
                    ctag = "",
                    supportedComponents = emptyList(),
                    calDavPrivileges = listOf(CalDavPrivilege.WRITE),
                    calendarSyncStatus = null,
                    syncToken = null,
                    attachmentCollectionUrl = null
                ),
                testUrl
            )

            Napier.d("Local principal, home collection and calendar added")
        }
    }


    private fun rerunAccountDiscovery(principal: Principal) = rerunAccountDiscovery(principal, null)

    private fun rerunAccountDiscovery(principals: List<Principal>) {
            principals.forEach { principal -> rerunAccountDiscovery(principal) }
    }

    private fun rerunAccountDiscovery(principal: Principal, newPassword: String?) {

        viewModelScope.launch {
            val credentials = credentialStore.load(principal.principalUrl)?.let {
                if(newPassword != null)
                    Credentials(it.server, it.username, newPassword)
                else
                    it
            }

            if(credentials == null)
                _state.update { it.copy(
                    processingState = ProcessingState.Error(message = "Credentials not found"),
                    snackbarText = "Credentials not found, please remove this account and add it again."
                ) }
            else
                runAccountDiscovery(credentials)
        }
    }

    private fun runAccountDiscovery(credentials: Credentials) {

        Napier.d("Adding principals")
        _state.update { it.copy(processingState = ProcessingState.Processing) }

        // Run the whole discovery pipeline off the Main dispatcher. On Compose Desktop the
        // viewModelScope's Main dispatcher does not reliably resume suspended network
        // continuations (nor the HttpTimeout timer), so leaving this on Main makes discovery
        // hang forever without ever hitting a timeout. IO resumes reliably on every platform.
        viewModelScope.launch(ioDispatcher) {
            try {

                // STEP 1: Discover principals
                val discoverPrincipalsResult = webDavCalendarDataSource.discoverPrincipals(credentials.server, credentials)
                when(discoverPrincipalsResult) {
                    is DiscoverPrincipalsResult.Failed -> {
                        _state.update { it.copy(processingState = ProcessingState.Error(message = discoverPrincipalsResult.message, detail = discoverPrincipalsResult.details)) }
                        return@launch
                    }
                    DiscoverPrincipalsResult.NotAuthorized -> {
                        _state.update { it.copy(processingState = ProcessingState.Error(message = getString(Res.string.login_message_forbidden))) }
                        return@launch
                    }
                    DiscoverPrincipalsResult.NotFound -> {
                        _state.update { it.copy(processingState = ProcessingState.Error(message = "Server not found.")) }
                        return@launch
                    }
                    is DiscoverPrincipalsResult.Success -> {
                        discoverPrincipalsResult.principals.forEach { principal ->
                            //principals are upserted with the discovery of homesets to avoid double db operations
                            credentialStore.save(Credentials(principal.principalUrl, credentials.username, credentials.password))
                            Napier.d("Principal added")
                        }
                    }
                }

                // STEP 2: discover home collections and update principal
                val discoveredHomeCollections = mutableListOf<HomeCollection>()
                val discoveredCalendars = mutableListOf<Calendar>()
                discoverPrincipalsResult.principals.forEach { principal ->

                    when(val discoverHomeCollectionsResult = webDavCalendarDataSource.discoverHomeCollections(principal, credentials)) {
                        is DiscoverHomeCollectionsResult.Failed -> {
                            _state.update { it.copy(
                                processingState = ProcessingState.Error(
                                    message = discoverHomeCollectionsResult.message,
                                    detail = discoverHomeCollectionsResult.details
                                )
                            ) }
                        }
                        DiscoverHomeCollectionsResult.NotAuthorized -> {
                            _state.update { it.copy(processingState = ProcessingState.Error(message = "Not authorized. Please check your username and password.")) }
                        }
                        DiscoverHomeCollectionsResult.NotFound -> {
                            _state.update { it.copy(processingState = ProcessingState.Error(message = "Server not found.")) }
                        }
                        is DiscoverHomeCollectionsResult.Success -> {
                            principal.displayName = discoverHomeCollectionsResult.principalDisplayName
                            principal.calendarUserAddressSet = discoverHomeCollectionsResult.principalCalendarUserAddressSet

                            calendarRepository.upsertPrincipal(principal)
                            discoveredHomeCollections.addAll(discoverHomeCollectionsResult.homeCollections)
                        }
                    }

                    // STEP 3: Discover Calendars
                    discoveredHomeCollections.forEach { homeCollection ->
                        when(val discoverCalendarsResult = webDavCalendarDataSource.discoverCalendars(
                            homeCollection = homeCollection,
                            credentials = credentials
                        )) {
                            is DiscoverCalendarsResult.Failed -> {
                                _state.update { it.copy(
                                    processingState = ProcessingState.Error(
                                        message = discoverCalendarsResult.message,
                                        detail = discoverCalendarsResult.details
                                    )
                                ) }
                            }
                            DiscoverCalendarsResult.NotAuthorized -> {
                                _state.update { it.copy(processingState = ProcessingState.Error(message = "Not authorized. Please check your username and password.")) }
                            }
                            DiscoverCalendarsResult.NotFound -> {
                                _state.update { it.copy(processingState = ProcessingState.Error(message = "Server not found.")) }
                            }
                            is DiscoverCalendarsResult.Success -> {
                                homeCollection.calDavPrivileges = discoverCalendarsResult.calDavPrivileges
                                calendarRepository.upsertHomeCollection(homeCollection, principal.principalUrl)

                                val disabledCalendarUrls = _state.value.calendars.filter { it.calendarSyncStatus?.type == CalendarSyncStatusType.DISABLED }.map { it.url }

                                discoverCalendarsResult.calendars.forEach { calendar ->

                                    if(calendar.supportedComponents.none { it == spectacledVariant.mainCalendarComponent })
                                        return@forEach     // skip calendars that don't support the mainCalendarComponent of the app

                                    calendarRepository.upsertCalendar(
                                        calendar =
                                            if(calendar.url in disabledCalendarUrls)    // remember disabled state
                                                calendar.copy(calendarSyncStatus = CalendarSyncStatus(type = CalendarSyncStatusType.DISABLED))
                                            else
                                                calendar,
                                        homeCollectionUrl = homeCollection.url
                                    )
                                }
                                discoveredCalendars.addAll(discoverCalendarsResult.calendars)
                            }
                        }
                    }
                }



                //reload calendars from DB, remove calendars that haven't been returned
                // theoretically there should be only one principal. but just in case...
                discoverPrincipalsResult.principals.forEach { principal ->
                    val localCalendars = calendarRepository.getCalendarsForPrincipalUrl(principal.principalUrl.toString())
                    val removedCalendars = localCalendars.filter { localCalendar -> discoveredCalendars.none { calendar -> calendar.url == localCalendar.url}  }
                    removedCalendars.forEach { calendarRepository.deleteCalendar(it.id) }
                    // Todo: infom user that calendar was removed!

                    _state.update { it.copy(
                        showAddPrincipalBottomSheet = false,
                        processingState = ProcessingState.Success("Account added/updated"),
                        snackbarText = "Account added/updated"
                    ) }
                    onSyncCalendars(localCalendars)
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    processingState = ProcessingState.Error(message = e.message ?: "Unknown error"),
                    snackbarText = e.message
                ) }
            }
        }
    }

    private fun createOrUpdateCalendar(principal: Principal, homeCollection: HomeCollection, calendar: Calendar) {
        Napier.d("Adding calendar")
        _state.update { it.copy(processingState = ProcessingState.Processing) }

        viewModelScope.launch(ioDispatcher) {

            try {
                val credentials = credentialStore.load(principal.principalUrl) ?: throw Exception("Credentials not found")
                val upsertCalendarResult = if(calendar.id == 0L) {
                    webDavCalendarDataSource.createCalendar(calendar, credentials)
                } else {
                    webDavCalendarDataSource.updateCalendar(calendar, credentials)
                }

                when(upsertCalendarResult) {
                    is UpsertCalendarResult.Success -> {
                        Napier.d("Saving Calendar")
                        calendarRepository.upsertCalendar(upsertCalendarResult.calendar, homeCollection.url)
                        Napier.d("Calendar ${calendar.displayName} added")
                        _state.update { it.copy(
                            snackbarText = "Calendar successfully added/updated",
                            showAddOrUpdateCalendarBottomSheet = null,
                            processingState = ProcessingState.Success("Calendar successfully added/updated"),
                        ) }
                    }
                    UpsertCalendarResult.NotFound -> deleteCalendar(principal, calendar)  // TODO: Inform user that the calendar was deleted in the background
                    is UpsertCalendarResult.Failed -> _state.update { it.copy(processingState = ProcessingState.Error(message = upsertCalendarResult.message, detail = upsertCalendarResult.details)) }
                }

            } catch (e: Exception) {
                _state.update { it.copy(processingState = ProcessingState.Error(message = e.message ?: "Unknown error", detail = e.stackTraceToString())) }
                Napier.e(e.stackTraceToString())
            }
        }
    }

    private fun onSyncCalendars(calendars: List<Calendar>, forgetSyncToken: Boolean = false) {

        val syncRelevantCalendars = calendars.filter { it.calendarSyncStatus?.type != CalendarSyncStatusType.DISABLED }

        viewModelScope.launch {

            if(forgetSyncToken) {
                calendars.forEach { calendar ->
                    calendarRepository.updateCalendarSyncStatus(calendar.calendarSyncStatus?.serialize(), calendar.syncToken, calendar.id)
                }
            }

            platformSyncTrigger.requestImmediate(syncRelevantCalendars.map { it.id })

            val calendarsRelevantForSyncCheck = _state.value.calendars.filter { calendar ->
                syncRelevantCalendars.any { syncRelevantCalendar -> calendar.id == syncRelevantCalendar.id }
            }

            // TODO: Double-Check if this actually works!
            val newProcessingState = if(calendarsRelevantForSyncCheck.all { it.calendarSyncStatus?.type == CalendarSyncStatusType.SYNCED })
                ProcessingState.Success("Calendars synced")
            else if(calendarsRelevantForSyncCheck.all { it.calendarSyncStatus?.type == CalendarSyncStatusType.NOT_AUTHORIZED })
                ProcessingState.Error(calendarsRelevantForSyncCheck.firstOrNull()?.calendarSyncStatus?.message?: "Not authorized", _state.value.calendars.firstOrNull()?.calendarSyncStatus?.details)
            else if(calendarsRelevantForSyncCheck.all { it.calendarSyncStatus?.type == CalendarSyncStatusType.NOT_FOUND })
                ProcessingState.Error(calendarsRelevantForSyncCheck.firstOrNull()?.calendarSyncStatus?.message?: "Not found", _state.value.calendars.firstOrNull()?.calendarSyncStatus?.details)
            else
                ProcessingState.Error("Some calendars failed to sync. Please check the calendars for details.")

            _state.update { it.copy(processingState = newProcessingState) }
        }
    }

    private fun onToggleSyncEnabled(calendarId: Long, enabled: Boolean) {
        viewModelScope.launch {
            calendarRepository.updateCalendarSyncStatus(
                calendarSyncStatus = if(enabled) null else CalendarSyncStatus(type = CalendarSyncStatusType.DISABLED).serialize(),
                syncToken = null,
                id = calendarId
            )
        }
    }
}
