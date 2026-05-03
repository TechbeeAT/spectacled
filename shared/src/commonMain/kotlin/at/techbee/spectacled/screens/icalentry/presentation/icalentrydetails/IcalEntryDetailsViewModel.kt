package at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails

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
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.mapper.dto.CATEGORY_SPLIT_DELIMITER
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import at.techbee.spectacled.screens.icalentry.domain.SyncState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
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
class IcalEntryDetailsViewModel(
    private val databaseDriverFactory: DatabaseDriverFactory,
    private val credentialStore: PlatformCredentialStore,
    private val platformSyncTrigger: PlatformSyncTrigger,
    private val shareManager: PlatformShareManager,
    private val spectacledVariant: SpectacledVariant
): ViewModel() {

    private var _state by mutableStateOf(IcalEntryDetailsState())
    val state: IcalEntryDetailsState get() = _state
    
    private lateinit var database: SpectacledDatabase
    private suspend fun getDatabase() = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)

    init {
        viewModelScope.launch {
            database = getDatabase()

            launch {
                snapshotFlow { state.icalEntry } // snapshotFlow tracks reads of the entry and emits on change
                    .debounce(500L) // Wait for 500ms pause in typing
                    .distinctUntilChanged { old, new -> old.lastModified == new.lastModified } // Only save if last modified changed
                    .collect {
                        if(!state.isLoading && state.icalEntry.calendarId != 0L && state.icalEntry.syncState != SyncState.SYNCED)
                            saveIcalEntry(state.icalEntry.syncState)
                    }
            }
        }
    }

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun load(icalEntryId: Long) {

        viewModelScope.launch {

            val icalEntry = database.icalentry_dtoQueries.getIcalEntryById(icalEntryId).awaitAsOneOrNull()?.toDomain() ?: return@launch
            val calendar = database.calendar_dtoQueries.getCalendarById(icalEntry.calendarId).awaitAsOneOrNull()?.toDomain() ?: return@launch

            _state = _state.copy(
                icalEntry = icalEntry,
                originalIcalEntry = icalEntry,
                calendar = calendar,
                isLoading = false,
                navigateUp = false
            )

            launch { observeColors() }
            launch { observeCategories() }
        }
    }

    private suspend fun observeColors() {
        database
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
        database
            .icalentry_dtoQueries.getAllCategories()
            .asFlow()
            .collect { categoriesFlow ->
                val allCategories = mutableSetOf<String>()
                categoriesFlow.awaitAsList().let {
                    it.forEach { allCategories.addAll(it.split(CATEGORY_SPLIT_DELIMITER)) }
                }
                _state = _state.copy(
                    allCategories = allCategories.toList()
                )
            }
    }


    @OptIn(ExperimentalTime::class)
    fun loadNew(calendarId: Long) {

        viewModelScope.launch {
            val newIcalEntry = IcalEntry(
                calendarId = calendarId,
                dtStart = if(spectacledVariant == SpectacledVariant.JOURNALS) IcsDateTime.now() else null
            )
            val calendar = database.calendar_dtoQueries.getCalendarById(calendarId).awaitAsOneOrNull()?.toDomain() ?: return@launch

            _state = _state.copy(
                icalEntry = newIcalEntry,
                originalIcalEntry = newIcalEntry,
                calendar = calendar,
                isLoading = false,
                showDeleteDialog = false,
                navigateUp = false
            )
        }
    }

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun loadCopy(icalEntryIdToCopy: Long, isRestoredCopy: Boolean = false) {
        viewModelScope.launch {
            //saveIcalEntry(false)
            val originalIcalEntry = database.icalentry_dtoQueries.getIcalEntryById(icalEntryIdToCopy).awaitAsOneOrNull()?.toDomain()
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
                summary = originalIcalEntry.summary + if(isRestoredCopy) " (${getString(Res.string.entry_restored)})" else " (${getString(Res.string.entry_copy)})",
                description = originalIcalEntry.description,
                dtStart = originalIcalEntry.dtStart,
                categories = originalIcalEntry.categories,
                color = originalIcalEntry.color,
                extraProperties = originalIcalEntry.extraProperties
            )
            _state = _state.copy(
                icalEntry = copiedIcalEntry,
                originalIcalEntry = copiedIcalEntry,
                isLoading = false,
                navigateUp = false
            )
        }
    }


    fun onAction(action: IcalEntryDetailsAction) {
        when(action) {
            is IcalEntryDetailsAction.OnUpdateSnackbar -> { _state = _state.copy(snackbarText = action.message) }
            is IcalEntryDetailsAction.OnUpdateCategories -> onUpdateCategories(action.addCategory, action.removeCategory)
            is IcalEntryDetailsAction.OnUpdateColor -> onUpdateColor(action.color)
            is IcalEntryDetailsAction.OnUpdateDescription -> onUpdateDescription(action.description)
            is IcalEntryDetailsAction.OnUpdateSummary -> onUpdateSummary(action.summary)
            IcalEntryDetailsAction.OnDeleteIcalEntry -> saveIcalEntry(syncState = SyncState.LOCAL_DELETED, navigateUp = true)
            is IcalEntryDetailsAction.OnNavigateUp -> onNavigateUp(action.navigateUp)
            is IcalEntryDetailsAction.OnShowDeleteIcalEntryDialog -> { _state = _state.copy(showDeleteDialog = action.show) }
            is IcalEntryDetailsAction.OnShowMoreBottomSheet -> { _state = _state.copy(showMoreBottomSheet = action.show) }
            is IcalEntryDetailsAction.OnShowCategorySelectorBottomSheet -> { _state = _state.copy(showCategorySelectorBottomSheet = action.show) }
            is IcalEntryDetailsAction.OnShowColorSelectorBottomSheet -> { _state = _state.copy(showColorSelectorBottomSheet = action.show) }
            IcalEntryDetailsAction.OnCreateCopy -> { loadCopy(_state.icalEntry.id) }
            is IcalEntryDetailsAction.OnSyncConflictUpdateUserDecision -> {
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
            IcalEntryDetailsAction.OnDispose -> onDispose()
            IcalEntryDetailsAction.OnRestoreEntry -> onRestoreEntry()
            is IcalEntryDetailsAction.OnShowDatePickerBottomSheet -> { _state = _state.copy(showDatePickerBottomSheet = action.show) }
            is IcalEntryDetailsAction.OnShowTimePickerBottomSheet -> { _state = _state.copy(showTimePickerBottomSheet = action.show) }
            is IcalEntryDetailsAction.OnShowTimezonePickerBottomSheet -> { _state = _state.copy(showTimezonePickerBottomSheet = action.show) }
            is IcalEntryDetailsAction.OnUpdateDtStart -> { onUpdateDtStart(action.icsDateTime) }
            IcalEntryDetailsAction.OnShare -> onShare()
            is IcalEntryDetailsAction.OnPinIcalEntry -> { onPinIcalEntry(action.pin) }
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
    private fun onUpdateDtStart(newDtStart: IcsDateTime) {
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

        viewModelScope.launch { database.insertOrUpdateIcalEntry(_state.icalEntry) }
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
                val calendar = database.calendar_dtoQueries.getCalendarById(_state.icalEntry.calendarId).awaitAsOneOrNull()?.toDomain() ?: throw Exception(getString(Res.string.unexpected_error_occurred))
                val homeCollection = database.home_collection_dtoQueries.getHomeCollectionsById(calendar.homeCollectionId).awaitAsOneOrNull()?.toDomain() ?: throw Exception(getString(Res.string.unexpected_error_occurred))
                val principal = database.principal_dtoQueries.getPrincipalById(homeCollection.principalId).awaitAsOneOrNull()?.toDomain() ?: throw Exception(getString(Res.string.unexpected_error_occurred))

                val credentials = credentialStore.load(principal.principalUrl) ?: throw Exception(getString(Res.string.credentials_not_found))
                val client = HttpClientFactory.create(getPlatformEngine(), credentials.username, credentials.password)

                SyncCoordinator(database, client).pushDirtyIcalEntry(_state.icalEntry, calendar)
                val processedIcalEntry = database.icalentry_dtoQueries.getIcalEntryByUid(_state.icalEntry.uid).awaitAsOneOrNull()?.toDomain() ?: throw Exception(getString(Res.string.unexpected_error_occurred))

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