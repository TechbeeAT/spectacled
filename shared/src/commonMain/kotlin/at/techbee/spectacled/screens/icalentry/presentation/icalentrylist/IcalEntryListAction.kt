package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListLayout
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListSortedBy

sealed interface IcalEntryListAction {
    data class OnSearchQueryChanged(val query: String): IcalEntryListAction
    data class OnCategoryFilterChanged(val category: String): IcalEntryListAction
    data class OnIcalEntryClicked(val id: Long): IcalEntryListAction
    data class OnSortedByChanged(val listSortedBy: ListSortedBy, val listSortedByAscending: Boolean): IcalEntryListAction
    data class OnViewModeChanged(val listLayout: ListLayout): IcalEntryListAction
    object OnTriggerSync: IcalEntryListAction
    data class OnSearchBarExpanded(val isExpanded: Boolean): IcalEntryListAction
    data class OnNavigateUp(val navigateUp: Boolean): IcalEntryListAction
    data class OnUpdateSnackbar(val message: String?): IcalEntryListAction
    object OnToggleShowDeletedItems: IcalEntryListAction
    data class OnToggleMultiselectItem(val icalEntryId: Long?): IcalEntryListAction
    object OnClearMultiselectItems: IcalEntryListAction
    object OnSelectAllMultiselectItems: IcalEntryListAction
    data class OnShowDeleteSelectedItemsDialog(val showDialog: Boolean): IcalEntryListAction
    object OnDeleteSelectedItems: IcalEntryListAction
    data class OnShowUpdateColorOfSelectedBottomSheet(val show: Boolean): IcalEntryListAction
    data class OnUpdateColorOfSelected(val color: Color?): IcalEntryListAction
    data class OnUpdateOrderNo(val fromIndex: Int, val toIndex: Int): IcalEntryListAction
    object OnPersistOrderNo: IcalEntryListAction
    data class OnToggleListGroupExpanded(val listGroup: String): IcalEntryListAction
}