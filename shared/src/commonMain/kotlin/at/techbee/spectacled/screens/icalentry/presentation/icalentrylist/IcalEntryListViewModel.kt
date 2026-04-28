package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.AppPreferences
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListGrouping
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListLayout
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListSortedBy
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi


class IcalEntryListViewModel(
    private val databaseDriverFactory: DatabaseDriverFactory,
    private val credentialStore: PlatformCredentialStore,
    private val platformSyncTrigger: PlatformSyncTrigger,
    private val appPreferences: AppPreferences,
    private val spectacledVariant: SpectacledVariant
): ViewModel() {

    private val _state = mutableStateOf(IcalEntryListState())
    val state by _state
    val dragAndDropList = mutableStateListOf<IcalEntry>()

    private lateinit var database: SpectacledDatabase
    private suspend fun getDatabase() = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)

    init {
        viewModelScope.launch { database = getDatabase() }
    }

    fun load(calendarId: Long) {
        _state.value = _state.value.copy(
            isRefreshing = true,
            errorMessage = null,
            navigateUp = false,
            snackbarText = null,
            listSortedBy = when {
                appPreferences.listSortedBy != null -> appPreferences.listSortedBy!!
                spectacledVariant == SpectacledVariant.JOURNALS -> ListSortedBy.DATE
                else -> ListSortedBy.CREATED
            },
            listSortedByAscending = appPreferences.listSortedByAscending,
            listLayout = when {
                appPreferences.listLayout != null -> appPreferences.listLayout!!
                spectacledVariant == SpectacledVariant.JOURNALS -> ListLayout.LIST
                else -> ListLayout.STAGGERED_GRID
            },
            listCollapsedListGroupings = appPreferences.listCollapsedSections,
            listCollapsedDayGroups = appPreferences.listCollapsedDays,
            listTrashbinExpanded = appPreferences.listTrashbinExpanded
        )

        appPreferences.lastUsedCalendarId = calendarId

        viewModelScope.launch {
            try {

                val principal = database.principal_dtoQueries.getPrincipalForCalendar(calendarId).awaitAsOneOrNull()?.toDomain() ?: throw NullPointerException("Principal not found")
                val credentials = credentialStore.load(principal.principalUrl) ?: throw NullPointerException("Credentials not found")

                _state.value = _state.value.copy(
                    principal = principal,
                    credentials = credentials
                )

                launch { observeCalendar(calendarId) }
                launch { observeIcalentries() }

            } catch (_: NullPointerException) {
                _state.value = _state.value.copy(navigateUp = true, isRefreshing = false)
            }
        }
    }

    private suspend fun observeCalendar(calendarId: Long) {
        database
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
        database
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


    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun onAction(action: IcalEntryListAction) {
        when(action) {
            is IcalEntryListAction.OnSearchQueryChanged -> {
                updateList(state.listSortedBy, state.listSortedByAscending, action.query, state.searchCategory)
            }
            is IcalEntryListAction.OnCategoryFilterChanged -> {
                updateList(state.listSortedBy, state.listSortedByAscending, state.searchQuery, action.category)
            }
            is IcalEntryListAction.OnTriggerSync -> platformSyncTrigger.requestImmediate(listOf(state.calendar.id))
            is IcalEntryListAction.OnIcalEntryClicked -> { }   // handled in IcalEntryListScreen
            is IcalEntryListAction.OnSortedByChanged -> {
                updateList(action.listSortedBy, action.listSortedByAscending, _state.value.searchQuery, _state.value.searchCategory)
                appPreferences.listSortedBy = action.listSortedBy
                appPreferences.listSortedByAscending = action.listSortedByAscending
            }
            is IcalEntryListAction.OnViewModeChanged -> {
                _state.value = _state.value.copy(listLayout = action.listLayout)
                appPreferences.listLayout = action.listLayout
            }
            is IcalEntryListAction.OnSearchBarExpanded -> {
                _state.value = _state.value.copy(isSearchBarExpanded = action.isExpanded)
            }
            is IcalEntryListAction.OnNavigateUp -> {
                if(action.navigateUp)
                    appPreferences.lastUsedCalendarId = null // reset lastUsedCalendarId
                _state.value = _state.value.copy(navigateUp = action.navigateUp)
            }
            is IcalEntryListAction.OnUpdateSnackbar -> { _state.value = _state.value.copy(snackbarText = action.message) }
            IcalEntryListAction.OnToggleShowDeletedItems -> { _state.value = _state.value.copy(showDeletedItems = !_state.value.showDeletedItems) }
            is IcalEntryListAction.OnToggleMultiselectItem -> toggleMultiselectItem(action.icalEntryId)
            IcalEntryListAction.OnClearMultiselectItems -> { _state.value = _state.value.copy(multiselectItems = null) }
            is IcalEntryListAction.OnShowDeleteSelectedItemsDialog -> { _state.value = _state.value.copy(showDeleteSelectedItemsDialog = action.showDialog)}
            IcalEntryListAction.OnDeleteSelectedItems -> onDeleteSelectedItems()
            is IcalEntryListAction.OnUpdateOrderNo -> onUpdateOrderNo(action.fromIndex, action.toIndex)
            IcalEntryListAction.OnPersistOrderNo -> onPersistOrderNo()
            is IcalEntryListAction.OnToggleListGroupingExpanded -> onToggleListGroupingExpanded(action.listGrouping)
            is IcalEntryListAction.OnToggleDayGroupingExpanded -> onToggleDayGroupingExpanded(action.listDayGroup)
            IcalEntryListAction.OnToggleListTrashbinExpanded -> onToggleTrashbinExpanded()
            is IcalEntryListAction.OnShowUpdateColorOfSelectedBottomSheet -> {
                _state.value = if(action.show)
                    _state.value.copy(showUpdateColorOfSelectedBottomSheet = true)
                else {
                    _state.value.copy(showUpdateColorOfSelectedBottomSheet = false, multiselectItems = null)
                }
            }
            is IcalEntryListAction.OnUpdateColorOfSelected -> { onUpdateColorOfSelectedItems(action.color) }
            IcalEntryListAction.OnSelectAllMultiselectItems -> { _state.value = _state.value.copy(multiselectItems = state.displayMap.flatMap { it.value }.map { it.id }) }
        }
    }


    private fun updateList() = updateList(
        _state.value.listSortedBy,
        _state.value.listSortedByAscending,
        _state.value.searchQuery,
        _state.value.searchCategory
    )
    @OptIn(ExperimentalTime::class)
    private fun updateList(listSortedBy: ListSortedBy, listSortedByAscending: Boolean, searchQuery: String, searchCategory: String) {

        _state.value = _state.value.copy(
            listSortedBy = listSortedBy,
            listSortedByAscending = listSortedByAscending,
            searchQuery = searchQuery,
            searchCategory = searchCategory,
            isRefreshing = false
        )
    }

    private fun onDeleteSelectedItems() {
        viewModelScope.launch {
            _state.value.multiselectItems?.let { database.icalentry_dtoQueries.markAsDeleted(it) }
            platformSyncTrigger.requestImmediatePush(_state.value.calendar.id)
            _state.value = _state.value.copy(multiselectItems = null, showDeleteSelectedItemsDialog = false)
        }
    }

    private fun onUpdateColorOfSelectedItems(color: Color?) {
        viewModelScope.launch {
            _state.value.multiselectItems?.let { database.icalentry_dtoQueries.updateColor( if(color == Color.Unspecified) null else color?.toArgb()?.toLong(), it) }
            platformSyncTrigger.requestImmediatePush(_state.value.calendar.id)
            //_state.value = _state.value.copy(multiselectItems = null, showDeleteSelectedItemsDialog = false)
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
            database.icalentry_dtoQueries.transaction {
                dragAndDropList.forEachIndexed { index, icalEntry ->
                    database.icalentry_dtoQueries.updateOrderNo(index.toLong(), icalEntry.id)
                }
            }
        }
    }

    private fun onToggleListGroupingExpanded(listGrouping: ListGrouping) {
        _state.value = _state.value.copy(
            listCollapsedListGroupings = if(state.listCollapsedListGroupings.contains(listGrouping))
                state.listCollapsedListGroupings.minus(listGrouping)
            else
                state.listCollapsedListGroupings.plus(listGrouping)
        )
        appPreferences.listCollapsedSections = state.listCollapsedListGroupings
    }

    private fun onToggleDayGroupingExpanded(listDayGrouping: String) {
        _state.value = _state.value.copy(
            listCollapsedDayGroups = if(state.listCollapsedDayGroups.contains(listDayGrouping))
                state.listCollapsedDayGroups.minus(listDayGrouping)
            else
                state.listCollapsedDayGroups.plus(listDayGrouping)
        )
        appPreferences.listCollapsedDays = state.listCollapsedDayGroups
    }

    private fun onToggleTrashbinExpanded() {
        _state.value = _state.value.copy(listTrashbinExpanded = !_state.value.listTrashbinExpanded)
        appPreferences.listTrashbinExpanded = state.listTrashbinExpanded
    }
}