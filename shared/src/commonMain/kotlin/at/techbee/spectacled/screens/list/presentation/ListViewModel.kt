package at.techbee.spectacled.screens.list.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.mapper.dto.CATEGORY_SPLIT_DELIMITER
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.core.mapper.dto.toDto
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

class ListViewModel(
    private val databaseDriverFactory: DatabaseDriverFactory,
    private val credentialStore: PlatformCredentialStore,
    private val syncTrigger: PlatformSyncTrigger,
    private val userAppPreferencesStore: PlatformUserAppPreferencesStore,
    val spectacledVariant: SpectacledVariant
): ViewModel() {

    private val _state = MutableStateFlow(ListState())
    val state = _state.asStateFlow()
    val dragAndDropList = mutableStateListOf<IcalEntry>()

    private var observationJob: Job? = null

    private suspend fun getDatabase() = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)


    fun load(calendarId: Long) {
        observationJob?.cancel()

        _state.update { it.copy(
            isRefreshing = true,
            errorMessage = null,
            navigateUp = false,
            snackbarText = null,
            listSortedBy = when {
                userAppPreferencesStore.listSortedBy != null -> userAppPreferencesStore.listSortedBy!!
                spectacledVariant == SpectacledVariant.JOURNALS -> ListSortedBy.DATE
                else -> ListSortedBy.CREATED
            },
            listSortedByAscending = userAppPreferencesStore.listSortedByAscending,
            listLayout = when {
                userAppPreferencesStore.listLayout != null -> userAppPreferencesStore.listLayout!!
                spectacledVariant == SpectacledVariant.JOURNALS -> ListLayout.LIST
                else -> ListLayout.STAGGERED_GRID
            },
            listCollapsedGroups = userAppPreferencesStore.listCollapsedGroups,
            spectacledVariant = spectacledVariant
        ).recompute() }

        userAppPreferencesStore.lastUsedCalendarId = calendarId

        observationJob = viewModelScope.launch {
            try {
                val principal = getDatabase().principal_dtoQueries.getPrincipalForCalendar(calendarId).awaitAsOneOrNull()?.toDomain() ?: throw NullPointerException("Principal not found")
                val credentials = credentialStore.load(principal.principalUrl) ?: throw NullPointerException("Credentials not found")

                _state.update { it.copy(
                    principal = principal,
                    credentials = credentials
                ).recompute() }

                launch { observeCalendar(calendarId) }
                launch { observeIcalentries() }
                launch { observeColors() }
                launch { observeCategories() }

            } catch (_: NullPointerException) {
                _state.update { it.copy(navigateUp = true, isRefreshing = false).recompute() }
            }
        }
    }

    private suspend fun observeCalendar(calendarId: Long) {
        getDatabase()
            .calendar_dtoQueries.getCalendarById(calendarId)
            .asFlow()
            .collect { calendarFlow ->
                val emittedCalendar = calendarFlow.awaitAsOneOrNull()?.toDomain() ?: return@collect
                _state.update { it.copy(
                    calendar = emittedCalendar
                ).recompute() }
            }
    }

    private suspend fun observeIcalentries() {
        getDatabase()
            .icalentry_dtoQueries.getIcalEntriesByCalendar(_state.value.calendar.id)
            .asFlow()
            .collect { journalsFlow ->
                val emittedIcalEntries = journalsFlow.awaitAsList().map { vjournalDto -> vjournalDto.toDomain() }
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
        getDatabase()
            .icalentry_dtoQueries.getAllColors()
            .asFlow()
            .collect { colorsFlow ->
                val emittedColors = colorsFlow.awaitAsList().map { Color(it) }
                _state.update { it.copy(
                    allColors = emittedColors
                ).recompute() }
            }
    }

    private suspend fun observeCategories() {
        getDatabase()
            .icalentry_dtoQueries.getAllCategories()
            .asFlow()
            .collect { categoriesFlow ->
                val allCategories = mutableSetOf<String>()
                categoriesFlow.awaitAsList().let { unsplitCategories ->
                    unsplitCategories.forEach { allCategories.addAll(it.split(CATEGORY_SPLIT_DELIMITER)) }
                }
                _state.update { it.copy(
                    allCategories = allCategories.toList()
                ).recompute() }
            }
    }


    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun onAction(action: ListAction) {
        when(action) {
            is ListAction.OnListFilterCriteriaChanged -> {
                updateList(_state.value.listSortedBy, _state.value.listSortedByAscending, action.listFilterCriteria)
            }
            is ListAction.OnTriggerSync -> syncTrigger.requestImmediate(listOf(_state.value.calendar.id))
            is ListAction.OnIcalEntryClicked -> {  _state.update { it.copy(navigateToIcalEntryId = action.id).recompute() } }
            is ListAction.OnSortedByChanged -> {
                updateList(action.listSortedBy, action.listSortedByAscending, _state.value.listFilterCriteria)
                userAppPreferencesStore.listSortedBy = action.listSortedBy
                userAppPreferencesStore.listSortedByAscending = action.listSortedByAscending
            }
            is ListAction.OnViewModeChanged -> {
                _state.update { it.copy(listLayout = action.listLayout).recompute() }
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
                _state.update { it.copy(navigateUp = action.navigateUp).recompute() }
            }
            is ListAction.OnUpdateSnackbar -> { _state.update { it.copy(snackbarText = action.message).recompute() } }
            ListAction.OnToggleShowDeletedItems -> { _state.update { it.copy(showDeletedItems = !it.showDeletedItems).recompute() } }
            is ListAction.OnToggleMultiselectItem -> toggleMultiselectItem(action.icalEntryId)
            ListAction.OnClearMultiselectItems -> { _state.update { it.copy(multiselectItems = null).recompute() } }
            is ListAction.OnShowDeleteSelectedItemsDialog -> { _state.update { it.copy(showDeleteSelectedItemsDialog = action.showDialog).recompute() }}
            ListAction.OnDeleteSelectedItems -> onDeleteSelectedItems()
            is ListAction.OnUpdateOrderNo -> onUpdateOrderNo(action.fromIndex, action.toIndex)
            ListAction.OnPersistOrderNo -> onPersistOrderNo()
            is ListAction.OnToggleListGroupExpanded -> onToggleListGroupExpanded(action.listGroup)
            is ListAction.OnShowUpdateColorOfSelectedBottomSheet -> {
                _state.update {
                    if(action.show)
                        it.copy(showUpdateColorOfSelectedBottomSheet = true).recompute()
                    else {
                        it.copy(showUpdateColorOfSelectedBottomSheet = false, multiselectItems = null).recompute()
                    }
                }
            }
            is ListAction.OnUpdateColorOfSelected -> { onUpdateColorOfSelectedItems(action.color) }
            ListAction.OnSelectAllMultiselectItems -> { _state.update { it.copy(multiselectItems = it.displayMap.flatMap { it.value }.map { it.id }).recompute() } }
            is ListAction.OnDraggingIcalEntry -> { _state.update { it.copy(draggingIcalEntryId = action.icalEntryId).recompute() } }
            is ListAction.OnShowUpdateCategoryOfSelectedBottomSheet -> {
                _state.update {
                    if(action.show)
                        it.copy(showUpdateCategoryOfSelectedBottomSheet = true).recompute()
                    else {
                        it.copy(showUpdateCategoryOfSelectedBottomSheet = false, multiselectItems = null).recompute()
                    }
                }
            }
            is ListAction.OnShowDateSelectorBottomSheet -> { _state.update { it.copy(showDateSelectorBottomSheet = action.show).recompute() } }
            is ListAction.OnUpdateCategoryOfSelected -> { onUpdateCategoryOfSelectedItems(action.addCategory, action.removeCategory) }
            is ListAction.OnTogglePinEntry -> { onUpdatePinOfSelectedItems(action.pin) }
            is ListAction.OnGoToSelectedDate -> { onGoToDate(action.selectedDate) }
            is ListAction.OnToggleProgress -> { onUpdateProgress(action.icalEntryId) }
        }
    }


    private fun onUpdatePinOfSelectedItems(pin: Boolean) {
        if(pin)
            onUpdateCategoryOfSelectedItems(IcalEntry.PINNED_CATEGORY, null)
        else
            onUpdateCategoryOfSelectedItems(null, IcalEntry.PINNED_CATEGORY)
    }

    private fun onUpdateCategoryOfSelectedItems(addCategory: String?, removeCategory: String?) {
        viewModelScope.launch {
            getDatabase().icalentry_dtoQueries.transaction {
                _state.value.multiselectItems?.forEach { id ->
                    _state.value.icalEntries.find { it.id == id }?.let { icalEntry ->
                        var newCategories = icalEntry.categories
                        if (addCategory?.isNotBlank() == true && !newCategories.contains(addCategory)) {
                            newCategories = newCategories + addCategory
                        }
                        if (removeCategory?.isNotBlank() == true && newCategories.contains(removeCategory)) {
                            newCategories = newCategories - removeCategory
                        }

                        if (newCategories != icalEntry.categories) {
                            icalEntry.copy(
                                categories = newCategories,
                                lastModified = IcsDateTime.now(),
                                syncState = if (icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else icalEntry.syncState
                            ).toDto().let { copyDto ->
                                getDatabase().icalentry_dtoQueries.updateCategory(
                                    newCategories = copyDto.categories,
                                    lastModified = copyDto.lastModified,
                                    syncState = copyDto.syncState,
                                    id = copyDto.id
                                )
                            }
                        }
                    }
                }
            }
            //platformSyncTrigger.requestImmediatePush(_state.value.calendar.id)
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
        viewModelScope.launch {
            _state.value.multiselectItems?.let { getDatabase().icalentry_dtoQueries.markAsDeleted(it) }
            syncTrigger.requestImmediatePush(_state.value.calendar.id)
            syncTrigger.triggerWidgetUpdate()
            _state.update { it.copy(multiselectItems = null, showDeleteSelectedItemsDialog = false).recompute() }
        }
    }


    private fun onUpdateColorOfSelectedItems(color: Color?) {
        viewModelScope.launch {

            getDatabase().icalentry_dtoQueries.transaction {
                _state.value.multiselectItems?.forEach { id ->
                    _state.value.icalEntries.find { it.id == id }?.let { icalEntry ->

                        icalEntry.copy(
                            color = if(color == Color.Unspecified) null else color,
                            lastModified = IcsDateTime.now(),
                            syncState = if (icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else icalEntry.syncState
                        ).toDto().let { copyDto ->
                            getDatabase().icalentry_dtoQueries.updateColor(
                                newColor = copyDto.color,
                                lastModified = copyDto.lastModified,
                                syncState = copyDto.syncState,
                                id = copyDto.id
                            )
                        }
                    }
                }
            }
            //platformSyncTrigger.requestImmediatePush(_state.value.calendar.id)
        }
    }

    private fun onUpdateProgress(icalEntryId: Long) {

        viewModelScope.launch {

            val icalEntry = getDatabase().icalentry_dtoQueries.getIcalEntryById(icalEntryId).awaitAsOneOrNull()?.toDomain() ?: return@launch

            val newPercent = if(icalEntry.percentComplete in 0L .. 99L) 100L else 0L

            icalEntry.copy(
                percentComplete = newPercent,
                status = when(newPercent) {
                    0L -> null
                    in 1L..99L -> Status.IN_PROCESS
                    100L -> Status.COMPLETED
                    else -> icalEntry.status
                },
                lastModified = IcsDateTime.now(),
                syncState = if (icalEntry.syncState == SyncState.SYNCED) SyncState.LOCAL_MODIFIED else icalEntry.syncState
            ).toDto().let { copyDto ->
                getDatabase().icalentry_dtoQueries.updateProgress(
                    newPercent = copyDto.percentComplete,
                    newStatus = copyDto.status,
                    lastModified = copyDto.lastModified,
                    syncState = copyDto.syncState,
                    id = copyDto.id
                )
                syncTrigger.triggerWidgetUpdate()
            }
        }
    }

    private fun toggleMultiselectItem(icalEntryId: Long?) {

        if(icalEntryId == null && _state.value.multiselectItems == null) {
            _state.update { it.copy(multiselectItems = emptyList()).recompute() }
            return
        } else if(icalEntryId == null && _state.value.multiselectItems != null) {
            _state.update { it.copy(multiselectItems = null).recompute() }
            return
        }

        val newMultiselectList = _state.value.multiselectItems?.let { currentMultiselectItems ->
            if(_state.value.multiselectItems?.contains(icalEntryId) == true)
                currentMultiselectItems.minus(icalEntryId!!)
            else
                currentMultiselectItems.plus(icalEntryId!!)
        } ?: listOf(icalEntryId!!)

        if(newMultiselectList.isEmpty())
            _state.update { it.copy(multiselectItems = null).recompute() }
        else
            _state.update { it.copy(multiselectItems = newMultiselectList).recompute() }
    }

    private fun onUpdateOrderNo(fromIndex: Int, toIndex: Int) {

        if(fromIndex == toIndex)
            return

        dragAndDropList.apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    private fun onPersistOrderNo() {
        viewModelScope.launch {
            getDatabase().icalentry_dtoQueries.transaction {
                dragAndDropList.forEachIndexed { index, icalEntry ->
                    getDatabase().icalentry_dtoQueries.updateOrderNo(index.toLong(), icalEntry.id)
                }
            }
        }
    }

    private fun onToggleListGroupExpanded(listGroup: String) {
        _state.update {
            it.copy(
                listCollapsedGroups = if(it.listCollapsedGroups.contains(listGroup))
                    it.listCollapsedGroups.minus(listGroup)
                else
                    it.listCollapsedGroups.plus(listGroup)
            ).recompute()
        }
        userAppPreferencesStore.listCollapsedGroups = _state.value.listCollapsedGroups
    }

    private fun onGoToDate(selectedDate: IcsDateTime?) {
        selectedDate?.let { _state.update { it.copy(scrollToDate = selectedDate).recompute() } }
    }
}
