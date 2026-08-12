package at.techbee.spectacled.screens.list.presentation

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy

sealed interface ListAction {
    data class OnListFilterCriteriaChanged(val listFilterCriteria: ListFilterCriteria): ListAction
    data class OnIcalEntryClicked(val id: Long?): ListAction
    data class OnSortedByChanged(val listSortedBy: ListSortedBy, val listSortedByAscending: Boolean): ListAction
    data class OnViewModeChanged(val listLayout: ListLayout): ListAction
    object OnTriggerSync: ListAction
    data class OnSearchBarExpanded(val isExpanded: Boolean): ListAction
    data class OnNavigateUp(val navigateUp: Boolean): ListAction
    data class OnToggleProgress(val icalEntryId: Long): ListAction
    data class OnUpdateSnackbar(val message: String?): ListAction
    object OnToggleShowDeletedItems: ListAction
    data class OnToggleMultiselectItem(val icalEntryId: Long?): ListAction
    object OnClearMultiselectItems: ListAction
    object OnSelectAllMultiselectItems: ListAction
    data class OnShowDeleteSelectedItemsDialog(val showDialog: Boolean): ListAction
    object OnDeleteSelectedItems: ListAction
    data class OnShowMoveSelectedItemsDialog(val showDialog: Boolean): ListAction
    data class OnMoveSelectedItems(val targetCalendarId: Long): ListAction

    data class OnShowDateSelectorBottomSheet(val show: Boolean): ListAction
    data class OnShowUpdateColorOfSelectedBottomSheet(val show: Boolean): ListAction
    data class OnShowUpdateCategoryOfSelectedBottomSheet(val show: Boolean): ListAction

    data class OnShowDeriveEntriesBottomSheet(val show: Boolean): ListAction
    data class OnDeriveEntriesFromText(val text: String, val createSubtasks: Boolean): ListAction

    data class OnUpdateColorOfSelected(val color: Color?): ListAction
    data class OnUpdateCategoryOfSelected(val addCategory: String?, val removeCategory: String?): ListAction
    data class OnUpdateOrderNo(val fromIndex: Int, val toIndex: Int): ListAction
    object OnPersistOrderNo: ListAction

    data class OnGoToSelectedDate(val selectedDate: IcsDateTime?): ListAction
    data class OnToggleListGroupExpanded(val listGroup: String): ListAction
    data class OnTogglePinEntry(val pin: Boolean): ListAction
    data class OnDraggingIcalEntry(val icalEntryId: Long?): ListAction
}