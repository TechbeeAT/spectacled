package at.techbee.spectacled.screens.note.presentation.notelist

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
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.AppPreferences
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.note.domain.Note
import at.techbee.spectacled.screens.note.presentation.notelist.datastructures.ListGrouping
import at.techbee.spectacled.screens.note.presentation.notelist.datastructures.ListLayout
import at.techbee.spectacled.screens.note.presentation.notelist.datastructures.ListSortedBy
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi


class NoteListViewModel(
    private val databaseDriverFactory: DatabaseDriverFactory,
    private val credentialStore: PlatformCredentialStore,
    private val platformSyncTrigger: PlatformSyncTrigger,
    private val appPreferences: AppPreferences
): ViewModel() {

    private val _state = mutableStateOf(NoteListState())
    val state by _state
    val dragAndDropList = mutableStateListOf<Note>()

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
            listSortedBy = appPreferences.listSortedBy?: ListSortedBy.CREATED,
            listSortedByAscending = appPreferences.listSortedByAscending,
            listLayout = appPreferences.listLayout?: ListLayout.STAGGERED_GRID
        )

        appPreferences.lastUsedCalendarId = calendarId

        viewModelScope.launch {
            try {

                val principal = database.principal_dtoQueries.getPrincipalForCalendar(calendarId).awaitAsOneOrNull()?.toDomain() ?: return@launch
                val credentials = credentialStore.load(principal.principalUrl)

                _state.value = _state.value.copy(
                    principal = principal,
                    credentials = credentials
                )

                launch { observeCalendar(calendarId) }
                launch { observeNotes() }

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

    private suspend fun observeNotes() {
        database
            .vjournal_dtoQueries.getJournalsByCalendar(state.calendar.id)
            .asFlow()
            .collect { journalsFlow ->
                val emittedNotes = journalsFlow.awaitAsList().map { vjournalDto -> vjournalDto.toDomain() }
                _state.value = _state.value.copy(
                    notes = emittedNotes,
                    isRefreshing = false,
                    errorMessage = null,
                    navigateUp = false,
                    snackbarText = null
                )
                updateList()
                dragAndDropList.apply {
                    clear()
                    addAll(emittedNotes.filter { emitted -> !emitted.syncState.isDeletedState() })
                    sortBy { note -> note.orderNo }
                }
            }
    }


    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun onAction(action: NoteListAction) {
        when(action) {
            is NoteListAction.OnSearchQueryChanged -> {
                updateList(state.listSortedBy, state.listSortedByAscending, action.query, state.searchCategory)
            }
            is NoteListAction.OnCategoryFilterChanged -> {
                updateList(state.listSortedBy, state.listSortedByAscending, state.searchQuery, action.category)
            }
            is NoteListAction.OnTriggerSync -> platformSyncTrigger.requestImmediate(listOf(state.calendar.id))
            is NoteListAction.OnNoteClicked -> { }   // handled in NoteListScreen
            is NoteListAction.OnSortedByChanged -> {
                updateList(action.listSortedBy, action.listSortedByAscending, _state.value.searchQuery, _state.value.searchCategory)
                appPreferences.listSortedBy = action.listSortedBy
                appPreferences.listSortedByAscending = action.listSortedByAscending
            }
            is NoteListAction.OnViewModeChanged -> {
                _state.value = _state.value.copy(listLayout = action.listLayout)
                appPreferences.listLayout = action.listLayout
            }
            is NoteListAction.OnSearchBarExpanded -> {
                _state.value = _state.value.copy(isSearchBarExpanded = action.isExpanded)
            }
            is NoteListAction.OnNavigateUp -> _state.value = _state.value.copy(navigateUp = action.navigateUp)
            is NoteListAction.OnUpdateSnackbar -> { _state.value = _state.value.copy(snackbarText = action.message) }
            NoteListAction.OnToggleShowDeletedItems -> { _state.value = _state.value.copy(showDeletedItems = !_state.value.showDeletedItems) }
            is NoteListAction.OnToggleMultiselectItem -> toggleMultiselectItem(action.noteId)
            NoteListAction.OnClearMultiselectItems -> { _state.value = _state.value.copy(multiselectItems = null) }
            is NoteListAction.OnShowDeleteSelectedItemsDialog -> { _state.value = _state.value.copy(showDeleteSelectedItemsDialog = action.showDialog)}
            NoteListAction.OnDeleteSelectedItems -> onDeleteSelectedItems()
            is NoteListAction.OnUpdateOrderNo -> onUpdateOrderNo(action.fromIndex, action.toIndex)
            NoteListAction.OnPersistOrderNo -> onPersistOrderNo()
            is NoteListAction.OnToggleListGroupingExpanded -> onToggleListGroupingExpanded(action.listGrouping)
            NoteListAction.OnToggleListTrashbinExpanded -> onToggleTrashbinExpanded()
            is NoteListAction.OnShowUpdateColorOfSelectedBottomSheet -> {
                _state.value = if(action.show)
                    _state.value.copy(showUpdateColorOfSelectedBottomSheet = true)
                else {
                    _state.value.copy(showUpdateColorOfSelectedBottomSheet = false, multiselectItems = null)
                }
            }
            is NoteListAction.OnUpdateColorOfSelected -> { onUpdateColorOfSelectedItems(action.color) }
            NoteListAction.OnSelectAllMultiselectItems -> { _state.value = _state.value.copy(multiselectItems = state.displayMap.flatMap { it.value }.map { it.id }) }
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
            _state.value.multiselectItems?.let { database.vjournal_dtoQueries.markAsDeleted(it) }
            platformSyncTrigger.requestImmediatePush(_state.value.calendar.id)
            _state.value = _state.value.copy(multiselectItems = null, showDeleteSelectedItemsDialog = false)
        }
    }

    private fun onUpdateColorOfSelectedItems(color: Color?) {
        viewModelScope.launch {
            _state.value.multiselectItems?.let { database.vjournal_dtoQueries.updateColor( if(color == Color.Unspecified) null else color?.toArgb()?.toLong(), it) }
            platformSyncTrigger.requestImmediatePush(_state.value.calendar.id)
            //_state.value = _state.value.copy(multiselectItems = null, showDeleteSelectedItemsDialog = false)
        }
    }

    private fun toggleMultiselectItem(noteId: Long?) {

        if(noteId == null && _state.value.multiselectItems == null) {
            _state.value = _state.value.copy(multiselectItems = emptyList())
            return
        } else if(noteId == null && _state.value.multiselectItems != null) {
            _state.value = _state.value.copy(multiselectItems = null)
            return
        }

        val newMultiselectList = state.multiselectItems?.let { currentMultiselectItems ->
            if(state.multiselectItems?.contains(noteId) == true)
                currentMultiselectItems.minus(noteId!!)
            else
                currentMultiselectItems.plus(noteId!!)
        } ?: listOf(noteId!!)

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
            database.vjournal_dtoQueries.transaction {
                dragAndDropList.forEachIndexed { index, note ->
                    database.vjournal_dtoQueries.updateOrderNo(index.toLong(), note.id)
                }
            }
        }
    }

    private fun onToggleListGroupingExpanded(listGrouping: ListGrouping) {
        _state.value = _state.value.copy(
            listExpandedSections = if(state.listExpandedSections.contains(listGrouping))
                state.listExpandedSections.minus(listGrouping)
            else
                state.listExpandedSections.plus(listGrouping)
        )
        appPreferences.listExpandedSections = state.listExpandedSections
    }

    private fun onToggleTrashbinExpanded() {
        _state.value = _state.value.copy(listTrashbinExpanded = !_state.value.listTrashbinExpanded)
        appPreferences.listTrashbinExpanded = state.listTrashbinExpanded
    }
}