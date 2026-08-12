package at.techbee.spectacled.screens.list.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.MoveIcalEntriesUseCase
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.ai.AiDeriveEntriesResult
import at.techbee.spectacled.screens.core.data.ai.newAiBatchCategory
import at.techbee.spectacled.screens.core.data.ai.toIcalEntries
import at.techbee.spectacled.screens.core.data.claude.KtorRemoteClaudeDataSource
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import at.techbee.spectacled.screens.core.ioDispatcher
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import io.ktor.client.HttpClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.recurring_entry_read_only_snackbar
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

class ListViewModel(
    private val calendarRepository: CalendarRepository,
    private val icalEntryRepository: IcalEntryRepository,
    private val credentialStore: PlatformCredentialStore,
    private val syncTrigger: PlatformSyncTrigger,
    private val userAppPreferencesStore: PlatformUserAppPreferencesStore,
    private val moveIcalEntriesUseCase: MoveIcalEntriesUseCase,
    private val client: HttpClient,
    val spectacledVariant: SpectacledVariant
): ViewModel() {

    private val _state = MutableStateFlow(ListState())
    val state = _state.asStateFlow()
    val dragAndDropList = mutableStateListOf<IcalEntry>()

    private var observationJob: Job? = null


    fun load(calendarId: Long) {
        observationJob?.cancel()

        _state.update { it.copy(
            isRefreshing = true,
            isInitialized = true,
            errorMessage = null,
            navigateUp = false,
            snackbarText = null,
            listSortedBy = userAppPreferencesStore.listSortedBy,
            listSortedByAscending = userAppPreferencesStore.listSortedByAscending,
            listLayout = userAppPreferencesStore.listLayout,
            listCollapsedGroups = userAppPreferencesStore.listCollapsedGroups,
            spectacledVariant = spectacledVariant
        ).recompute() }

        userAppPreferencesStore.lastUsedCalendarId = calendarId

        // Off the Main dispatcher: reads the credential store (disk) and keeps the flow
        // collectors — including the CPU-bound recompute() — off the UI thread.
        observationJob = viewModelScope.launch(ioDispatcher) {
            val principal = calendarRepository.getPrincipalForCalendar(calendarId)
            val credentials = principal?.let { credentialStore.load(it.principalUrl) }
            val calendar = calendarRepository.getCalendarById(calendarId)

            if (principal == null || credentials == null || calendar == null) {
                // The calendar can no longer be opened (deleted, or its account/credentials
                // are gone). Clear the stored preference so we don't keep returning to it.
                userAppPreferencesStore.lastUsedCalendarId = null
                _state.update { it.copy(navigateUp = true, isRefreshing = false) }
                return@launch
            }

            _state.update { it.copy(
                principal = principal,
                credentials = credentials,
                calendar = calendar
            ) }

            launch { observeCalendar(calendarId) }
            launch { observeIcalentries(calendarId) }
            launch { observeColors() }
            launch { observeCategories() }
            launch { observeClaudeApiKey() }
            launch { loadAllCollections() }
        }
    }

    private suspend fun observeClaudeApiKey() {
        userAppPreferencesStore.getClaudeUserApiKeyAsFlow()
            .collect { apiKey ->
                _state.update { it.copy(claudeApiKeyPresent = !apiKey.isNullOrEmpty()) }
            }
    }

    // Collections available as move-to targets. Loaded once; the list stays scoped to one calendar.
    private suspend fun loadAllCollections() {
        _state.update { it.copy(
            allPrincipals = calendarRepository.getAllPrincipals(),
            allHomeCollections = calendarRepository.getAllHomeCollections(),
            allCalendars = calendarRepository.getAllCalendars()
        ) }
    }

    private suspend fun observeCalendar(calendarId: Long) {
        calendarRepository.getAllCalendarsFlow()
            .collect { calendars ->
                val emittedCalendar = calendars.find { it.id == calendarId } ?: return@collect
                _state.update { it.copy(
                    calendar = emittedCalendar
                ) }
            }
    }

    private suspend fun observeIcalentries(calendarId: Long) {
        icalEntryRepository.getIcalEntriesByCalendarFlow(calendarId)
            .collect { emittedIcalEntries ->
                _state.update { it.copy(
                    icalEntries = emittedIcalEntries,
                    isRefreshing = false,
                    errorMessage = null,
                    navigateUp = false,
                    snackbarText = null
                ).recompute() }

                dragAndDropList.apply {
                    clear()
                    addAll(_state.value.displayMap.flatMap { it.value })
                    sortBy { icalEntry -> icalEntry.orderNo }
                }
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


    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun onAction(action: ListAction) {
        when(action) {
            is ListAction.OnListFilterCriteriaChanged -> {
                updateList(_state.value.listSortedBy, _state.value.listSortedByAscending, action.listFilterCriteria)
            }
            is ListAction.OnTriggerSync -> syncTrigger.requestImmediate(listOf(_state.value.calendar.id))
            is ListAction.OnIcalEntryClicked -> {  _state.update { it.copy(navigateToIcalEntryId = action.id) } }
            is ListAction.OnSortedByChanged -> {
                updateList(action.listSortedBy, action.listSortedByAscending, _state.value.listFilterCriteria)
                userAppPreferencesStore.listSortedBy = action.listSortedBy
                userAppPreferencesStore.listSortedByAscending = action.listSortedByAscending
            }
            is ListAction.OnViewModeChanged -> {
                _state.update { it.copy(listLayout = action.listLayout) }
                userAppPreferencesStore.listLayout = action.listLayout
            }
            is ListAction.OnSearchBarExpanded -> {
                _state.update { it.copy(
                    listFilterCriteria = it.listFilterCriteria.copy(
                        searchQuery = if(action.isExpanded) "" else null,
                        searchCategory = if(action.isExpanded) "" else null,
                        filterStatus = if(action.isExpanded) it.listFilterCriteria.filterStatus else null,
                    )
                ).recompute() }
            }
            is ListAction.OnNavigateUp -> {
                if(action.navigateUp)
                    userAppPreferencesStore.lastUsedCalendarId = null // reset lastUsedCalendarId
                _state.update { it.copy(navigateUp = action.navigateUp) }
            }
            is ListAction.OnUpdateSnackbar -> { _state.update { it.copy(snackbarText = action.message) } }
            ListAction.OnToggleShowDeletedItems -> { _state.update { it.copy(showDeletedItems = !it.showDeletedItems) } }
            is ListAction.OnToggleMultiselectItem -> toggleMultiselectItem(action.icalEntryId)
            ListAction.OnClearMultiselectItems -> { _state.update { it.copy(multiselectItems = null) } }
            is ListAction.OnShowDeleteSelectedItemsDialog -> { _state.update { it.copy(showDeleteSelectedItemsDialog = action.showDialog) }}
            ListAction.OnDeleteSelectedItems -> onDeleteSelectedItems()
            is ListAction.OnShowMoveSelectedItemsDialog -> { _state.update { it.copy(showMoveSelectedItemsDialog = action.showDialog) }}
            is ListAction.OnMoveSelectedItems -> onMoveSelectedItems(action.targetCalendarId)
            is ListAction.OnUpdateOrderNo -> onUpdateOrderNo(action.fromIndex, action.toIndex)
            ListAction.OnPersistOrderNo -> onPersistOrderNo()
            is ListAction.OnToggleListGroupExpanded -> onToggleListGroupExpanded(action.listGroup)
            is ListAction.OnShowUpdateColorOfSelectedBottomSheet -> {
                _state.update {
                    if(action.show)
                        it.copy(showUpdateColorOfSelectedBottomSheet = true)
                    else {
                        it.copy(showUpdateColorOfSelectedBottomSheet = false, multiselectItems = null)
                    }
                }
            }
            is ListAction.OnUpdateColorOfSelected -> { onUpdateColorOfSelectedItems(action.color) }
            // Recurring entries are read-only, so "select all" skips them - they can never enter a selection.
            ListAction.OnSelectAllMultiselectItems -> { _state.update { it.copy(multiselectItems = it.displayMap.flatMap { map -> map.value }.filterNot { icalEntry -> icalEntry.isRecurring() }.map { icalEntry -> icalEntry.id }) } }
            is ListAction.OnDraggingIcalEntry -> { _state.update { it.copy(draggingIcalEntryId = action.icalEntryId) } }
            is ListAction.OnShowUpdateCategoryOfSelectedBottomSheet -> {
                _state.update {
                    if(action.show)
                        it.copy(showUpdateCategoryOfSelectedBottomSheet = true)
                    else {
                        it.copy(showUpdateCategoryOfSelectedBottomSheet = false, multiselectItems = null)
                    }
                }
            }
            is ListAction.OnShowDateSelectorBottomSheet -> { _state.update { it.copy(showDateSelectorBottomSheet = action.show) } }
            is ListAction.OnShowDeriveEntriesBottomSheet -> { _state.update { it.copy(showDeriveEntriesBottomSheet = action.show) } }
            is ListAction.OnDeriveEntriesFromText -> deriveEntriesFromText(action.text)
            is ListAction.OnUpdateCategoryOfSelected -> { onUpdateCategoryOfSelectedItems(action.addCategory, action.removeCategory) }
            is ListAction.OnTogglePinEntry -> { onUpdatePinOfSelectedItems(action.pin) }
            is ListAction.OnGoToSelectedDate -> { onGoToDate(action.selectedDate) }
            is ListAction.OnToggleProgress -> { onUpdateProgress(action.icalEntryId) }
        }
    }


    /**
     * Sends [text] to the AI, then creates the derived entries. Each derived parent becomes a note,
     * journal or task (the DTO -> IcalEntry mapper rejects anything else), and its subtasks are
     * created as VTODO children linked via parentUid. Every created entry is tagged with a single
     * per-generation batch category so the user can filter/delete/regenerate the batch.
     */
    private fun deriveEntriesFromText(text: String) {

        if(!state.value.calendar.canWriteContent())
            return

        if(text.isBlank())
            return

        val apiKey = userAppPreferencesStore.claudeUserApiKey
        if(apiKey.isNullOrEmpty()) {
            _state.update { it.copy(
                showDeriveEntriesBottomSheet = false,
                snackbarText = "API key not provided. Please update the API key in the settings."
            ) }
            return
        }

        _state.update { it.copy(isDerivingEntries = true) }

        val calendarId = state.value.calendar.id
        val batchCategory = newAiBatchCategory()
        // The top-level entry kind is decided by which list we're on, not by the AI.
        val entryKindHint = when(spectacledVariant) {
            SpectacledVariant.NOTES -> "note"
            SpectacledVariant.JOURNALS -> "journal entry"
            SpectacledVariant.TASKS -> "task"
        }

        // Claude API network call + inserts - keep off the Main dispatcher.
        viewModelScope.launch(ioDispatcher) {
            when(val result = KtorRemoteClaudeDataSource(client, apiKey).deriveEntries(text, entryKindHint)) {
                is AiDeriveEntriesResult.Failed -> {
                    _state.update { it.copy(
                        isDerivingEntries = false,
                        snackbarText = result.message + if(!result.details.isNullOrBlank()) " (${result.details})" else ""
                    ) }
                }
                is AiDeriveEntriesResult.Success -> {
                    // Flatten each derived entry (top-level = this list's kind, descendants = tasks)
                    // into a parent-first list, then insert in order.
                    val toInsert = result.entries.flatMap {
                        it.toIcalEntries(calendarId, batchCategory, spectacledVariant)
                    }
                    toInsert.forEach { icalEntryRepository.insertOrUpdateIcalEntry(it) }

                    if(toInsert.isNotEmpty()) {
                        syncTrigger.requestImmediate(listOf(calendarId))
                        syncTrigger.triggerWidgetUpdate()
                    }

                    _state.update { it.copy(
                        isDerivingEntries = false,
                        showDeriveEntriesBottomSheet = toInsert.isEmpty(),   // keep open if nothing was created
                        snackbarText = if(toInsert.isEmpty()) "No entries could be created from the text." else null
                    ) }
                }
            }
        }
    }

    private fun onUpdatePinOfSelectedItems(pin: Boolean) {
        if(pin)
            onUpdateCategoryOfSelectedItems(IcalEntry.PINNED_CATEGORY, null)
        else
            onUpdateCategoryOfSelectedItems(null, IcalEntry.PINNED_CATEGORY)
    }

    private fun onUpdateCategoryOfSelectedItems(addCategory: String?, removeCategory: String?) {

        if(!state.value.calendar.canWriteContent())
            return

        viewModelScope.launch {
            _state.value.multiselectItems?.forEach { id ->
                _state.value.icalEntries.find { it.id == id }?.let { icalEntry ->
                    if(icalEntry.syncState.isDeletedState() || icalEntry.isRecurring())
                        return@forEach

                    var newCategories = icalEntry.categories
                    if (addCategory?.isNotBlank() == true && !newCategories.contains(addCategory)) {
                        newCategories = newCategories + addCategory
                    }
                    if (removeCategory?.isNotBlank() == true && newCategories.contains(removeCategory)) {
                        newCategories = newCategories - removeCategory
                    }

                    if (newCategories != icalEntry.categories) {
                        icalEntryRepository.updateCategory(
                            id = icalEntry.id,
                            categories = newCategories,
                            lastModified = IcsDateTime.now(),
                            syncState = if (icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else icalEntry.syncState
                        )
                    }
                }
            }
        }
    }


    private fun updateList(listSortedBy: ListSortedBy, listSortedByAscending: Boolean, listFilterCriteria: ListFilterCriteria) {

        _state.update { it.copy(
            listSortedBy = listSortedBy,
            listSortedByAscending = listSortedByAscending,
            listFilterCriteria = listFilterCriteria,
            isRefreshing = false
        ).recompute() }
    }

    private fun onDeleteSelectedItems() {

        if(!state.value.calendar.canWriteContent())
            return

        viewModelScope.launch {
            // Recurring entries are read-only, so exclude them even if a selection somehow contains one.
            _state.value.multiselectItems?.let { icalEntryRepository.markAsDeleted(withoutRecurringEntries(it)) }
            syncTrigger.requestImmediate(listOf(_state.value.calendar.id))
            syncTrigger.triggerWidgetUpdate()
            _state.update { it.copy(multiselectItems = null, showDeleteSelectedItemsDialog = false) }
        }
    }

    private fun onMoveSelectedItems(targetCalendarId: Long) {

        // Moving deletes the entries from the current (source) collection, so it needs write access.
        if(!state.value.calendar.canWriteContent())
            return

        // Recurring entries are read-only, so exclude them even if a selection somehow contains one.
        val ids = _state.value.multiselectItems?.let { withoutRecurringEntries(it) } ?: return

        // Off the Main dispatcher: the use case may download attachments before the local move.
        viewModelScope.launch(ioDispatcher) {
            moveIcalEntriesUseCase.move(ids, targetCalendarId)
            _state.update { it.copy(multiselectItems = null, showMoveSelectedItemsDialog = false) }
        }
    }


    private fun onUpdateColorOfSelectedItems(color: Color?) {

        if(!state.value.calendar.canWriteContent())
            return

        viewModelScope.launch {
            _state.value.multiselectItems?.forEach { id ->
                _state.value.icalEntries.find { it.id == id }?.let { icalEntry ->

                    if(icalEntry.syncState.isDeletedState() || icalEntry.isRecurring())
                        return@forEach

                    icalEntryRepository.updateColor(
                        id = icalEntry.id,
                        color = if(color == Color.Unspecified) null else color,
                        lastModified = IcsDateTime.now(),
                        syncState = if (icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else icalEntry.syncState
                    )
                }
            }
        }
    }

    private fun onUpdateProgress(icalEntryId: Long) {

        if(!state.value.calendar.canWriteContent())
            return

        viewModelScope.launch {

            val icalEntry = icalEntryRepository.getIcalEntryById(icalEntryId) ?: return@launch
            if(icalEntry.syncState.isDeletedState())
                return@launch
            // Recurring entries are read-only (this app has no recurrence support).
            if(icalEntry.isRecurring())
                return@launch

            val newPercent = if(icalEntry.percentComplete in 0L .. 99L) 100L else 0L
            val updatedEntry = icalEntry.withProgressUpdated(newPercent)

            icalEntryRepository.updateProgress(
                id = updatedEntry.id,
                percentComplete = updatedEntry.percentComplete,
                status = updatedEntry.status,
                lastModified = updatedEntry.lastModified,
                syncState = updatedEntry.syncState
            )
            syncTrigger.triggerWidgetUpdate()
        }
    }

    /** Drops the ids of any recurring (read-only) entries from [ids]; recurrence isn't supported. */
    private fun withoutRecurringEntries(ids: List<Long>): List<Long> =
        ids.filterNot { id -> _state.value.icalEntries.find { it.id == id }?.isRecurring() == true }

    private fun toggleMultiselectItem(icalEntryId: Long?) {

        if(icalEntryId == null && _state.value.multiselectItems == null) {
            _state.update { it.copy(multiselectItems = emptyList()) }
            return
        } else if(icalEntryId == null && _state.value.multiselectItems != null) {
            _state.update { it.copy(multiselectItems = null) }
            return
        }

        val alreadySelected = _state.value.multiselectItems?.contains(icalEntryId) == true

        // Recurring entries are read-only (this app has no recurrence support), so they can't be
        // added to a selection - bulk delete/move/color/category would otherwise modify them.
        // Deselecting an already-selected entry is always allowed.
        if(!alreadySelected && _state.value.icalEntries.find { it.id == icalEntryId }?.isRecurring() == true) {
            viewModelScope.launch {
                _state.update { it.copy(snackbarText = getString(Res.string.recurring_entry_read_only_snackbar)) }
            }
            return
        }

        val newMultiselectList = _state.value.multiselectItems?.let { currentMultiselectItems ->
            if(alreadySelected)
                currentMultiselectItems.minus(icalEntryId!!)
            else
                currentMultiselectItems.plus(icalEntryId!!)
        } ?: listOf(icalEntryId!!)

        if(newMultiselectList.isEmpty())
            _state.update { it.copy(multiselectItems = null) }
        else
            _state.update { it.copy(multiselectItems = newMultiselectList) }
    }

    private fun onUpdateOrderNo(fromIndex: Int, toIndex: Int) {

        if(!state.value.calendar.canWriteContent())
            return

        if(fromIndex == toIndex)
            return

        dragAndDropList.apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    private fun onPersistOrderNo() {

        if(!state.value.calendar.canWriteContent())
            return

        viewModelScope.launch {
            icalEntryRepository.updateOrderNo(dragAndDropList.map { it.id })
        }
    }

    private fun onToggleListGroupExpanded(listGroup: String) {
        _state.update {
            it.copy(
                listCollapsedGroups = if(it.listCollapsedGroups.contains(listGroup))
                    it.listCollapsedGroups.minus(listGroup)
                else
                    it.listCollapsedGroups.plus(listGroup)
            )
        }
        userAppPreferencesStore.listCollapsedGroups = _state.value.listCollapsedGroups
    }

    private fun onGoToDate(selectedDate: IcsDateTime?) {
        selectedDate?.let { _state.update { it.copy(scrollToDate = selectedDate) } }
    }
}
