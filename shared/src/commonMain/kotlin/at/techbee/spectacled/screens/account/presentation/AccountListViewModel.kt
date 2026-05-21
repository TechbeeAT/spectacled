package at.techbee.spectacled.screens.account.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.account.data.upsertCalendar
import at.techbee.spectacled.screens.account.data.upsertHomeCollection
import at.techbee.spectacled.screens.account.data.upsertPrincipal
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.data.HttpClientFactory
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.getPlatformEngine
import at.techbee.spectacled.screens.core.data.webdav.DeleteCalendarResult
import at.techbee.spectacled.screens.core.data.webdav.DiscoverCalendarsResult
import at.techbee.spectacled.screens.core.data.webdav.DiscoverHomeCollectionsResult
import at.techbee.spectacled.screens.core.data.webdav.DiscoverPrincipalsResult
import at.techbee.spectacled.screens.core.data.webdav.UpsertCalendarResult
import at.techbee.spectacled.screens.core.data.webdav.createCalendarMultiplatform
import at.techbee.spectacled.screens.core.data.webdav.deleteCalendarMultiplatform
import at.techbee.spectacled.screens.core.data.webdav.discoverCalendars
import at.techbee.spectacled.screens.core.data.webdav.discoverHomeCollections
import at.techbee.spectacled.screens.core.data.webdav.discoverPrincipalsMultiplatform
import at.techbee.spectacled.screens.core.data.webdav.updateCalDavCalendarMultiplatform
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi


class AccountListViewModel(
    private val databaseDriverFactory: DatabaseDriverFactory,
    private val credentialStore: PlatformCredentialStore,
    private val platformSyncTrigger: PlatformSyncTrigger,
    val spectacledVariant: SpectacledVariant,
): ViewModel() {

    private val _state = mutableStateOf(AccountListState())
    val state by _state

    private suspend fun getDatabase() = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)

    init {
        viewModelScope.launch {
            launch { observePrincipals() }
            launch { observeHomeCollections() }
            launch { observeCalendars() }
        }
    }

    private suspend fun observePrincipals() {
        Napier.d("Observing principals")
        getDatabase().principal_dtoQueries.getAllPrincipals().asFlow().collect {
            _state.value = _state.value.copy(
                principals = it.awaitAsList().map { principalDto -> principalDto.toDomain() }
            )
        }
    }

    private suspend fun observeHomeCollections() {
        Napier.d("Observing homeCollections")
        getDatabase().home_collection_dtoQueries.getAllHomeCollections().asFlow().collect {
            _state.value = _state.value.copy(
                homeCollections = it.awaitAsList().map { homeCollectionDto -> homeCollectionDto.toDomain() }
            )
        }
    }

    private suspend fun observeCalendars() {
        Napier.d("Observing calendars")
        getDatabase().calendar_dtoQueries.getAllCalendars().asFlow().collect {
            _state.value = _state.value.copy(
                calendars = it.awaitAsList().map { calendarDto -> calendarDto.toDomain() }
            )
        }
    }

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun onAction(action: AccountListAction) {
        when (action) {
            is AccountListAction.OnShowAboutBottomSheet -> { _state.value = _state.value.copy(showAboutBottomSheet = action.show ?: !state.showAboutBottomSheet) }
            is AccountListAction.OnShowAddPrincipalBottomSheet -> { _state.value = _state.value.copy(showAddPrincipalBottomSheet = action.show ?: !state.showAddPrincipalBottomSheet, processingState = ProcessingState.Idle) }
            is AccountListAction.OnShowDeleteCalendarDialog -> { _state.value = _state.value.copy(showDeleteCalendarDialog = action) }
            is AccountListAction.OnShowRemovePrincipalDialog -> { _state.value = _state.value.copy(showRemovePrincipalDialog = action) }
            is AccountListAction.OnShowCreateOrUpdateCalendarBottomSheet -> { _state.value = _state.value.copy(showAddOrUpdateCalendarBottomSheet = action, processingState = ProcessingState.Idle) }
            is AccountListAction.OnEditAccountFolders -> { _state.value = _state.value.copy(editFoldersOfPrincipal = action.principal) }
            is AccountListAction.OnUpdateSnackbar -> { _state.value = _state.value.copy(snackbarText = action.message) }
            //CalendarListAction.OnAddLocalCalendar -> addLocalCalendar()
            is AccountListAction.OnRemovePrincipal -> { removePrincipal(action.principal) }
            is AccountListAction.OnCalendarClicked -> {} // handled in screen
            is AccountListAction.OnDeleteCalendar -> { deleteCalendar(action.principal, action.calendar) }
            is AccountListAction.OnAddPrincipal -> { runAccountDiscovery(action.credentials) }
            is AccountListAction.OnCreateOrUpdateCalendar -> { createOrUpdateCalendar(action.principal, action.homeCollection, action.calendar) }
            is AccountListAction.OnDismissCreateOrUpdateCalendarBottomSheet -> { _state.value = _state.value.copy(showAddOrUpdateCalendarBottomSheet = null) }
            is AccountListAction.OnSyncCalendars -> { onSyncCalendars(action.calendars, true) }
            is AccountListAction.OnRerunAccountDiscovery -> { rerunAccountDiscovery(action.principals) }
            is AccountListAction.OnUpdatePrincipalPassword -> { rerunAccountDiscovery(action.principal, action.newPassword) }
            is AccountListAction.OnShowSyncInfoDialog -> { _state.value = _state.value.copy(showSyncInfoDialog = action) }
            is AccountListAction.OnShowUpdatePrincipalPasswordBottomSheet -> {
                _state.value = _state.value.copy(
                    showUpdatePrincipalPasswordBottomSheet = action,
                    showSyncInfoDialog = null,
                    processingState = ProcessingState.Idle
                )
            }
            is AccountListAction.OnDismissSyncInfoDialog -> { _state.value = _state.value.copy(showSyncInfoDialog = null) }
            AccountListAction.OnDismissUpdatePrincipalPasswordBottomSheet -> { _state.value = _state.value.copy(showUpdatePrincipalPasswordBottomSheet = null) }
            AccountListAction.OnAddLocalCalendar -> addLocalCalendar()
        }
    }


    private fun removePrincipal(principal: Principal) {

        _state.value = _state.value.copy(processingState = ProcessingState.Processing)
        viewModelScope.launch {
            credentialStore.clear(principal.principalUrl)
            getDatabase().principal_dtoQueries.delete(principal.id)
            _state.value = _state.value.copy(
                processingState = ProcessingState.Success(message = "Account successfully removed"),
                showRemovePrincipalDialog = null,
                snackbarText = "Account successfully removed"
            )
        }
    }


    private fun deleteCalendar(principal: Principal, calendar: Calendar) {
        _state.value = _state.value.copy(processingState = ProcessingState.Processing)
        viewModelScope.launch {

            try {
                val credentials = credentialStore.load(principal.principalUrl) ?: throw Exception("Credentials not found")
                val client = HttpClientFactory.create(getPlatformEngine(), credentials.username, credentials.password)

                deleteCalendarMultiplatform(client, calendar).let { remoteResult ->

                    when (remoteResult) {
                        is DeleteCalendarResult.SuccessfullyDeleted, is DeleteCalendarResult.AlreadyDeleted -> {
                            getDatabase().calendar_dtoQueries.delete(calendar.id)
                            _state.value = _state.value.copy(
                                processingState = ProcessingState.Success(message = "Calendar successfully deleted"),
                                showDeleteCalendarDialog = null,
                                snackbarText = "Calendar successfully deleted",
                            )
                        }
                        is DeleteCalendarResult.Failed -> _state.value = _state.value.copy(
                            processingState = ProcessingState.Error(message = remoteResult.message, detail = remoteResult.details),
                            snackbarText = remoteResult.message
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    processingState = ProcessingState.Error(message = e.message?:"")
                )
            }
        }
    }


    private fun addLocalCalendar() {
        Napier.d("Adding local calendar")
        viewModelScope.launch {
            Napier.d("Entered viewModelScope for Adding local calendar")

            val testUrl = Url("https://localhost/${Random.nextInt(100)}")

            getDatabase().upsertPrincipal(
                Principal(
                    id = 0,
                    principalUrl = testUrl,
                    displayName = "Local Calendar",
                    calendarUserAddressSet = listOf("john@doe.com")
                )
            )

            getDatabase().upsertHomeCollection(
                HomeCollection(
                    id = 0,
                    principalId = 0,
                    url = testUrl,
                    calDavPrivileges = listOf(CalDavPrivilege.WRITE)
                ),
                testUrl
            )

            getDatabase().upsertCalendar(
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
                    syncComponent = spectacledVariant.syncCalendarComponent
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
                _state.value = _state.value.copy(
                    processingState = ProcessingState.Error(message = "Credentials not found"),
                    snackbarText = "Credentials not found, please remove this account and add it again."
                )
            else
                runAccountDiscovery(credentials)
        }
    }

    private fun runAccountDiscovery(credentials: Credentials) {

        Napier.d("Adding principals")
        _state.value = _state.value.copy(processingState = ProcessingState.Processing)

        viewModelScope.launch {
            try {
                val client = HttpClientFactory.create(getPlatformEngine(), credentials.username, credentials.password)

                // STEP 1: Discover principals
                val discoverPrincipalsResult = discoverPrincipalsMultiplatform(client, Url(credentials.server))
                when(discoverPrincipalsResult) {
                    is DiscoverPrincipalsResult.Failed -> {
                        _state.value = _state.value.copy(processingState = ProcessingState.Error(message = discoverPrincipalsResult.message, detail = discoverPrincipalsResult.details))
                        return@launch
                    }
                    DiscoverPrincipalsResult.NotAuthorized -> {
                        _state.value = _state.value.copy(processingState = ProcessingState.Error(message = "Not authorized. Please check your username and password."))
                        return@launch
                    }
                    DiscoverPrincipalsResult.NotFound -> {
                        _state.value = _state.value.copy(processingState = ProcessingState.Error(message = "Server not found."))
                        return@launch
                    }
                    is DiscoverPrincipalsResult.Success -> {
                        discoverPrincipalsResult.principals.forEach { principal ->
                            //getDatabase().upsertPrincipal(principal)    // principals are upserted with the discovery of homesets to avoid double db operations
                            credentialStore.save(Credentials(principal.principalUrl.toString(), credentials.username, credentials.password))
                            Napier.d("Principal added")
                        }
                    }
                }

                // STEP 2: discover home collections and update principal
                val discoveredHomeCollections = mutableListOf<HomeCollection>()
                val discoveredCalendars = mutableListOf<Calendar>()
                discoverPrincipalsResult.principals.forEach { principal ->

                    when(val discoverHomeCollectionsResult = discoverHomeCollections(client, principal)) {
                        is DiscoverHomeCollectionsResult.Failed -> {
                            _state.value = _state.value.copy(
                                processingState = ProcessingState.Error(
                                    message = discoverHomeCollectionsResult.message,
                                    detail = discoverHomeCollectionsResult.details
                                )
                            )
                        }
                        DiscoverHomeCollectionsResult.NotAuthorized -> {
                            _state.value = _state.value.copy(processingState = ProcessingState.Error(message = "Not authorized. Please check your username and password."))
                        }
                        DiscoverHomeCollectionsResult.NotFound -> {
                            _state.value = _state.value.copy(processingState = ProcessingState.Error(message = "Server not found."))
                        }
                        is DiscoverHomeCollectionsResult.Success -> {
                            principal.displayName = discoverHomeCollectionsResult.principalDisplayName
                            principal.calendarUserAddressSet = discoverHomeCollectionsResult.principalCalendarUserAddressSet

                            getDatabase().upsertPrincipal(principal)
                            /*   // home collections are upserted with calendars to avoid double db operations
                            discoverHomeCollectionsResult.homeCollections.forEach { homeCollection ->
                                getDatabase().upsertHomeCollection(homeCollection, principal.principalUrl)
                            }
                             */
                            discoveredHomeCollections.addAll(discoverHomeCollectionsResult.homeCollections)
                        }
                    }

                    // STEP 3: Discover Calendars
                    discoveredHomeCollections.forEach { homeCollection ->
                        when(val discoverCalendarsResult = discoverCalendars(
                            client = client,
                            homeCollection = homeCollection,
                            supportedCalendarComponent = spectacledVariant.syncCalendarComponent
                        )) {
                            is DiscoverCalendarsResult.Failed -> {
                                _state.value = _state.value.copy(
                                    processingState = ProcessingState.Error(
                                        message = discoverCalendarsResult.message,
                                        detail = discoverCalendarsResult.details
                                    )
                                )
                            }
                            DiscoverCalendarsResult.NotAuthorized -> {
                                _state.value = _state.value.copy(processingState = ProcessingState.Error(message = "Not authorized. Please check your username and password."))
                            }
                            DiscoverCalendarsResult.NotFound -> {
                                _state.value = _state.value.copy(processingState = ProcessingState.Error(message = "Server not found."))
                            }
                            is DiscoverCalendarsResult.Success -> {
                                homeCollection.calDavPrivileges = discoverCalendarsResult.calDavPrivileges
                                getDatabase().upsertHomeCollection(homeCollection, principal.principalUrl)
                                discoverCalendarsResult.calendars.forEach {
                                    getDatabase().upsertCalendar(it, homeCollection.url)
                                }
                                discoveredCalendars.addAll(discoverCalendarsResult.calendars)
                            }
                        }
                    }
                }



                //reload calendars from DB, remove calendars that haven't been returned
                // theoretically there should be only one principal. but just in case...
                discoverPrincipalsResult.principals.forEach { principal ->
                    val localCalendars = getDatabase().calendar_dtoQueries.getCalendarsForPrincipalUrl(principal.principalUrl.toString()).awaitAsList().map { it.toDomain() }
                    val removedCalendars = localCalendars.filter { localCalendar -> discoveredCalendars.none { calendar -> calendar.url == localCalendar.url}  }
                    removedCalendars.forEach { getDatabase().calendar_dtoQueries.delete(it.id) }
                    // Todo: infom user that calendar was removed!

                    _state.value = _state.value.copy(
                        showAddPrincipalBottomSheet = false,
                        processingState = ProcessingState.Success("Account added/updated"),
                        snackbarText = "Account added/updated"
                    )
                    onSyncCalendars(localCalendars)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    processingState = ProcessingState.Error(message = e.message ?: "Unknown error"),
                    snackbarText = e.message
                )
            }
        }
    }

    private fun createOrUpdateCalendar(principal: Principal, homeCollection: HomeCollection, calendar: Calendar) {
        Napier.d("Adding calendar")
        _state.value = _state.value.copy(processingState = ProcessingState.Processing)

        viewModelScope.launch {

            try {
                val credentials = credentialStore.load(principal.principalUrl) ?: throw Exception("Credentials not found")
                val client = HttpClientFactory.create(getPlatformEngine(), credentials.username, credentials.password)
                val upsertCalendarResult = if(calendar.id == 0L) {
                    createCalendarMultiplatform(
                        client = client,
                        newCalendar = calendar.copy(syncComponent = spectacledVariant.syncCalendarComponent)
                    )
                } else {
                    updateCalDavCalendarMultiplatform(client, calendar)
                }

                when(upsertCalendarResult) {
                    is UpsertCalendarResult.Success -> {
                        Napier.d("Saving Calendar")
                        getDatabase().upsertCalendar(upsertCalendarResult.calendar, homeCollection.url)
                        Napier.d("Calendar ${calendar.displayName} added")
                        _state.value = state.copy(
                            snackbarText = "Calendar successfully added/updated",
                            showAddOrUpdateCalendarBottomSheet = null,
                            processingState = ProcessingState.Success("Calendar successfully added/updated"),
                        )
                    }
                    UpsertCalendarResult.NotFound -> deleteCalendar(principal, calendar)  // TODO: Inform user that the calendar was deleted in the background
                    is UpsertCalendarResult.Failed -> _state.value = _state.value.copy(processingState = ProcessingState.Error(message = upsertCalendarResult.message, detail = upsertCalendarResult.details))
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(processingState = ProcessingState.Error(message = e.message ?: "Unknown error", detail = e.stackTraceToString()))
                println(e.stackTraceToString())
            }
        }
    }

    private fun onSyncCalendars(calendars: List<Calendar>, forgetSyncToken: Boolean = false) {

        viewModelScope.launch {

            if(forgetSyncToken) {
                calendars.forEach { calendar ->
                    getDatabase().calendar_dtoQueries.updateCalendarSyncStatus(calendar.calendarSyncStatus?.serialize(), calendar.syncToken, calendar.id)
                }
            }

            platformSyncTrigger.requestImmediate(calendars.map { it.id })

            // TODO: Double-Check if this actually works!
            val newProcessingState = if(_state.value.calendars.all { it.calendarSyncStatus?.type == CalendarSyncStatusType.SYNCED })
                ProcessingState.Success("Calendars synced")
            else if(_state.value.calendars.all { it.calendarSyncStatus?.type == CalendarSyncStatusType.NOT_AUTHORIZED })
                ProcessingState.Error(_state.value.calendars.firstOrNull()?.calendarSyncStatus?.message?: "Not authorized", _state.value.calendars.firstOrNull()?.calendarSyncStatus?.details)
            else if(_state.value.calendars.all { it.calendarSyncStatus?.type == CalendarSyncStatusType.NOT_FOUND })
                ProcessingState.Error(_state.value.calendars.firstOrNull()?.calendarSyncStatus?.message?: "Not found", _state.value.calendars.firstOrNull()?.calendarSyncStatus?.details)
            else
                ProcessingState.Error("Some calendars failed to sync. Please check the calendars for details.")

            _state.value = _state.value.copy(processingState = newProcessingState)
        }
    }
}