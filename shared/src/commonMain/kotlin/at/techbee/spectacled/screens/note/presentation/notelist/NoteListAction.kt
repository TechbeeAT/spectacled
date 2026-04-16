package at.techbee.spectacled.screens.note.presentation.notelist

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.note.presentation.notelist.datastructures.ListGrouping
import at.techbee.spectacled.screens.note.presentation.notelist.datastructures.ListLayout
import at.techbee.spectacled.screens.note.presentation.notelist.datastructures.ListSortedBy

sealed interface NoteListAction {
    data class OnSearchQueryChanged(val query: String): NoteListAction
    data class OnCategoryFilterChanged(val category: String): NoteListAction
    data class OnNoteClicked(val id: Long): NoteListAction
    data class OnSortedByChanged(val listSortedBy: ListSortedBy, val listSortedByAscending: Boolean): NoteListAction
    data class OnViewModeChanged(val listLayout: ListLayout): NoteListAction
    object OnTriggerSync: NoteListAction
    data class OnSearchBarExpanded(val isExpanded: Boolean): NoteListAction
    data class OnNavigateUp(val navigateUp: Boolean): NoteListAction
    data class OnUpdateSnackbar(val message: String?): NoteListAction
    object OnToggleShowDeletedItems: NoteListAction
    data class OnToggleMultiselectItem(val noteId: Long?): NoteListAction
    object OnClearMultiselectItems: NoteListAction
    object OnSelectAllMultiselectItems: NoteListAction
    data class OnShowDeleteSelectedItemsDialog(val showDialog: Boolean): NoteListAction
    object OnDeleteSelectedItems: NoteListAction
    data class OnShowUpdateColorOfSelectedBottomSheet(val show: Boolean): NoteListAction
    data class OnUpdateColorOfSelected(val color: Color?): NoteListAction
    data class OnUpdateOrderNo(val fromIndex: Int, val toIndex: Int): NoteListAction
    object OnPersistOrderNo: NoteListAction
    data class OnToggleListGroupingExpanded(val listGrouping: ListGrouping): NoteListAction
    object OnToggleListTrashbinExpanded: NoteListAction
}