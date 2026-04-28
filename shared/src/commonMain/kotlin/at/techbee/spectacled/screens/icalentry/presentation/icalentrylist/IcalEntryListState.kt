package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListGrouping
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListLayout
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListSortedBy
import io.ktor.http.Url
import kotlin.time.ExperimentalTime

data class IcalEntryListState(
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
    val searchQuery: String = "",
    val searchCategory: String = "",
    val errorMessage: String? = null,

    val isSearchBarExpanded: Boolean = false,
    val listSortedBy: ListSortedBy = ListSortedBy.CREATED,
    val listSortedByAscending: Boolean = true,
    val listLayout: ListLayout = ListLayout.STAGGERED_GRID,

    val snackbarText: String? = null,
    val showDeletedItems: Boolean = false,
    val navigateUp: Boolean = false,
    val multiselectItems: List<Long>? = null,
    val showDeleteSelectedItemsDialog: Boolean = false,
    val showUpdateColorOfSelectedBottomSheet: Boolean = false,

    val listCollapsedListGroupings: Set<ListGrouping> = emptySet(),
    val listCollapsedDayGroups: Set<String> = emptySet(),
    val listTrashbinExpanded: Boolean = false,

    val spectacledVariant: SpectacledVariant = SpectacledVariant.NOTES  // must be overwritten immediately on load
) {

    val displayMap: Map<ListGrouping, List<IcalEntry>>
        get() = computeDisplayMap()

    val trashbin: List<IcalEntry>
        get() = getTrashbinList()

    @OptIn(ExperimentalTime::class)
    private fun computeDisplayMap(): Map<ListGrouping, List<IcalEntry>> {

        val baseList = icalEntries
            .filter { when(spectacledVariant) {
                SpectacledVariant.JOURNALS -> !it.syncState.isDeletedState() && it.dtStart != null
                SpectacledVariant.NOTES -> !it.syncState.isDeletedState() && it.dtStart == null
                SpectacledVariant.TASKS -> !it.syncState.isDeletedState()
            } }

        val filteredList =
            if (searchQuery.isBlank())
                baseList
            else
                baseList.filter {
                    it.summary?.contains(searchQuery, ignoreCase = true) == true
                            || it.description?.contains(searchQuery, ignoreCase = true) == true
                }

        val filteredListByCategory =
            if (searchCategory.isBlank())
                filteredList
            else
                filteredList.filter { it.categories.any { category -> category.equals(searchCategory, ignoreCase = true) } }

        val comparator = compareBy<IcalEntry> {
            when (listSortedBy) {
                ListSortedBy.CREATED -> it.created.instant.toEpochMilliseconds()
                ListSortedBy.LAST_MODIFIED -> it.lastModified?.instant?.toEpochMilliseconds()
                ListSortedBy.DATE -> it.dtStart?.instant?.toEpochMilliseconds()
                ListSortedBy.SUMMARY -> it.summary?.uppercase() ?: ""
                ListSortedBy.DRAGANDDROP -> it.orderNo?:-1
            }
        }

        val sortedFilteredList =
            if (listSortedByAscending) filteredListByCategory.sortedWith(comparator)
            else filteredListByCategory.sortedWith(comparator.reversed())

        val groupedMap: Map<ListGrouping, List<IcalEntry>> = sortedFilteredList.groupBy {
            when (listSortedBy) {
                ListSortedBy.CREATED -> ListGrouping.getGrouping(it.created.instant)
                ListSortedBy.LAST_MODIFIED -> ListGrouping.getGrouping(it.lastModified?.instant ?: it.created.instant)
                ListSortedBy.DATE -> ListGrouping.GROUP_NONE
                ListSortedBy.SUMMARY -> ListGrouping.GROUP_NONE
                ListSortedBy.DRAGANDDROP -> ListGrouping.GROUP_NONE
            }
        }

        return groupedMap
    }

    private fun getTrashbinList()= icalEntries.filter { it.syncState.isDeletedState() }
}