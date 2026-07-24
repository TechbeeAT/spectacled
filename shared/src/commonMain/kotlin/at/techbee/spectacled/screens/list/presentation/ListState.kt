package at.techbee.spectacled.screens.list.presentation

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import at.techbee.spectacled.screens.list.presentation.datastructures.ListGrouping
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import io.ktor.http.Url
import kotlinx.datetime.number

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
        syncToken = null
    ),
    val principal: Principal = Principal(
        id = 0L,
        principalUrl = Url(""),
        displayName = null,
        calendarUserAddressSet = emptyList()
    ),
    val credentials: Credentials? = null,
    val isRefreshing: Boolean = false,
    val isInitialized: Boolean = false,
    val errorMessage: String? = null,

    val allColors: List<Color> = emptyList(),
    val allCategories: List<String> = emptyList(),

    // Loaded once for the move-to-calendar target selector.
    val allPrincipals: List<Principal> = emptyList(),
    val allHomeCollections: List<HomeCollection> = emptyList(),
    val allCalendars: List<Calendar> = emptyList(),

    val listFilterCriteria: ListFilterCriteria = ListFilterCriteria(),
    val listSortedBy: ListSortedBy = ListSortedBy.CREATED,
    val listSortedByAscending: Boolean = true,
    val listLayout: ListLayout = ListLayout.STAGGERED_GRID,

    val snackbarText: String? = null,
    val showDeletedItems: Boolean = false,
    val navigateUp: Boolean = false,
    val navigateToIcalEntryId: Long? = null,
    val multiselectItems: List<Long>? = null,

    val showDeleteSelectedItemsDialog: Boolean = false,
    val showMoveSelectedItemsDialog: Boolean = false,
    val showUpdateColorOfSelectedBottomSheet: Boolean = false,
    val showUpdateCategoryOfSelectedBottomSheet: Boolean = false,
    val showDateSelectorBottomSheet: Boolean = false,

    val draggingIcalEntryId: Long? = null,
    val scrollToDate: IcsDateTime? = null,

    val listCollapsedGroups: Set<String> = emptySet(),
    val spectacledVariant: SpectacledVariant = SpectacledVariant.NOTES,  // must be overwritten immediately on load

    val displayMap: Map<ListGrouping, List<IcalEntry>> = emptyMap(),
    val displayMapByDtStartDay: Map<String, List<IcalEntry>> = emptyMap(),
    val displayMapByDtStartMonth: Map<String, List<IcalEntry>> = emptyMap(),
    val trashbin: List<IcalEntry> = emptyList(),
    val pinned: List<IcalEntry> = emptyList(),
    val subtasks: Map<String, List<IcalEntry>> = emptyMap()
) {

    val isSearchBarExpanded: Boolean
        get() = listFilterCriteria.anyFilterActive()

    fun recompute(): ListState {
        val baseList = getBaseList(icalEntries)
        val filteredList = getFilteredList(baseList)
        val sortedList = getSortedList(filteredList)

        // Optimization: Partition the list once into pinned and unpinned
        val (pinnedList, unpinnedList) = sortedList.partition {
            it.categories.contains(IcalEntry.PINNED_CATEGORY)
        }

        return this.copy(
            displayMap = getGroupedMap(unpinnedList),
            displayMapByDtStartDay = unpinnedList
                .groupBy { (it.dtStart ?: IcsDateTime.now()).formatLocalized(IcsDateTimeFormat.DATE) },
            displayMapByDtStartMonth = unpinnedList
                .groupBy {
                    val dateTime = it.dtStart?.toLocalDateTime()
                    if (dateTime != null) "${dateTime.year}-${dateTime.month.number}" else ""
                },
            trashbin = icalEntries
                .filter { it.syncState.isDeletedState() }
                .let { getSortedList(it) },
            pinned = pinnedList,
            subtasks = getSubtasksLogic(icalEntries)
        )
    }


    private fun getBaseList(icalEntries: List<IcalEntry>, trashbin: Boolean = false) =
        icalEntries
            .filter { when(spectacledVariant) {
                SpectacledVariant.JOURNALS, SpectacledVariant.NOTES -> (it.isJournal() || it.isNote()) && it.syncState.isDeletedState() == trashbin
                SpectacledVariant.TASKS -> it.isTask() && it.syncState.isDeletedState() == trashbin && it.parentUid == null
        } }

    private fun getSubtasksLogic(icalEntries: List<IcalEntry>) =
        icalEntries
                    .filter { !it.syncState.isDeletedState() && it.parentUid != null }
                    .sortedBy { it.orderNo ?: it.created.instant.toEpochMilliseconds() }
                    .groupBy { it.parentUid!! }

    private fun getPinnedFilteredList(icalEntries: List<IcalEntry>, pinned: Boolean = false) =
        icalEntries.filter { it.isPinned() == pinned }


    private fun getFilteredList(icalEntries: List<IcalEntry>): List<IcalEntry> {
        val criteria = listFilterCriteria

        // Early exit if no filter is active
        if (!criteria.anyFilterActive()) return icalEntries

        val query = criteria.searchQuery
        val category = criteria.searchCategory
        val status = criteria.filterStatus

        return icalEntries.filter { item ->
            // Single pass check for all conditions
            val matchesQuery = query.isNullOrBlank() ||
                    item.summary?.contains(query, ignoreCase = true) == true ||
                    item.description?.contains(query, ignoreCase = true) == true

            if (!matchesQuery) return@filter false

            val matchesCategory = category.isNullOrBlank() ||
                    item.categories.any { it.equals(category, ignoreCase = true) }

            if (!matchesCategory) return@filter false

            val matchesStatus = status == null || item.status == status

            matchesStatus
        }
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
                val order = if (ascending) comparator else comparator.reversed()
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