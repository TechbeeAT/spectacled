package at.techbee.spectacled.screens.note.presentation.notedetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.account.data.insertOrUpdateNote
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.SyncCoordinator
import at.techbee.spectacled.screens.core.data.HttpClientFactory
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.getPlatformEngine
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.note.domain.Note
import at.techbee.spectacled.screens.note.domain.SyncState
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.credentials_not_found
import spectacled.shared.generated.resources.entry_copy
import spectacled.shared.generated.resources.entry_deleted
import spectacled.shared.generated.resources.entry_restored
import spectacled.shared.generated.resources.entry_successfully_saved
import spectacled.shared.generated.resources.sync_conflict_detected
import spectacled.shared.generated.resources.unexpected_error_occurred
import io.github.aakira.napier.Napier
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi


@OptIn(FlowPreview::class)
class NoteDetailsViewModel(
    private val databaseDriverFactory: DatabaseDriverFactory,
    private val credentialStore: PlatformCredentialStore,
    private val platformSyncTrigger: PlatformSyncTrigger
): ViewModel() {
    
    val allCategories = mutableSetOf<String>()
    val allColors = mutableSetOf<Color>()

    private var _state by mutableStateOf(NoteDetailsState())
    val state: NoteDetailsState get() = _state
    
    private lateinit var database: SpectacledDatabase
    private suspend fun getDatabase() = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)

    init {
        viewModelScope.launch {
            database = getDatabase()

            launch {
            snapshotFlow { state.note } // snapshotFlow tracks reads of the note and emits on change
                .debounce(500L) // Wait for 500ms pause in typing
                .distinctUntilChanged { old, new -> old.lastModified == new.lastModified } // Only save if last modified changed
                .collect {
                    if(!state.isLoading && state.note.calendarId != 0L && state.note.syncState != SyncState.SYNCED)
                        saveNote(state.note.syncState)
                }
                }
        }
    }

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun load(noteId: Long) {

        viewModelScope.launch {

            val unsplitCategories = database.vjournal_dtoQueries.getAllCategories().awaitAsList()
            unsplitCategories.forEach { category ->
                allCategories.addAll(category.split(","))
            }
            allColors.addAll(database.vjournal_dtoQueries.getAllColors().awaitAsList().map { color -> Color(color) })


            val note = database.vjournal_dtoQueries.getJournalById(noteId).awaitAsOneOrNull()?.toDomain() ?: return@launch
            val calendar = database.calendar_dtoQueries.getCalendarById(note.calendarId).awaitAsOneOrNull()?.toDomain() ?: return@launch

            _state = _state.copy(
                note = note,
                originalNote = note,
                calendar = calendar,
                isLoading = false,
                navigateUp = false
            )

        }
    }


    @OptIn(ExperimentalTime::class)
    fun loadNew(calendarId: Long) {
        viewModelScope.launch {
            val newNote = Note(calendarId = calendarId)
            val calendar = database.calendar_dtoQueries.getCalendarById(calendarId).awaitAsOneOrNull()?.toDomain() ?: return@launch

            _state = _state.copy(
                note = newNote,
                originalNote = newNote,
                calendar = calendar,
                isLoading = false,
                showDeleteDialog = false,
                navigateUp = false
            )
        }

    }

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun loadCopy(noteIdToCopy: Long, isRestoredCopy: Boolean = false) {
        viewModelScope.launch {
            //saveNote(false)
            val originalNote = database.vjournal_dtoQueries.getJournalById(noteIdToCopy).awaitAsOneOrNull()?.toDomain()
            if(originalNote == null) {
                _state = _state.copy(
                    snackbarText = getString(Res.string.unexpected_error_occurred),
                    isLoading = false,
                    navigateUp = true
                )
                return@launch
            }
            val copiedNote = Note(
                calendarId = originalNote.calendarId,
                summary = originalNote.summary + if(isRestoredCopy) " (${getString(Res.string.entry_restored)})" else " (${getString(Res.string.entry_copy)})",
                description = originalNote.description,
                dtStart = originalNote.dtStart,
                categories = originalNote.categories,
                color = originalNote.color,
                extraProperties = originalNote.extraProperties
            )
            _state = _state.copy(
                note = copiedNote,
                originalNote = copiedNote,
                isLoading = false,
                navigateUp = false
            )
        }
    }


    fun onAction(action: NoteDetailsAction) {
        when(action) {
            is NoteDetailsAction.OnUpdateSnackbar -> { _state = _state.copy(snackbarText = action.message) }
            is NoteDetailsAction.OnUpdateCategories -> onUpdateCategories(action.categories)
            is NoteDetailsAction.OnUpdateColor -> onUpdateColor(action.color)
            is NoteDetailsAction.OnUpdateDescription -> onUpdateDescription(action.description)
            is NoteDetailsAction.OnUpdateSummary -> onUpdateSummary(action.summary)
            NoteDetailsAction.OnDeleteNote -> saveNote(syncState = SyncState.LOCAL_DELETED, navigateUp = true)
            //NoteDetailsAction.OnSaveNote -> saveAndAwaitSync(_state.note.syncState)
            is NoteDetailsAction.OnNavigateUp -> onNavigateUp(action.navigateUp)
            is NoteDetailsAction.OnShowDeleteNoteDialog -> { _state = _state.copy(showDeleteDialog = action.show) }
            is NoteDetailsAction.OnShowMoreBottomSheet -> { _state = _state.copy(showMoreBottomSheet = action.show) }
            is NoteDetailsAction.OnShowCategorySelectorBottomSheet -> { _state = _state.copy(showCategorySelectorBottomSheet = action.show) }
            is NoteDetailsAction.OnShowColorSelectorBottomSheet -> { _state = _state.copy(showColorSelectorBottomSheet = action.show) }
            NoteDetailsAction.OnCreateCopy -> { loadCopy(_state.note.id) }
            is NoteDetailsAction.OnSyncConflictUpdateUserDecision -> {
                when(action.syncState) {
                    SyncState.USER_DECIDED_SERVER_WINS -> saveAndAwaitSync(action.syncState)
                    SyncState.USER_DECIDED_CLIENT_WINS ->
                        if (state.note.syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED) { // treat like a new note
                            saveNote(SyncState.REMOTE_DELETED_LOCAL_TRASHBIN)
                            loadCopy(_state.note.id, true)
                        } else {
                            saveNote(action.syncState)
                        }
                    SyncState.REMOTE_DELETED_LOCAL_TRASHBIN -> saveNote(action.syncState, navigateUp = true)
                    else -> {}  // not allowed
                }
            }
            NoteDetailsAction.OnDispose -> onDispose()
            NoteDetailsAction.OnRestoreEntry -> onRestoreEntry()
            is NoteDetailsAction.OnShowDatePickerBottomSheet -> { _state = _state.copy(showDatePickerBottomSheet = action.show) }
            is NoteDetailsAction.OnShowTimePickerBottomSheet -> { _state = _state.copy(showTimePickerBottomSheet = action.show) }
            is NoteDetailsAction.OnShowTimezonePickerBottomSheet -> { _state = _state.copy(showTimezonePickerBottomSheet = action.show) }
            is NoteDetailsAction.OnUpdateDtStart -> { onUpdateDtStart(action.icsDateTime) }
        }
    }


    @OptIn(ExperimentalTime::class)
    private fun onUpdateSummary(newSummary: String) {
        _state = _state.copy(
            note = _state.note.copy(
                summary = newSummary,
                lastModified = IcsDateTime.now(),
                syncState = if(state.note.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                        SyncState.USER_DECIDED_CLIENT_WINS
                    else
                        SyncState.LOCAL_MODIFIED
            )
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun onUpdateDescription(newDescription: String) {
        _state = _state.copy(
            note = _state.note.copy(
                description = newDescription,
                lastModified = IcsDateTime.now(),
                syncState = if(state.note.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                    SyncState.USER_DECIDED_CLIENT_WINS
                else
                    SyncState.LOCAL_MODIFIED
            )
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun onUpdateCategories(newCategories: List<String>) {
        _state = _state.copy(
            note = _state.note.copy(
                categories = newCategories,
                lastModified = IcsDateTime.now(),
                syncState = if(state.note.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                    SyncState.USER_DECIDED_CLIENT_WINS
                else
                    SyncState.LOCAL_MODIFIED
            )
        )
    }


    @OptIn(ExperimentalTime::class)
    private fun onUpdateColor(newColor: Color?) {
        _state = _state.copy(
            note = _state.note.copy(
                color = newColor,
                lastModified = IcsDateTime.now(),
                syncState = if(state.note.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
                    SyncState.USER_DECIDED_CLIENT_WINS
                else
                    SyncState.LOCAL_MODIFIED
            )
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun onUpdateDtStart(newDtStart: IcsDateTime) {
        _state = _state.copy(
            note = _state.note.copy(
                dtStart = newDtStart,
                lastModified = IcsDateTime.now(),
                syncState = if(state.note.syncState == SyncState.USER_DECIDED_CLIENT_WINS)
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
            platformSyncTrigger.requestImmediatePush(state.note.calendarId)
    }

    private fun onRestoreEntry() {
        if(state.note.syncState == SyncState.LOCAL_DELETED)
            saveNote(SyncState.LOCAL_MODIFIED)
        else
            loadCopy(state.note.id, true)
    }

    fun showDeleteDialog(showDialog: Boolean) {
        _state = _state.copy(showDeleteDialog = showDialog)
    }

    private fun saveNote(syncState: SyncState, navigateUp: Boolean = false) {

        _state = _state.copy(
            note = _state.note.copy(syncState = syncState),
            showDeleteDialog = false,
            navigateUp = navigateUp
        )

        viewModelScope.launch { database.insertOrUpdateNote(_state.note) }
        Napier.d("Note saved")
    }


    private fun saveAndAwaitSync(syncState: SyncState) {
        saveNote(syncState)
        syncAndAwaitResult()
    }

    private fun syncAndAwaitResult() {     // TODO: Take SyncConflictStrategy from Settings

        _state = _state.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val calendar = database.calendar_dtoQueries.getCalendarById(_state.note.calendarId).awaitAsOneOrNull()?.toDomain() ?: throw Exception(getString(Res.string.unexpected_error_occurred))
                val homeCollection = database.home_collection_dtoQueries.getHomeCollectionsById(calendar.homeCollectionId).awaitAsOneOrNull()?.toDomain() ?: throw Exception(getString(Res.string.unexpected_error_occurred))
                val principal = database.principal_dtoQueries.getPrincipalById(homeCollection.principalId).awaitAsOneOrNull()?.toDomain() ?: throw Exception(getString(Res.string.unexpected_error_occurred))

                val credentials = credentialStore.load(principal.principalUrl) ?: throw Exception(getString(Res.string.credentials_not_found))
                val client = HttpClientFactory.create(getPlatformEngine(), credentials.username, credentials.password)

                //database!!.insertOrUpdateNote(_state.note)
                SyncCoordinator(database, client).pushDirtyNote(_state.note, calendar)
                val processedNote = database.vjournal_dtoQueries.getJournalByUid(_state.note.uid).awaitAsOneOrNull()?.toDomain() ?: throw Exception(getString(Res.string.unexpected_error_occurred))

                when(processedNote.syncState) {
                    SyncState.LOCAL_MODIFIED, SyncState.SYNCED -> {
                        _state = _state.copy(
                            snackbarText = getString(Res.string.entry_successfully_saved),
                            isLoading = false,
                            note = processedNote,
                            navigateUp = false
                        )
                    }
                    SyncState.LOCAL_DELETED, SyncState.REMOTE_DELETED_LOCAL_TRASHBIN -> {
                        _state = _state.copy(
                            snackbarText = getString(Res.string.entry_deleted),
                            isLoading = false,
                            note = processedNote,
                            navigateUp = true
                        )
                    }
                    SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED, SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED, SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED -> {
                        _state = _state.copy(
                            snackbarText = getString(Res.string.sync_conflict_detected),
                            isLoading = false,
                            note = processedNote,
                            navigateUp = false
                        )
                    }

                    SyncState.USER_DECIDED_CLIENT_WINS, SyncState.USER_DECIDED_SERVER_WINS -> {
                        _state = _state.copy(
                            isLoading = false,
                            note = processedNote,
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