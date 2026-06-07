package at.techbee.spectacled.screens.list.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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

    private val _state = mutableStateOf(ListState())
    val state by _state
    val dragAndDropList = mutableStateListOf<IcalEntry>()

    private suspend fun getDatabase() = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)


    fun load(calendarId: Long) {
        _state.value = _state.value.copy(
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
        )

        userAppPreferencesStore.lastUsedCalendarId = calendarId

        viewModelScope.launch {
            try {
                val principal = getDatabase().principal_dtoQueries.getPrincipalForCalendar(calendarId).awaitAsOneOrNull()?.toDomain() ?: throw NullPointerException("Principal not found")
                val credentials = credentialStore.load(principal.principalUrl) ?: throw NullPointerException("Credentials not found")

                _state.value = _state.value.copy(
                    principal = principal,
                    credentials = credentials
                )

                launch { observeCalendar(calendarId) }
                launch { observeIcalentries() }
                launch { observeColors() }
                launch { observeCategories() }

            } catch (_: NullPointerException) {
                _state.value = _state.value.copy(navigateUp = true, isRefreshing = false)
            }
        }
    }

    private suspend fun observeCalendar(calendarId: Long) {
        getDatabase()
            .calendar_dtoQueries.getCalendarById(calendarId)
            .asFlow()
            .collect { calendarFlow ->
                val emittedCalendar = calendarFlow.awaitAsOneOrNull()?.toDomain() ?: return@collect
                _state.value = _state.value.copy(
                    calendar = emittedCalendar
                )
            }
    }

    private suspend fun observeIcalentries() {
        getDatabase()
            .icalentry_dtoQueries.getIcalEntriesByCalendar(state.calendar.id)
            .asFlow()
            .collect { journalsFlow ->
                val emittedIcalEntries = journalsFlow.awaitAsList().map { vjournalDto -> vjournalDto.toDomain() }
                _state.value = _state.value.copy(
                    icalEntries = emittedIcalEntries,
                    isRefreshing = false,
                    errorMessage = null,
                    navigateUp = false,
                    snackbarText = null
                )
                updateList()
                dragAndDropList.apply {
                    clear()
                    addAll(emittedIcalEntries.filter { emitted -> !emitted.syncState.isDeletedState() })
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
                _state.value = state.copy(
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
                categoriesFlow.awaitAsList().let { unsplitCategories ->
                    unsplitCategories.forEach { allCategories.addAll(it.split(CATEGORY_SPLIT_DELIMITER)) }
                }
                _state.value = state.copy(
                    allCategories = allCategories.toList()
                )
            }
    }


    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun onAction(action: ListAction) {
        when(action) {
            is ListAction.OnListFilterCriteriaChanged -> {
                updateList(state.listSortedBy, state.listSortedByAscending, action.listFilterCriteria)
            }
            is ListAction.OnTriggerSync -> syncTrigger.requestImmediate(listOf(state.calendar.id))
            is ListAction.OnIcalEntryClicked -> {  _state.value = _state.value.copy(navigateToIcalEntryId = action.id) }
            is ListAction.OnSortedByChanged -> {
                updateList(action.listSortedBy, action.listSortedByAscending, _state.value.listFilterCriteria)
                userAppPreferencesStore.listSortedBy = action.listSortedBy
                userAppPreferencesStore.listSortedByAscending = action.listSortedByAscending
            }
            is ListAction.OnViewModeChanged -> {
                _state.value = _state.value.copy(listLayout = action.listLayout)
                userAppPreferencesStore.listLayout = action.listLayout
            }
            is ListAction.OnSearchBarExpanded -> {
                _state.value = _state.value.copy(
                    listFilterCriteria = _state.value.listFilterCriteria.copy(
                        searchQuery = if(action.isExpanded) "" else null,
                        searchCategory = if(action.isExpanded) "" else null,
                        filterStatus = if(action.isExpanded) _state.value.listFilterCriteria.filterStatus else null,
                    )
                )
            }
            is ListAction.OnNavigateUp -> {
                if(action.navigateUp)
                    userAppPreferencesStore.lastUsedCalendarId = null // reset lastUsedCalendarId
                _state.value = _state.value.copy(navigateUp = action.navigateUp)
            }
            is ListAction.OnUpdateSnackbar -> { _state.value = _state.value.copy(snackbarText = action.message) }
            ListAction.OnToggleShowDeletedItems -> { _state.value = _state.value.copy(showDeletedItems = !_state.value.showDeletedItems) }
            is ListAction.OnToggleMultiselectItem -> toggleMultiselectItem(action.icalEntryId)
            ListAction.OnClearMultiselectItems -> { _state.value = _state.value.copy(multiselectItems = null) }
            is ListAction.OnShowDeleteSelectedItemsDialog -> { _state.value = _state.value.copy(showDeleteSelectedItemsDialog = action.showDialog)}
            ListAction.OnDeleteSelectedItems -> onDeleteSelectedItems()
            is ListAction.OnUpdateOrderNo -> onUpdateOrderNo(action.fromIndex, action.toIndex)
            ListAction.OnPersistOrderNo -> onPersistOrderNo()
            is ListAction.OnToggleListGroupExpanded -> onToggleListGroupExpanded(action.listGroup)
            is ListAction.OnShowUpdateColorOfSelectedBottomSheet -> {
                _state.value = if(action.show)
                    _state.value.copy(showUpdateColorOfSelectedBottomSheet = true)
                else {
                    _state.value.copy(showUpdateColorOfSelectedBottomSheet = false, multiselectItems = null)
                }
            }
            is ListAction.OnUpdateColorOfSelected -> { onUpdateColorOfSelectedItems(action.color) }
            ListAction.OnSelectAllMultiselectItems -> { _state.value = _state.value.copy(multiselectItems = state.displayMap.flatMap { it.value }.map { it.id }) }
            is ListAction.OnDraggingIcalEntry -> { _state.value = _state.value.copy(draggingIcalEntryId = action.icalEntryId) }
            is ListAction.OnShowUpdateCategoryOfSelectedBottomSheet -> {
                _state.value = if(action.show)
                    _state.value.copy(showUpdateCategoryOfSelectedBottomSheet = true)
                else {
                    _state.value.copy(showUpdateCategoryOfSelectedBottomSheet = false, multiselectItems = null)
                }
            }
            is ListAction.OnShowDateSelectorBottomSheet -> { _state.value = _state.value.copy(showDateSelectorBottomSheet = action.show) }
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


    private fun updateList() = updateList(
        _state.value.listSortedBy,
        _state.value.listSortedByAscending,
        _state.value.listFilterCriteria
    )
    @OptIn(ExperimentalTime::class)
    private fun updateList(listSortedBy: ListSortedBy, listSortedByAscending: Boolean, listFilterCriteria: ListFilterCriteria) {

        _state.value = _state.value.copy(
            listSortedBy = listSortedBy,
            listSortedByAscending = listSortedByAscending,
            listFilterCriteria = listFilterCriteria,
            isRefreshing = false
        )
    }

    private fun onDeleteSelectedItems() {
        viewModelScope.launch {
            _state.value.multiselectItems?.let { getDatabase().icalentry_dtoQueries.markAsDeleted(it) }
            syncTrigger.requestImmediatePush(_state.value.calendar.id)
            syncTrigger.triggerWidgetUpdate()
            _state.value = _state.value.copy(multiselectItems = null, showDeleteSelectedItemsDialog = false)
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
            _state.value = _state.value.copy(multiselectItems = emptyList())
            return
        } else if(icalEntryId == null && _state.value.multiselectItems != null) {
            _state.value = _state.value.copy(multiselectItems = null)
            return
        }

        val newMultiselectList = state.multiselectItems?.let { currentMultiselectItems ->
            if(state.multiselectItems?.contains(icalEntryId) == true)
                currentMultiselectItems.minus(icalEntryId!!)
            else
                currentMultiselectItems.plus(icalEntryId!!)
        } ?: listOf(icalEntryId!!)

        if(newMultiselectList.isEmpty())
            _state.value = _state.value.copy(multiselectItems = null)
        else
            _state.value = _state.value.copy(multiselectItems = newMultiselectList)
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
        _state.value = _state.value.copy(
            listCollapsedGroups = if(state.listCollapsedGroups.contains(listGroup))
                state.listCollapsedGroups.minus(listGroup)
            else
                state.listCollapsedGroups.plus(listGroup)
        )
        userAppPreferencesStore.listCollapsedGroups = state.listCollapsedGroups
    }

    private fun onGoToDate(selectedDate: IcsDateTime?) {
        selectedDate?.let { _state.value = state.copy(scrollToDate = selectedDate) }
    }
}