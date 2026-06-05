package at.techbee.spectacled.screens.list.presentation

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.PlatformInstantFormatter
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.list.presentation.datastructures.ListGrouping
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import io.ktor.http.Url

data class ListState(
    val icalEntries: List<IcalEntry> = emptyList(),
    val calendar: Calendar = Calendar(
        id = 0L,
        homeCollectionId = 0L,
        displayName = null,
        calendarDescription = null,
        url = Url(""),
        color = Color.Unspecified,
        ctag = null,
        supportedComponents = emptyList(),
        calDavPrivileges = emptyList(),
        calendarSyncStatus = null,
        syncToken = null,
        syncComponent = null
    ),
    val principal: Principal = Principal(
        id = 0L,
        principalUrl = Url(""),
        displayName = null,
        calendarUserAddressSet = emptyList()
    ),
    val credentials: Credentials? = null,
    val isRefreshing: Boolean = false,
    val searchQuery: String? = null,
    val searchCategory: String? = null,
    val errorMessage: String? = null,

    val allColors: List<Color> = emptyList(),
    val allCategories: List<String> = emptyList(),

    val listSortedBy: ListSortedBy = ListSortedBy.CREATED,
    val listSortedByAscending: Boolean = true,
    val listLayout: ListLayout = ListLayout.STAGGERED_GRID,

    val snackbarText: String? = null,
    val showDeletedItems: Boolean = false,
    val navigateUp: Boolean = false,
    val navigateToIcalEntryId: Long? = null,
    val multiselectItems: List<Long>? = null,

    val showDeleteSelectedItemsDialog: Boolean = false,
    val showUpdateColorOfSelectedBottomSheet: Boolean = false,
    val showUpdateCategoryOfSelectedBottomSheet: Boolean = false,
    val showDateSelectorBottomSheet: Boolean = false,

    val draggingIcalEntryId: Long? = null,
    val scrollToDate: IcsDateTime? = null,

    val listCollapsedGroups: Set<String> = emptySet(),
    val spectacledVariant: SpectacledVariant = SpectacledVariant.NOTES  // must be overwritten immediately on load
) {

    val isSearchBarExpanded: Boolean
        get() = searchQuery != null || searchCategory != null


    val displayMap: Map<ListGrouping, List<IcalEntry>>
        get() = getBaseList(icalEntries)
            .let { getFilteredList(it) }
            .let { getPinnedFilteredList(it) }
            .let { getSortedList(it) }
            .let { getGroupedMap(it) }

    val displayMapByDtStartDay: Map<String, List<IcalEntry>>
        get() = getBaseList(icalEntries)
            .let { getFilteredList(it) }
            .let { getPinnedFilteredList(it) }
            .let { getSortedList(it) }
            .groupBy { PlatformInstantFormatter(it.dtStart ?: IcsDateTime.now()).formatLocalizedDate() }

    val displayMapByDtStartMonth: Map<String, List<IcalEntry>>
        get() = getBaseList(icalEntries)
            .let { getFilteredList(it) }
            .let { getPinnedFilteredList(it) }
            .let { getSortedList(it) }
            .groupBy { "${it.dtStart?.toLocalDateTime()?.year}-${it.dtStart?.toLocalDateTime()?.month}" }


    val trashbin: List<IcalEntry>
        get() = getBaseList(icalEntries, true)
            .let { getFilteredList(it) }
            .let { getSortedList(it) }

    val pinned: List<IcalEntry>
        get() = getBaseList(icalEntries)
            .let { getFilteredList(it) }
            .let { getPinnedFilteredList(it, true) }
            .let { getSortedList(it) }


    private fun getBaseList(icalEntries: List<IcalEntry>, trashbin: Boolean = false) =
        icalEntries
            .filter { when(spectacledVariant) {
                SpectacledVariant.JOURNALS -> it.syncState.isDeletedState() == trashbin && it.dtStart != null
                SpectacledVariant.NOTES -> it.syncState.isDeletedState() == trashbin && it.dtStart == null
                SpectacledVariant.TASKS -> it.syncState.isDeletedState() == trashbin
        } }

    private fun getPinnedFilteredList(icalEntries: List<IcalEntry>, pinned: Boolean = false) =
        icalEntries.filter {
            if(pinned)
                it.categories.any { category -> category == IcalEntry.PINNED_CATEGORY}
            else
                it.categories.none { category -> category == IcalEntry.PINNED_CATEGORY }
        }


    private fun getFilteredList(icalEntries: List<IcalEntry>): List<IcalEntry> {
        val filteredList =
            if (searchQuery.isNullOrBlank())
                icalEntries
            else
                icalEntries.filter {
                    it.summary?.contains(searchQuery, ignoreCase = true) == true
                            || it.description?.contains(searchQuery, ignoreCase = true) == true
                }

        val filteredListByCategory =
            if (searchCategory.isNullOrBlank())
                filteredList
            else
                filteredList.filter { it.categories.any { category -> category.equals(searchCategory, ignoreCase = true) } }

        return filteredListByCategory
    }


    private fun getSortedList(icalEntries: List<IcalEntry>): List<IcalEntry> {
        val ascending = listSortedByAscending
        return when (listSortedBy) {
            ListSortedBy.CREATED -> {
                if (ascending) icalEntries.sortedBy { it.created.instant }
                else icalEntries.sortedByDescending { it.created.instant }
            }
            ListSortedBy.LAST_MODIFIED -> {
                if (ascending) icalEntries.sortedBy { it.lastModified?.instant ?: it.created.instant }
                else icalEntries.sortedByDescending { it.lastModified?.instant ?: it.created.instant }
            }
            ListSortedBy.DATE, ListSortedBy.START -> {
                val comparator = compareBy<IcalEntry> { it.dtStart?.toLocalDateTime() }
                val order = if (ascending) comparator.reversed() else comparator
                icalEntries.sortedWith(compareBy<IcalEntry> { it.dtStart == null }.then(order))
            }
            ListSortedBy.DUE -> {
                val comparator = compareBy<IcalEntry> { it.due?.toLocalDateTime() }
                val order = if (ascending) comparator.reversed() else comparator
                icalEntries.sortedWith(compareBy<IcalEntry> { it.due == null }.then(order))
            }
            ListSortedBy.SUMMARY -> {
                val selector: (IcalEntry) -> String = { it.summary?.uppercase() ?: it.description?.uppercase() ?: "" }
                if (ascending) icalEntries.sortedBy(selector)
                else icalEntries.sortedByDescending(selector)
            }
            ListSortedBy.DRAGANDDROP -> { icalEntries.sortedBy { it.orderNo ?: Long.MAX_VALUE } }
        }
    }


    private fun getGroupedMap(icalEntries: List<IcalEntry>) =
        icalEntries.groupBy {
            when (listSortedBy) {
                ListSortedBy.CREATED -> ListGrouping.getGrouping(ListGrouping.createdGroups, it.created)
                ListSortedBy.LAST_MODIFIED -> ListGrouping.getGrouping(ListGrouping.lastModifiedGroups, it.lastModified ?: it.created)
                ListSortedBy.DATE -> ListGrouping.GROUP_NO_DATE
                ListSortedBy.SUMMARY -> ListGrouping.GROUP_NO_DATE
                ListSortedBy.DRAGANDDROP -> ListGrouping.GROUP_NO_DATE
                ListSortedBy.START -> ListGrouping.getGrouping(ListGrouping.startGroups, it.dtStart)
                ListSortedBy.DUE -> ListGrouping.getGrouping(ListGrouping.dueGroups, it.due)
            }
        }
}