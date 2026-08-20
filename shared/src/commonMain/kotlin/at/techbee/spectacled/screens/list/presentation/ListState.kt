package at.techbee.spectacled.screens.list.presentation

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_NO_CRITERIA
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_PINNED
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_TRASHBIN
import at.techbee.spectacled.screens.core.data.ai.AiDeriveEntriesResult
import at.techbee.spectacled.screens.core.data.ai.AiProvider
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import at.techbee.spectacled.screens.list.presentation.datastructures.ListGrouping
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSection
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSectionHeader
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import io.ktor.http.Url
import kotlinx.datetime.number
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.grouping_no_date
import spectacled.shared.generated.resources.grouping_other
import spectacled.shared.generated.resources.ic_pin_rotated
import spectacled.shared.generated.resources.ic_trashbin
import spectacled.shared.generated.resources.pinned
import spectacled.shared.generated.resources.trashbin_x

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

    val aiProvider: AiProvider = AiProvider.NONE,
    val claudeApiKeyPresent: Boolean = false,
    val openAiBaseUrlPresent: Boolean = false,
    val showDeriveEntriesBottomSheet: Boolean = false,
    val aiDerivedEntriesResult: AiDeriveEntriesResult? = null,

    val draggingIcalEntryId: Long? = null,
    val scrollToDate: IcsDateTime? = null,

    val listCollapsedGroups: Set<String> = emptySet(),
    val spectacledVariant: SpectacledVariant = SpectacledVariant.NOTES,  // must be overwritten immediately on load

    // Single ordered structure every list screen renders from:
    // pinned -> grouped-by-selection -> no-criteria -> trashbin. Built in [recompute].
    val sections: List<ListSection> = emptyList(),
    val subtasks: Map<String, List<IcalEntry>> = emptyMap()
) {

    val isSearchBarExpanded: Boolean
        get() = listFilterCriteria.anyFilterActive()

    /**
     * All entries currently shown in the grouped/no-criteria body (everything except pinned and the
     * trashbin). Backs the drag-and-drop feed and "select all".
     */
    val displayedEntries: List<IcalEntry>
        get() = sections
            .filter { it.kind == ListSection.Kind.GROUPED || it.kind == ListSection.Kind.NO_CRITERIA }
            .flatMap { it.entries }

    /** True when there is nothing to show outside the trashbin (drives the empty-state overlay). */
    val isDisplayEmpty: Boolean
        get() = sections.none { it.kind != ListSection.Kind.TRASHBIN && it.entries.isNotEmpty() }

    val isAiProviderConfigured: Boolean
        get() = (aiProvider == AiProvider.CLAUDE && claudeApiKeyPresent)
                || (aiProvider == AiProvider.OPENAI_COMPATIBLE && openAiBaseUrlPresent)

    fun recompute(): ListState {
        val baseList = getBaseList(icalEntries)
        val filteredList = getFilteredList(baseList)
        val sortedList = getSortedList(filteredList)

        return this.copy(
            sections = buildSections(sortedList),
            subtasks = getSubtasksLogic(icalEntries)
        )
    }


    private fun getBaseList(icalEntries: List<IcalEntry>) =
        icalEntries
            .filter { when(spectacledVariant) {
                SpectacledVariant.JOURNALS, SpectacledVariant.NOTES -> (it.isJournal() || it.isNote())
                SpectacledVariant.TASKS -> it.isTask() && it.parentUid == null
        } }

    private fun getSubtasksLogic(icalEntries: List<IcalEntry>) =
        icalEntries
                    .filter { !it.syncState.isDeletedState() && it.parentUid != null }
                    .sortedBy { it.orderNo ?: it.created.instant.toEpochMilliseconds() }
                    .groupBy { it.parentUid!! }

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


    /**
     * Assembles the ordered section list every list screen renders from:
     * pinned -> grouped-by-selection -> no-criteria -> trashbin.
     * @param entries  the already filtered + sorted
     */
    private fun buildSections(entries: List<IcalEntry>): List<ListSection> {
        val (trashbin, notTrashbin) = entries.partition { it.syncState.isDeletedState() }
        val (pinned, rest) = notTrashbin.partition { it.isPinned() }
        val (noCriteria, grouped) = rest.partition { it.isNoCriteria() }

        return buildList {
            if (pinned.isNotEmpty()) add(
                ListSection(
                    key = LIST_COLLAPSED_GROUP_PINNED,
                    header = ListSectionHeader.Res(Res.string.pinned, iconDrawableRes = Res.drawable.ic_pin_rotated),
                    entries = pinned,
                    kind = ListSection.Kind.PINNED
                )
            )

            addAll(groupedSections(grouped))

            if (noCriteria.isNotEmpty()) add(
                ListSection(
                    key = LIST_COLLAPSED_GROUP_NO_CRITERIA,
                    header = ListSectionHeader.Res(noCriteriaHeaderRes()),
                    entries = noCriteria,
                    kind = ListSection.Kind.NO_CRITERIA,
                    dimmed = true
                )
            )

            if (trashbin.isNotEmpty()) add(
                ListSection(
                    key = LIST_COLLAPSED_GROUP_TRASHBIN,
                    header = ListSectionHeader.Res(Res.string.trashbin_x, param = trashbin.size, iconDrawableRes = Res.drawable.ic_trashbin),
                    entries = trashbin,
                    kind = ListSection.Kind.TRASHBIN,
                    dimmed = true
                )
            )
        }
    }

    /** Builds the grouped ("grouped by the given selection") sections for [entries]. */
    private fun groupedSections(entries: List<IcalEntry>): List<ListSection> {
        if (entries.isEmpty()) return emptyList()

        // Journals always groups chronologically by month with sticky headers, regardless of sort.
        if (spectacledVariant == SpectacledVariant.JOURNALS)
            return monthSections(entries)

        return when (listSortedBy) {
            ListSortedBy.CREATED -> datetimeBucketSections(entries, ListGrouping.createdGroups) { it.created }
            ListSortedBy.LAST_MODIFIED -> datetimeBucketSections(entries, ListGrouping.lastModifiedGroups) { it.lastModified ?: it.created }
            ListSortedBy.START -> datetimeBucketSections(entries, ListGrouping.startGroups) { it.dtStart }
            ListSortedBy.DUE -> datetimeBucketSections(entries, ListGrouping.dueGroups) { it.due }
            ListSortedBy.DATE -> daySections(entries)
            // No grouping criterion: a single header-less block.
            ListSortedBy.SUMMARY, ListSortedBy.DRAGANDDROP -> listOf(
                ListSection(key = "grouped_all", header = null, entries = entries, kind = ListSection.Kind.GROUPED)
            )
        }
    }

    /** Groups [entries] into [ListGrouping] date buckets, preserving the sorted encounter order. */
    private fun datetimeBucketSections(
        entries: List<IcalEntry>,
        groups: Set<ListGrouping>,
        dateSelector: (IcalEntry) -> IcsDateTime?
    ): List<ListSection> =
        entries
            .groupBy { ListGrouping.getGrouping(groups, dateSelector(it)) }
            .mapNotNull { (grouping, groupEntries) ->
                // null grouping is routed to the no-criteria section, so it never reaches here.
                if (grouping == null)
                    return@mapNotNull null

                ListSection(
                    key = grouping.name,
                    header = grouping.stringRes?.let { ListSectionHeader.Res(it, grouping.stringResParam) },
                    entries = groupEntries,
                    kind = ListSection.Kind.GROUPED
                )
            }

    /** Groups [entries] by calendar day (dtStart is guaranteed non-null: undated entries are no-criteria). */
    private fun daySections(entries: List<IcalEntry>): List<ListSection> =
        entries
            .groupBy { it.dtStart!!.formatLocalized(IcsDateTimeFormat.DATE) }
            .map { (day, dayEntries) ->
                ListSection(key = day, header = ListSectionHeader.Raw(day), entries = dayEntries, kind = ListSection.Kind.GROUPED)
            }

    /** Groups [entries] by month for the journals list (dtStart guaranteed non-null: undated notes are no-criteria). */
    private fun monthSections(entries: List<IcalEntry>): List<ListSection> =
        entries
            .groupBy {
                val dt = it.dtStart!!.toLocalDateTime()
                "${dt.year}-${dt.month.number}"
            }
            .map { (monthKey, monthEntries) ->
                ListSection(
                    key = monthKey,
                    header = ListSectionHeader.Month(monthEntries.firstOrNull()?.dtStart),
                    entries = monthEntries,
                    kind = ListSection.Kind.GROUPED
                )
            }

    /**
     * Whether [this] belongs in the "no criteria" section: it is the wrong entry type for this app
     * (a dated journal in the notes list, an undated note in the journals list), or it lacks the
     * value the list is currently grouped by (e.g. no start/due date).
     */
    private fun IcalEntry.isNoCriteria(): Boolean {
        if (isWrongTypeForVariant()) return true
        if (spectacledVariant == SpectacledVariant.JOURNALS) return false  // undated already caught above
        return when (listSortedBy) {
            ListSortedBy.CREATED -> ListGrouping.getGrouping(ListGrouping.createdGroups, created) == null
            ListSortedBy.LAST_MODIFIED -> ListGrouping.getGrouping(ListGrouping.lastModifiedGroups, lastModified ?: created) == null
            ListSortedBy.START -> ListGrouping.getGrouping(ListGrouping.startGroups, dtStart) == null
            ListSortedBy.DUE -> ListGrouping.getGrouping(ListGrouping.dueGroups, due) == null
            ListSortedBy.DATE -> dtStart == null
            ListSortedBy.SUMMARY, ListSortedBy.DRAGANDDROP -> false
        }
    }

    /** An entry whose type doesn't match this app: dated "journals" in the notes list and vice versa. */
    private fun IcalEntry.isWrongTypeForVariant(): Boolean = when (spectacledVariant) {
        SpectacledVariant.NOTES -> isJournal()
        SpectacledVariant.JOURNALS -> isNote()
        SpectacledVariant.TASKS -> false
    }

    private fun noCriteriaHeaderRes() = when (spectacledVariant) {
        SpectacledVariant.NOTES -> Res.string.grouping_other      // dated "journals" shown in the notes list
        SpectacledVariant.JOURNALS -> Res.string.grouping_no_date // undated "notes" shown in the journals list
        SpectacledVariant.TASKS -> Res.string.grouping_no_date    // tasks without the sorted date
    }
}