package at.techbee.spectacled.screens.details.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.account.data.insertOrUpdateIcalEntry
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.PlatformShareManager
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.ShareContent
import at.techbee.spectacled.screens.core.SyncCoordinator
import at.techbee.spectacled.screens.core.data.HttpClientFactory
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.getPlatformEngine
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.mapper.dto.CATEGORY_SPLIT_DELIMITER
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import io.github.aakira.napier.Napier
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.getString
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.credentials_not_found
import spectacled.shared.generated.resources.entry_copy
import spectacled.shared.generated.resources.entry_deleted
import spectacled.shared.generated.resources.entry_restored
import spectacled.shared.generated.resources.entry_successfully_saved
import spectacled.shared.generated.resources.sync_conflict_detected
import spectacled.shared.generated.resources.unexpected_error_occurred
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

@OptIn(FlowPreview::class)
class DetailsViewModel(
    private val databaseDriverFactory: DatabaseDriverFactory,
    private val credentialStore: PlatformCredentialStore,
    private val platformSyncTrigger: PlatformSyncTrigger,
    private val shareManager: PlatformShareManager,
    private val spectacledVariant: SpectacledVariant
): ViewModel() {

    private var _state by mutableStateOf(DetailsState())
    val state: DetailsState get() = _state

    private suspend fun getDatabase() = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)

    init {
        viewModelScope.launch {

            snapshotFlow { state.icalEntry } // snapshotFlow tracks reads of the entry and emits on change
                .debounce(500L) // Wait for 500ms pause in typing
                .distinctUntilChanged { old, new -> old.lastModified == new.lastModified } // Only save if last modified changed
                .collect {
                    if(!state.isLoading && state.icalEntry.calendarId != 0L && state.icalEntry.syncState != SyncState.SYNCED)
                        saveIcalEntry(state.icalEntry.syncState)
                }
        }
    }

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun load(icalEntryId: Long) {

        viewModelScope.launch {
            val icalEntry = getDatabase().icalentry_dtoQueries.getIcalEntryById(icalEntryId).awaitAsOneOrNull()?.toDomain() ?: return@launch
            val calendar = getDatabase().calendar_dtoQueries.getCalendarById(icalEntry.calendarId).awaitAsOneOrNull()?.toDomain() ?: return@launch

            _state = _state.copy(
                icalEntry = icalEntry,
                originalIcalEntry = icalEntry,
                calendar = calendar,
                isLoading = false,
                navigateUp = false
            )

            launch { observeColors() }
            launch { observeCategories() }
            launch { observeTimezones() }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun loadNew(calendarId: Long) {

        viewModelScope.launch {
            val newIcalEntry = IcalEntry(
                calendarId = calendarId,
                dtStart = if (spectacledVariant == SpectacledVariant.JOURNALS) IcsDateTime.now() else null,
                calendarComponent = spectacledVariant.syncCalendarComponent
            )
            val calendar = getDatabase().calendar_dtoQueries.getCalendarById(calendarId).awaitAsOneOrNull()?.toDomain() ?: return@launch

            _state = _state.copy(
                icalEntry = newIcalEntry,
                originalIcalEntry = newIcalEntry,
                calendar = calendar,
                isLoading = false,
                showDeleteDialog = false,
                navigateUp = false
            )

            launch { observeColors() }
            launch { observeCategories() }
            launch { observeTimezones() }
        }
    }


    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun loadCopy(icalEntryIdToCopy: Long, isRestoredCopy: Boolean = false) {
        viewModelScope.launch {
            //saveIcalEntry(false)
            val originalIcalEntry = getDatabase().icalentry_dtoQueries.getIcalEntryById(icalEntryIdToCopy).awaitAsOneOrNull()?.toDomain()
            if(originalIcalEntry == null) {
                _state = _state.copy(
                    snackbarText = getString(Res.string.unexpected_error_occurred),
                    isLoading = false,
                    navigateUp = true
                )
                return@launch
            }
            val copiedIcalEntry = IcalEntry(
                calendarId = originalIcalEntry.calendarId,
                summary = originalIcalEntry.summary + if (isRestoredCopy) " (${getString(Res.string.entry_restored)})" else " (${
                    getString(
                        Res.string.entry_copy
                    )
                })",
                description = originalIcalEntry.description,
                dtStart = originalIcalEntry.dtStart,
                categories = originalIcalEntry.categories,
                color = originalIcalEntry.color,
                extraProperties = originalIcalEntry.extraProperties,
                calendarComponent = originalIcalEntry.calendarComponent
            )
            _state = _state.copy(
                icalEntry = copiedIcalEntry,
                originalIcalEntry = copiedIcalEntry,
                isLoading = false,
                navigateUp = false
            )

            launch { observeColors() }
            launch { observeCategories() }
            launch { observeTimezones() }
        }
    }

    private suspend fun observeColors() {
        getDatabase()
            .icalentry_dtoQueries.getAllColors()
            .asFlow()
            .collect { colorsFlow ->
                val emittedColors = colorsFlow.awaitAsList().map { Color(it) }
                _state = _state.copy(
                    allColors = emittedColors
                )
            }
    }

    private suspend fun observeCategories() {
        getDatabase()
            .icalentry_dtoQueries.getAllCategories()
            .asFlow()
            .collect { categoriesFlow ->
                val allCategories = mutableSetOf<String>()
                categoriesFlow.awaitAsList().let {
                    it.forEach { unsplitCategory -> allCategories.addAll(unsplitCategory.split(CATEGORY_SPLIT_DELIMITER)) }
                }
                _state = _state.copy(
                    allCategories = allCategories.toList()
                )
            }
    }

    private suspend fun observeTimezones() {
        getDatabase()
            .icalentry_dtoQueries.getLastUsedTimezones()
            .asFlow()
            .collect { timezonesFlow ->
                timezonesFlow.awaitAsList().let { timezones ->
                    _state = _state.copy(latestUsedTimezones = timezones.map { TimeZone.of(it) })
                }
            }
    }


    fun onAction(action: DetailsAction) {
        when(action) {
            is DetailsAction.OnUpdateSnackbar -> { _state = _state.copy(snackbarText = action.message) }
            is DetailsAction.OnUpdateCategories -> onUpdateCategories(action.addCategory, action.removeCategory)
            is DetailsAction.OnUpdateColor -> onUpdateColor(action.color)
            is DetailsAction.OnUpdateDescription -> onUpdateDescription(action.description)
            is DetailsAction.OnUpdateSummary -> onUpdateSummary(action.summary)
            DetailsAction.OnDelete -> saveIcalEntry(syncState = SyncState.LOCAL_DELETED, navigateUp = true)
            is DetailsAction.OnNavigateUp -> onNavigateUp(action.navigateUp)
            is DetailsAction.OnShowDeleteDialog -> { _state = _state.copy(showDeleteDialog = action.show) }
            is DetailsAction.OnShowMoreBottomSheet -> { _state = _state.copy(showMoreBottomSheet = action.show) }
            is DetailsAction.OnShowCategorySelectorBottomSheet -> { _state = _state.copy(showCategorySelectorBottomSheet = action.show) }
            is DetailsAction.OnShowColorSelectorBottomSheet -> { _state = _state.copy(showColorSelectorBottomSheet = action.show) }
            DetailsAction.OnCreateCopy -> { loadCopy(_state.icalEntry.id) }
            is DetailsAction.OnSyncConflictUpdateUserDecision -> {
                when(action.syncState) {
                    SyncState.USER_DECIDED_SERVER_WINS -> saveAndAwaitSync(action.syncState)
                    SyncState.USER_DECIDED_CLIENT_WINS ->
                        if (state.icalEntry.syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED) { // treat like a new entry
                            saveIcalEntry(SyncState.REMOTE_DELETED_LOCAL_TRASHBIN)
                            loadCopy(_state.icalEntry.id, true)
                        } else {
                            saveIcalEntry(action.syncState)
                        }
                    SyncState.REMOTE_DELETED_LOCAL_TRASHBIN -> saveIcalEntry(action.syncState, navigateUp = true)
                    else -> {}  // not allowed
                }
            }
            DetailsAction.OnDispose -> onDispose()
            DetailsAction.OnRestoreEntry -> onRestoreEntry()
            is DetailsAction.OnShowJournalStatusPickerBottomSheet -> { _state = _state.copy(showJournalStatusPickerBottomSheet = action.show) }
            is DetailsAction.OnShowTaskStatusProgressPickerBottomSheet -> { _state = _state.copy(showTaskStatusProgressPickerBottomSheet = action.show) }
            is DetailsAction.OnUpdateDtStart -> { onUpdateDtStart(action.icsDateTime) }
            is DetailsAction.OnUpdateDue -> { onUpdateDue(action.icsDateTime) }
            DetailsAction.OnShare -> onShare()
            is DetailsAction.OnPin -> { onPinIcalEntry(action.pin) }
            is DetailsAction.OnUpdateStatus -> { onUpdateStatus(action.status) }
            is DetailsAction.OnUpdateProgress -> { onUpdateTaskProgress(action.percent) }
        }
    }

    private fun onShare() {
        _state = _state.copy(showMoreBottomSheet = false)
        viewModelScope.launch {
            val categoryLabel = getString(Res.string.category)
            shareManager.share(
                ShareContent(
                    subject = state.icalEntry.summary ?: "",
                    body = state.icalEntry.getPlainTextForShare(categoryLabel)
                )
            )
        }
    }


    @OptIn(ExperimentalTime::class)
    private fun onUpdateSummary(newSummary: String) {
        _state = _state.copy(
            icalEntry = _state.icalEntry.copy(
                summary = newSummary,
                lastModified = IcsDateTime.now(),
                syncState = if(state.icalEntry.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                        SyncState.USER_DECIDED_CLIENT_WINS
                    else
                        SyncState.LOCAL_MODIFIED
            )
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun onUpdateDescription(newDescription: String) {
        _state = _state.copy(
            icalEntry = _state.icalEntry.copy(
                description = newDescription,
                lastModified = IcsDateTime.now(),
                syncState = if(state.icalEntry.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                    SyncState.USER_DECIDED_CLIENT_WINS
                else
                    SyncState.LOCAL_MODIFIED
            )
        )
    }

    private fun onPinIcalEntry(pin: Boolean) {
        if(pin)
            onUpdateCategories(IcalEntry.PINNED_CATEGORY, null)
        else
            onUpdateCategories(null, IcalEntry.PINNED_CATEGORY)
    }

    private fun onUpdateCategories(addCategory: String?, removeCategory: String?) {
        _state = _state.copy(
            icalEntry = _state.icalEntry.copy(
                categories = _state.icalEntry.categories.let {
                    if(addCategory != null)
                        it.plus(addCategory)
                    else if (removeCategory != null)
                        it.minus(removeCategory)
                    else
                        it
                },
                lastModified = IcsDateTime.now(),
                syncState = if(state.icalEntry.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                    SyncState.USER_DECIDED_CLIENT_WINS
                else
                    SyncState.LOCAL_MODIFIED
            )
        )
    }

    private fun onUpdateStatus(status: Status?) {
        _state = _state.copy(
            icalEntry = _state.icalEntry.copy(
                status = status,
                percentComplete = if(state.icalEntry.isTask()) {
                    when(status) {
                        Status.COMPLETED -> 100L
                        Status.IN_PROCESS -> when(_state.icalEntry.percentComplete) {
                            0L -> 1L
                            in 1L .. 99L -> _state.icalEntry.percentComplete
                            100L -> 99L
                            else -> _state.icalEntry.percentComplete
                        }
                        Status.NEEDS_ACTION -> 0L
                        else -> _state.icalEntry.percentComplete
                    }
                } else 0,
                lastModified = IcsDateTime.now(),
                syncState = if(state.icalEntry.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                    SyncState.USER_DECIDED_CLIENT_WINS
                else
                    SyncState.LOCAL_MODIFIED
            )
        )
    }

    private fun onUpdateTaskProgress(percent: Long) {
        _state = _state.copy(
            icalEntry = _state.icalEntry.copy(
                status = when(percent) {
                    0L -> if(state.icalEntry.status == Status.NEEDS_ACTION) Status.NEEDS_ACTION else null
                    in 1L..99L -> Status.IN_PROCESS
                    100L -> Status.COMPLETED
                    else -> null
                },
                percentComplete = percent,
                lastModified = IcsDateTime.now(),
                syncState = if(state.icalEntry.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                    SyncState.USER_DECIDED_CLIENT_WINS
                else
                    SyncState.LOCAL_MODIFIED
            )
        )
    }


    @OptIn(ExperimentalTime::class)
    private fun onUpdateColor(newColor: Color?) {
        _state = _state.copy(
            icalEntry = _state.icalEntry.copy(
                color = newColor,
                lastModified = IcsDateTime.now(),
                syncState = if(state.icalEntry.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                    SyncState.USER_DECIDED_CLIENT_WINS
                else
                    SyncState.LOCAL_MODIFIED
            )
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun onUpdateDtStart(newDtStart: IcsDateTime?) {

        if(_state.icalEntry.isJournal() && newDtStart == null)   // for safety only, setting date to null for journals would convert it to a note, not allowed.
            return

        _state = _state.copy(
            icalEntry = _state.icalEntry.copy(
                dtStart = newDtStart,
                lastModified = IcsDateTime.now(),
                syncState = if(state.icalEntry.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                    SyncState.USER_DECIDED_CLIENT_WINS
                else
                    SyncState.LOCAL_MODIFIED
            )
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun onUpdateDue(newDue: IcsDateTime?) {

        _state = _state.copy(
            icalEntry = _state.icalEntry.copy(
                due = newDue,
                lastModified = IcsDateTime.now(),
                syncState = if(state.icalEntry.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                    SyncState.USER_DECIDED_CLIENT_WINS
                else
                    SyncState.LOCAL_MODIFIED
            )
        )
    }

    private fun onNavigateUp(navigateUp: Boolean) {
        _state = _state.copy(navigateUp = navigateUp)
    }

    private fun onDispose() {
        if(getPlatform().platform == Platforms.WASM)
            syncAndAwaitResult()
        else
            platformSyncTrigger.requestImmediatePush(state.icalEntry.calendarId)
    }

    private fun onRestoreEntry() {
        if(state.icalEntry.syncState == SyncState.LOCAL_DELETED)
            saveIcalEntry(SyncState.LOCAL_MODIFIED)
        else
            loadCopy(state.icalEntry.id, true)
    }

    fun showDeleteDialog(showDialog: Boolean) {
        _state = _state.copy(showDeleteDialog = showDialog)
    }

    private fun saveIcalEntry(syncState: SyncState, navigateUp: Boolean = false) {

        _state = _state.copy(
            icalEntry = _state.icalEntry.copy(syncState = syncState),
            showDeleteDialog = false,
            navigateUp = navigateUp
        )

        viewModelScope.launch { 
            getDatabase().insertOrUpdateIcalEntry(_state.icalEntry)
        }
        Napier.d("Entry saved")
    }


    private fun saveAndAwaitSync(syncState: SyncState) {
        saveIcalEntry(syncState)
        syncAndAwaitResult()
    }

    private fun syncAndAwaitResult() {     // TODO: Take SyncConflictStrategy from Settings

        _state = _state.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val calendar = getDatabase().calendar_dtoQueries.getCalendarById(_state.icalEntry.calendarId).awaitAsOneOrNull()?.toDomain() ?: throw Exception(
                    getString(Res.string.unexpected_error_occurred)
                )
                val homeCollection = getDatabase().home_collection_dtoQueries.getHomeCollectionsById(calendar.homeCollectionId).awaitAsOneOrNull()?.toDomain() ?: throw Exception(
                    getString(Res.string.unexpected_error_occurred)
                )
                val principal = getDatabase().principal_dtoQueries.getPrincipalById(homeCollection.principalId).awaitAsOneOrNull()?.toDomain() ?: throw Exception(
                    getString(Res.string.unexpected_error_occurred)
                )

                val credentials = credentialStore.load(principal.principalUrl) ?: throw Exception(getString(Res.string.credentials_not_found))
                val client = HttpClientFactory.create(getPlatformEngine(), credentials.username, credentials.password)

                SyncCoordinator(getDatabase(), client).pushDirtyIcalEntry(_state.icalEntry, calendar)
                val processedIcalEntry = getDatabase().icalentry_dtoQueries.getIcalEntryByUid(_state.icalEntry.uid).awaitAsOneOrNull()?.toDomain() ?: throw Exception(
                    getString(Res.string.unexpected_error_occurred)
                )

                when(processedIcalEntry.syncState) {
                    SyncState.LOCAL_MODIFIED, SyncState.SYNCED -> {
                        _state = _state.copy(
                            snackbarText = getString(Res.string.entry_successfully_saved),
                            isLoading = false,
                            icalEntry = processedIcalEntry,
                            navigateUp = false
                        )
                    }
                    SyncState.LOCAL_DELETED, SyncState.REMOTE_DELETED_LOCAL_TRASHBIN -> {
                        _state = _state.copy(
                            snackbarText = getString(Res.string.entry_deleted),
                            isLoading = false,
                            icalEntry = processedIcalEntry,
                            navigateUp = true
                        )
                    }
                    SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED, SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED, SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED -> {
                        _state = _state.copy(
                            snackbarText = getString(Res.string.sync_conflict_detected),
                            isLoading = false,
                            icalEntry = processedIcalEntry,
                            navigateUp = false
                        )
                    }

                    SyncState.USER_DECIDED_CLIENT_WINS, SyncState.USER_DECIDED_SERVER_WINS -> {
                        _state = _state.copy(
                            isLoading = false,
                            icalEntry = processedIcalEntry,
                            navigateUp = false
                        )
                    }

                }
            } catch (e: Exception) {
                _state = _state.copy(
                    snackbarText = e.message,
                    isLoading = false
                )
            }
        }
    }
}