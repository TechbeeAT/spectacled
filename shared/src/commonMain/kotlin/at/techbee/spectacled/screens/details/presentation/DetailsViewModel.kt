package at.techbee.spectacled.screens.details.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.PlatformFileLauncher
import at.techbee.spectacled.screens.core.PlatformFileManager
import at.techbee.spectacled.screens.core.PlatformShareManager
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.ShareContent
import at.techbee.spectacled.screens.core.SyncCoordinator
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.claude.ClaudeRemoteResponseResult
import at.techbee.spectacled.screens.core.data.claude.KtorRemoteClaudeDataSource
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.webdav.WebDavRemoteIcalEntryDataSource
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.AttachmentSyncState
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.MIMETYPE_SVG
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.ioDispatcher
import at.techbee.spectacled.screens.core.presentation.components.PathData
import at.techbee.spectacled.screens.core.presentation.components.PathDataSvgConverter
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.http.Url
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DetailsViewModel(
    private val calendarRepository: CalendarRepository,
    private val icalEntryRepository: IcalEntryRepository,
    private val credentialStore: PlatformCredentialStore,
    private val fileManager: PlatformFileManager,
    private val fileLauncher: PlatformFileLauncher,
    private val client: HttpClient,
    private val webDavIcalEntryDataSource: WebDavRemoteIcalEntryDataSource,
    private val platformSyncTrigger: PlatformSyncTrigger,
    private val shareManager: PlatformShareManager,
    private val userAppPreferencesStore: PlatformUserAppPreferencesStore,
    private val spectacledVariant: SpectacledVariant
): ViewModel() {

    private val _state = MutableStateFlow(DetailsState())
    val state = _state.asStateFlow()

    private var entryObservationJob: Job? = null

    init {
        viewModelScope.launch {
            _state.map { it.icalEntry }
                .debounce(500L.milliseconds) // Wait for 500ms pause in typing
                .distinctUntilChanged { old, new -> old.lastModified == new.lastModified } // Only save if last modified changed
                .collect { entry ->
                    if(!_state.value.isLoading && entry.calendarId != 0L && entry.syncState != SyncState.SYNCED)
                        saveIcalEntry(entry.syncState)
                }
        }

        viewModelScope.launch { observeColors() }
        viewModelScope.launch { observeCategories() }
        viewModelScope.launch { observeTimezones() }
        viewModelScope.launch { observeSubtasks() }

        viewModelScope.launch {
            userAppPreferencesStore.getClaudeUserApiKeyAsFlow().collect { apiKey ->
                _state.update { it.copy(claudeUserApiKey = apiKey) }
            }
        }

        viewModelScope.launch {
            val allPrincipals = calendarRepository.getAllPrincipals()
            val allHomeCollections = calendarRepository.getAllHomeCollections()
            val allCalendars = calendarRepository.getAllCalendars()

            _state.update { it.copy(
                allPrincipals = allPrincipals,
                allHomeCollections = allHomeCollections,
                allCalendars = allCalendars,
                claudeUserApiKey = userAppPreferencesStore.claudeUserApiKey
            ) }
        }
    }

    fun load(icalEntryId: Long) {

        viewModelScope.launch {
            val icalEntry = icalEntryRepository.getIcalEntryById(icalEntryId) ?: return@launch
            val calendar = calendarRepository.getCalendarById(icalEntry.calendarId) ?: return@launch

            _state.update { it.copy(
                icalEntry = icalEntry,
                originalIcalEntry = icalEntry,
                calendar = calendar,
                isLoading = false,
                isInitialized = true,
                navigateUp = false
            ) }

            observeIcalEntry(icalEntry.calendarId, icalEntry.uid)
        }
    }

    fun loadNew(calendarId: Long? = null, initialDescription: String? = null) {

        viewModelScope.launch {

            val calendar = if(calendarId == null || calendarId == 0L) null else calendarRepository.getCalendarById(calendarId)

            val newIcalEntry = IcalEntry(
                calendarId = calendar?.id ?: 0L,   // if 0L user is forced to select a calendar
                description = initialDescription,
                dtStart = if (spectacledVariant == SpectacledVariant.JOURNALS) IcsDateTime.now() else null,
                calendarComponent = spectacledVariant.mainCalendarComponent
            )

            _state.update { it.copy(
                icalEntry = newIcalEntry,
                originalIcalEntry = newIcalEntry,
                calendar = calendar,
                isLoading = false,
                isInitialized = true,
                showDeleteDialog = false,
                navigateUp = false
            ) }

            observeIcalEntry(newIcalEntry.calendarId, newIcalEntry.uid)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun loadCopy(icalEntryIdToCopy: Long, isRestoredCopy: Boolean = false) {
        viewModelScope.launch {
            //saveIcalEntry(false)
            val originalIcalEntry = icalEntryRepository.getIcalEntryById(icalEntryIdToCopy)
            if(originalIcalEntry == null) {
                _state.update { it.copy(
                    snackbarText = getString(Res.string.unexpected_error_occurred),
                    isLoading = false,
                    navigateUp = true
                ) }
                return@launch
            }
            val copiedIcalEntry = IcalEntry(
                calendarId = originalIcalEntry.calendarId,
                summary = (originalIcalEntry.summary?:"") + if (isRestoredCopy) " (${getString(Res.string.entry_restored)})" else " (${
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
            _state.update { it.copy(
                icalEntry = copiedIcalEntry,
                originalIcalEntry = copiedIcalEntry,
                isLoading = false,
                isInitialized = true,
                navigateUp = false
            ) }

            observeIcalEntry(copiedIcalEntry.calendarId, copiedIcalEntry.uid)
        }
    }

    private suspend fun observeColors() {
        icalEntryRepository.getAllColors()
            .collect { emittedColors ->
                _state.update { it.copy(
                    allColors = emittedColors
                ) }
            }
    }

    private suspend fun observeCategories() {
        icalEntryRepository.getAllCategories()
            .collect { allCategories ->
                _state.update { it.copy(
                    allCategories = allCategories
                ) }
            }
    }

    private suspend fun observeTimezones() {
        icalEntryRepository.getLastUsedTimezones()
            .collect { timezones ->
                _state.update { state ->
                    state.copy(latestUsedTimezones = timezones.map { tz -> TimeZone.of(tz) })
                }
            }
    }

    private suspend fun observeSubtasks() {
        _state.map { it.icalEntry.calendarId to it.icalEntry.uid }
            .distinctUntilChanged()
            .flatMapLatest { (calendarId, uid) ->
                icalEntryRepository.getSubtasksByParentUid(calendarId, uid)
            }
            .collect { subtasks ->
                _state.update { it.copy(subtasks = subtasks) }
            }
    }

    private fun observeIcalEntry(calendarId: Long, uid: String) {
        entryObservationJob?.cancel()
        entryObservationJob = viewModelScope.launch {
            icalEntryRepository.getIcalEntryByUidFlow(calendarId, uid)
                .collect { dbEntry ->
                    if (dbEntry == null) return@collect
                    _state.update { currentState ->
                        if (currentState.icalEntry.uid == uid) {
                            currentState.copy(
                                icalEntry = currentState.icalEntry.copy(
                                    id = dbEntry.id,
                                    syncState = dbEntry.syncState,
                                    etag = dbEntry.etag,
                                    href = dbEntry.href,
                                    attachments = currentState.icalEntry.attachments.map { currentAtt ->
                                        dbEntry.attachments.find { it.uid == currentAtt.uid }?.let { dbAtt ->
                                            currentAtt.copy(
                                                id = dbAtt.id,
                                                syncState = dbAtt.syncState,
                                                remoteUrl = dbAtt.remoteUrl
                                            )
                                        } ?: currentAtt
                                    }
                                )
                            )
                        } else currentState
                    }
                }
        }
    }


    fun onAction(action: DetailsAction) {
        when(action) {
            is DetailsAction.OnUpdateSnackbar -> { _state.update { it.copy(snackbarText = action.message) } }
            is DetailsAction.OnUpdateCategories -> onUpdateCategories(action.addCategory, action.removeCategory)
            is DetailsAction.OnUpdateColor -> onUpdateColor(action.color)
            is DetailsAction.OnUpdateUrl -> onUpdateUrl(action.url)
            is DetailsAction.OnUpdateDescription -> onUpdateDescription(action.description)
            is DetailsAction.OnUpdateSummary -> onUpdateSummary(action.summary)
            DetailsAction.OnDelete -> saveIcalEntry(syncState = SyncState.LOCAL_DELETED, navigateUp = true)
            is DetailsAction.OnNavigateUp -> onNavigateUp(action.navigateUp)
            is DetailsAction.OnShowDeleteDialog -> { _state.update { it.copy(showDeleteDialog = action.show) } }
            is DetailsAction.OnShowMoreBottomSheet -> { _state.update { it.copy(showMoreBottomSheet = action.show) } }
            is DetailsAction.OnShowCategorySelectorBottomSheet -> { _state.update { it.copy(showCategorySelectorBottomSheet = action.show) } }
            is DetailsAction.OnShowColorSelectorBottomSheet -> { _state.update { it.copy(showColorSelectorBottomSheet = action.show) } }
            is DetailsAction.OnShowAddSubtaskBottomSheet -> { _state.update { it.copy(showAddSubtaskBottomSheet = action.show) } }
            is DetailsAction.OnShowEditUrlBottomSheet -> { _state.update { it.copy(showEditUrlBottomSheet = action.show) } }
            is DetailsAction.OnShowDrawingCanvasBottomSheet -> { _state.update { it.copy(showDrawingCanvasBottomSheet = action) } }
            DetailsAction.OnCreateCopy -> { loadCopy(_state.value.icalEntry.id) }
            is DetailsAction.OnSyncConflictUpdateUserDecision -> {
                when(action.syncState) {
                    SyncState.USER_DECIDED_SERVER_WINS -> saveAndAwaitSync(action.syncState)
                    SyncState.USER_DECIDED_CLIENT_WINS ->
                        if (_state.value.icalEntry.syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED) { // treat like a new entry
                            saveIcalEntry(SyncState.REMOTE_DELETED_LOCAL_TRASHBIN)
                            loadCopy(_state.value.icalEntry.id, true)
                        } else {
                            saveIcalEntry(action.syncState)
                        }
                    SyncState.REMOTE_DELETED_LOCAL_TRASHBIN -> saveIcalEntry(action.syncState, navigateUp = true)
                    else -> {}  // not allowed
                }
            }
            DetailsAction.OnDispose -> onDispose()
            DetailsAction.OnRestoreEntry -> onRestoreEntry()
            is DetailsAction.OnShowJournalStatusPickerBottomSheet -> { _state.update { it.copy(showJournalStatusPickerBottomSheet = action.show) } }
            is DetailsAction.OnShowTaskStatusProgressPickerBottomSheet -> { _state.update { it.copy(showTaskStatusProgressPickerBottomSheet = action.show) } }
            is DetailsAction.OnUpdateDtStart -> { onUpdateDtStart(action.icsDateTime) }
            is DetailsAction.OnUpdateDue -> { onUpdateDue(action.icsDateTime) }
            DetailsAction.OnShare -> onShare()
            is DetailsAction.OnPin -> { onPinIcalEntry(action.pin) }
            is DetailsAction.OnUpdateStatus -> { onUpdateStatus(action.status) }
            is DetailsAction.OnUpdateProgress -> { onUpdateTaskProgress(action.percent) }
            is DetailsAction.OnUpdateSubtaskProgress -> { onUpdateSubtaskProgress(action.percent, action.subtaskIcalEntryId) }
            is DetailsAction.OnAddSubtask -> { insertSubtask(action.summary) }
            is DetailsAction.OnNavigateToIcalEntryId -> { _state.update { it.copy(navigateToIcalEntryId = action.id) } }
            is DetailsAction.OnPersistOrderNo -> { onPersistOrderNo(action.list)}
            DetailsAction.OnProcessWithAI -> { onProcessWithAI() }
            is DetailsAction.OnNewCalendarIdSelected -> { onNewCalendarIdSelected(action.calendarId) }
            is DetailsAction.OnAddAttachment -> { onAddAttachment(action.fileName, action.bytes, action.mimeType) }
            is DetailsAction.OnOpenAttachment -> { onOpenAttachment(action.attachmentUid) }
            is DetailsAction.OnDeleteAttachment -> { onDeleteAttachment(action.attachmentUid) }
            is DetailsAction.OnUpdateDrawing -> { onUpdateDrawing(action.replaceAttachmentUid, action.paths) }
        }
    }

    private fun onShare() {
        _state.update { it.copy(showMoreBottomSheet = false) }
        viewModelScope.launch {
            val categoryLabel = getString(Res.string.category)
            shareManager.share(
                ShareContent(
                    subject = _state.value.icalEntry.summary ?: "",
                    body = _state.value.icalEntry.getPlainTextForShare(categoryLabel)
                )
            )
        }
    }


    @OptIn(ExperimentalTime::class)
    private fun onUpdateSummary(newSummary: String) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        _state.update {
            it.copy(
                icalEntry = it.icalEntry.copy(
                    summary = newSummary,
                    lastModified = IcsDateTime.now(),
                    syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                )
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun onUpdateDescription(newDescription: String) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        _state.update {
            it.copy(
                icalEntry = it.icalEntry.copy(
                    description = newDescription,
                    lastModified = IcsDateTime.now(),
                    syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                )
            )
        }
    }

    private fun onPinIcalEntry(pin: Boolean) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        if(pin)
            onUpdateCategories(IcalEntry.PINNED_CATEGORY, null)
        else
            onUpdateCategories(null, IcalEntry.PINNED_CATEGORY)
    }

    private fun onUpdateCategories(addCategory: String?, removeCategory: String?) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        _state.update {
            it.copy(
                icalEntry = it.icalEntry.copy(
                    categories = it.icalEntry.categories.let { categories ->
                        if(addCategory != null)
                            categories.plus(addCategory)
                        else if (removeCategory != null)
                            categories.minus(removeCategory)
                        else
                            categories
                    },
                    lastModified = IcsDateTime.now(),
                    syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                )
            )
        }
    }

    private fun onUpdateStatus(status: Status?) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        _state.update {
            it.copy(
                icalEntry = it.icalEntry.copy(
                    status = status,
                    percentComplete = if(it.icalEntry.isTask()) {
                        when(status) {
                            Status.COMPLETED -> 100L
                            Status.IN_PROCESS -> when(it.icalEntry.percentComplete) {
                                0L -> 1L
                                in 1L .. 99L -> it.icalEntry.percentComplete
                                100L -> 99L
                                else -> it.icalEntry.percentComplete
                            }
                            Status.NEEDS_ACTION -> 0L
                            else -> it.icalEntry.percentComplete
                        }
                    } else 0,
                    lastModified = IcsDateTime.now(),
                    syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                )
            )
        }
    }


    private fun onUpdateColor(newColor: Color?) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        _state.update {
            it.copy(
                icalEntry = it.icalEntry.copy(
                    color = newColor,
                    lastModified = IcsDateTime.now(),
                    syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                )
            )
        }
    }

    private fun onUpdateUrl(newUrl: Url?) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        _state.update {
            it.copy(
                icalEntry = it.icalEntry.copy(
                    url = newUrl,
                    lastModified = IcsDateTime.now(),
                    syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                )
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun onUpdateDtStart(newDtStart: IcsDateTime?) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        if(_state.value.icalEntry.isJournal() && newDtStart == null)   // for safety only, setting date to null for journals would convert it to a note, not allowed.
            return

        _state.update {
            // make sure dtstart and due have the same format (all day or both with time)
            val newDue = it.icalEntry.due?.let { due ->
                if(newDtStart?.isDateOnly == true && !due.isDateOnly)
                    due.asDateOnly()
                else if(newDtStart?.isDateOnly == false && due.isDateOnly)
                    due.asDateTime()
                else
                    due
            }

            it.copy(
                icalEntry = it.icalEntry.copy(
                    dtStart = newDtStart,
                    due = newDue,
                    lastModified = IcsDateTime.now(),
                    syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                )
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun onUpdateDue(newDue: IcsDateTime?) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        _state.update {
            // make sure dtstart and due have the same format (all day or both with time)
            val newDtStart = it.icalEntry.dtStart?.let { dtStart ->
                if(newDue?.isDateOnly == true && !dtStart.isDateOnly)
                    dtStart.asDateOnly()
                else if(newDue?.isDateOnly == false && dtStart.isDateOnly)
                    dtStart.asDateTime()
                else
                    dtStart
            }

            it.copy(
                icalEntry = it.icalEntry.copy(
                    dtStart = newDtStart,
                    due = newDue,
                    lastModified = IcsDateTime.now(),
                    syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                )
            )
        }
    }

    private fun onNavigateUp(navigateUp: Boolean) {
        _state.update { it.copy(navigateUp = navigateUp) }
    }

    private fun onDispose() {
        if(getPlatform().platform == Platforms.WASM)
            syncAndAwaitResult()
        else
            platformSyncTrigger.requestImmediate(listOf(_state.value.icalEntry.calendarId))
    }

    private fun onRestoreEntry() {

        if(state.value.calendar?.canWriteContent() != true)
            return

        if(_state.value.icalEntry.syncState == SyncState.LOCAL_DELETED)
            saveIcalEntry(SyncState.LOCAL_MODIFIED)
        else
            loadCopy(_state.value.icalEntry.id, true)
    }

    fun showDeleteDialog(showDialog: Boolean) {
        _state.update { it.copy(showDeleteDialog = showDialog) }
    }

    private fun saveIcalEntry(syncState: SyncState, navigateUp: Boolean = false) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        val entryToSave = _state.value.icalEntry.copy(syncState = syncState)

        _state.update {
            it.copy(
                icalEntry = entryToSave,
                showDeleteDialog = false,
                navigateUp = navigateUp
            )
        }

        viewModelScope.launch { 
            icalEntryRepository.insertOrUpdateIcalEntry(entryToSave)
            platformSyncTrigger.triggerWidgetUpdate()
        }
        Napier.d("Entry saved")
    }


    private fun saveAndAwaitSync(syncState: SyncState) {
        saveIcalEntry(syncState)
        syncAndAwaitResult()
    }

    private fun syncAndAwaitResult(icalEntry: IcalEntry = _state.value.icalEntry) {

        _state.update { it.copy(isLoading = true) }

        // Full CalDAV sync (network + iCal parsing) — keep it off the Main dispatcher.
        viewModelScope.launch(ioDispatcher) {
            try {

                val principalUrl = calendarRepository.getPrincipalUrlForCalendarId(icalEntry.calendarId)
                    ?.let { Url(it) }
                    ?: throw Exception(
                        getString(Res.string.unexpected_error_occurred)
                    )

                val calendar = _state.value.calendar ?: throw Exception(
                    getString(Res.string.unexpected_error_occurred)
                )

                val credentials = credentialStore.load(principalUrl) ?: throw Exception(getString(Res.string.credentials_not_found))

                SyncCoordinator(calendarRepository, icalEntryRepository, fileManager, client, credentials).syncCalendarWithSyncLock(calendar)
                val processedIcalEntry = icalEntryRepository.getIcalEntryByUid(icalEntry.calendarId, icalEntry.uid) ?: throw Exception(
                    getString(Res.string.unexpected_error_occurred)
                )

                when(processedIcalEntry.syncState) {
                    SyncState.LOCAL_MODIFIED, SyncState.SYNCED -> {
                        _state.update { it.copy(
                            snackbarText = getString(Res.string.entry_successfully_saved),
                            isLoading = false,
                            icalEntry = processedIcalEntry,
                            navigateUp = false
                        ) }
                    }
                    SyncState.LOCAL_DELETED, SyncState.REMOTE_DELETED_LOCAL_TRASHBIN -> {
                        _state.update { it.copy(
                            snackbarText = getString(Res.string.entry_deleted),
                            isLoading = false,
                            icalEntry = processedIcalEntry,
                            navigateUp = true
                        ) }
                    }
                    SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED, SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED, SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED -> {
                        _state.update { it.copy(
                            snackbarText = getString(Res.string.sync_conflict_detected),
                            isLoading = false,
                            icalEntry = processedIcalEntry,
                            navigateUp = false
                        ) }
                    }

                    SyncState.USER_DECIDED_CLIENT_WINS, SyncState.USER_DECIDED_SERVER_WINS -> {
                        _state.update { it.copy(
                            isLoading = false,
                            icalEntry = processedIcalEntry,
                            navigateUp = false
                        ) }
                    }

                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    snackbarText = e.message,
                    isLoading = false
                ) }
            }
        }
    }

    private fun insertSubtask(summary: String) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        val subtask = IcalEntry.newTask().copy(
            summary = summary,
            parentUid = _state.value.icalEntry.uid,
            relType = "PARENT",
            calendarId = _state.value.icalEntry.calendarId)

        viewModelScope.launch {
            val savedSubtask = icalEntryRepository.insertOrUpdateIcalEntry(subtask)
            if(getPlatform().platform == Platforms.WASM)
                syncAndAwaitResult(savedSubtask)
            platformSyncTrigger.triggerWidgetUpdate()
        }
        Napier.d("Entry saved")
    }

    private fun onUpdateTaskProgress(percent: Long) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        _state.update {
            it.copy(icalEntry = it.icalEntry.withProgressUpdated(percent))
        }
    }

    private fun onUpdateSubtaskProgress(percent: Long, subtaskIcalEntryId: Long) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        val subtask = _state.value.subtasks.find { it.id == subtaskIcalEntryId } ?: return

        val updatedSubtask = subtask.withProgressUpdated(percent)
        viewModelScope.launch {
            icalEntryRepository.updateProgress(
                id = updatedSubtask.id,
                percentComplete = updatedSubtask.percentComplete,
                status = updatedSubtask.status,
                lastModified = updatedSubtask.lastModified,
                syncState = updatedSubtask.syncState
            )
        }
    }

    private fun onPersistOrderNo(sortedList: List<Long>) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        viewModelScope.launch {
            icalEntryRepository.updateOrderNo(sortedList)
        }
    }

    private fun onProcessWithAI() {

        if(state.value.calendar?.canWriteContent() != true)
            return

        if(state.value.claudeUserApiKey.isNullOrEmpty()) {
            _state.update {
                it.copy(
                    isLoading = false,
                    snackbarText = "API key not provided. Please update the API key in the settings."
                )
            }
            return
        }

        _state.update { it.copy(isLoading = true) }

        // Claude API network call — keep it off the Main dispatcher.
        viewModelScope.launch(ioDispatcher) {

            val remoteResult = KtorRemoteClaudeDataSource(client, state.value.claudeUserApiKey?:"").applyAiMetadata(_state.value.icalEntry)

            when(remoteResult) {
                is ClaudeRemoteResponseResult.Failed -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            snackbarText = remoteResult.message + if(!remoteResult.details.isNullOrBlank()) "(${remoteResult.details})" else ""
                        )
                    }
                }
                is ClaudeRemoteResponseResult.Success -> {

                    val processedEntry = remoteResult.icalEntry
                    val currentEntry = _state.value.icalEntry

                    _state.update {
                        it.copy(
                            icalEntry = currentEntry.copy(
                                summary = currentEntry.summary ?: processedEntry.summary,
                                //description = processedEntry.description,
                                dtStart = if(currentEntry.dtStart == null && currentEntry.isTask()) processedEntry.dtStart else currentEntry.dtStart ,
                                due = if(currentEntry.due == null && currentEntry.isTask()) processedEntry.due else currentEntry.due ,
                                categories = currentEntry.categories.plus(processedEntry.categories).distinct(),
                                lastModified = IcsDateTime.now(),
                                syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                            ),
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun onNewCalendarIdSelected(calendarId: Long) {
        loadNew(calendarId, state.value.icalEntry.description)
    }

    @OptIn(ExperimentalTime::class)
    private fun onAddAttachment(fileName: String, bytes: ByteArray, mimeType: String?, isInline: Boolean = false) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        // Writes the attachment bytes to disk — keep the file I/O off the Main dispatcher.
        viewModelScope.launch(ioDispatcher) {
            val attachmentUid = Uuid.random().toString()
            val localPath = fileManager.saveAttachment("$fileName-${attachmentUid.take(8)}", bytes)
            val newAttachment = Attachment(
                uid = attachmentUid,
                icalEntryId = _state.value.icalEntry.id,
                fileName = fileName,
                localPath = localPath,
                mimeType = mimeType,
                size = bytes.size.toLong(),
                isInline = isInline,
                syncState = AttachmentSyncState.LOCAL_MODIFIED
            )

            _state.update {
                it.copy(
                    icalEntry = it.icalEntry.copy(
                        attachments = it.icalEntry.attachments + newAttachment,
                        lastModified = IcsDateTime.now(),
                        syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                    )
                )
            }
        }
    }

    private fun onOpenAttachment(attachmentUid: String) {
        val attachment = _state.value.icalEntry.attachments.find { it.uid == attachmentUid } ?: return
        
        if (attachment.localPath != null && fileManager.exists(attachment.localPath)) {
            fileLauncher.openFile(attachment.localPath, attachment.mimeType)
        } else if (attachment.remoteUrl != null) {
            // WebDAV download + saving the file to disk — keep it off the Main dispatcher.
            viewModelScope.launch(ioDispatcher) {
                try {
                    _state.update { it.copy(downloadingAttachmentUids = it.downloadingAttachmentUids + attachmentUid) }
                    
                    val principalUrl = calendarRepository.getPrincipalUrlForCalendarId(_state.value.icalEntry.calendarId)
                        ?.let { Url(it) } ?: throw Exception("Principal not found")
                    val credentials = credentialStore.load(principalUrl) ?: throw Exception("Credentials not found")
                    
                    val bytes = webDavIcalEntryDataSource.downloadFile(Url(attachment.remoteUrl), credentials)
                    if (bytes != null) {
                        // On Web, we open directly from bytes. On Native, we save then open.
                        if (getPlatform().platform == Platforms.WASM) {
                            withContext(Dispatchers.Main) { fileLauncher.openFile(bytes, attachment.fileName ?: "file", attachment.mimeType) }
                        } else {
                            val localPath = fileManager.saveAttachment("${attachment.uid}_${attachment.fileName ?: "file"}", bytes)
                            val updatedAttachment = attachment.copy(localPath = localPath, syncState = AttachmentSyncState.SYNCED)
                            icalEntryRepository.insertOrUpdateAttachment(updatedAttachment)
                            withContext(Dispatchers.Main) { fileLauncher.openFile(localPath, attachment.mimeType) }
                        }
                    } else {
                        throw Exception("Download failed")
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(snackbarText = "Error: ${e.message}") }
                } finally {
                    _state.update { it.copy(downloadingAttachmentUids = it.downloadingAttachmentUids - attachmentUid) }
                }
            }
        }
    }

    private fun onDeleteAttachment(attachmentUid: String) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        // Deletes the attachment file from disk — keep the file I/O off the Main dispatcher.
        viewModelScope.launch(ioDispatcher) {
            val attachment = _state.value.icalEntry.attachments.find { it.uid == attachmentUid }
            if (attachment != null) {
                attachment.localPath?.let { fileManager.deleteAttachment(it) }
                
                _state.update {
                    it.copy(
                        icalEntry = it.icalEntry.copy(
                            attachments = it.icalEntry.attachments.filter { a -> a.uid != attachmentUid },
                            lastModified = IcsDateTime.now(),
                            syncState = if (it.icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else it.icalEntry.syncState
                        )
                    )
                }
            }
        }
    }

    private fun onUpdateDrawing(replaceAttachmentUid: String?, paths: List<PathData>) {

        if(state.value.calendar?.canWriteContent() != true)
            return

        val svg = PathDataSvgConverter.toSvg(paths)
        onAddAttachment(
            fileName = "drawing_" + Uuid.random().toString().take(8) + ".svg",
            bytes = svg.toByteArray(),
            mimeType = MIMETYPE_SVG,
            isInline = true
        )

        replaceAttachmentUid?.let { onDeleteAttachment(it) }
    }
}
