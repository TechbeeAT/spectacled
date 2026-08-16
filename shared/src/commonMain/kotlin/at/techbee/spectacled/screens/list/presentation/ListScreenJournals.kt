package at.techbee.spectacled.screens.list.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.list.presentation.components.EmptyListScreen
import at.techbee.spectacled.screens.list.presentation.components.ListItem
import at.techbee.spectacled.screens.list.presentation.components.TaskListItem
import at.techbee.spectacled.screens.list.presentation.components.listSections
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSection
import kotlinx.coroutines.launch


@Composable
fun JournalsListJournals(
    state: ListState,
    onAction: (ListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    @Composable
    fun LazyItemScope.getListItem(
        icalEntry: IcalEntry,
        subtasks: List<IcalEntry> = emptyList(),
        index: Int,
        lastIndex: Int,
        showDayBlock: Boolean = false,
        overrideTopRoundedCornerSize: Dp? = null,
        overrideBottomRoundedCornerSize: Dp? = null,
        isFirst: Boolean = (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,
        isLast: Boolean = (state.listLayout == ListLayout.LIST && index == lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
        modifier: Modifier = Modifier
    ) {
        ListItem(
            icalEntry = icalEntry,
            isFirst = isFirst,
            isLast = isLast,
            overrideTopRoundedCornerSize = overrideTopRoundedCornerSize,
            overrideBottomRoundedCornerSize = overrideBottomRoundedCornerSize,
            isSelected = state.multiselectItems?.contains(icalEntry.id) == true,
            onClick = {
                if (state.multiselectItems == null)
                    onAction(ListAction.OnIcalEntryClicked(icalEntry.id))
                else
                    onAction(ListAction.OnToggleMultiselectItem(icalEntry.id))
            },
            onLongClick = { onAction(ListAction.OnToggleMultiselectItem(icalEntry.id)) },
            onFilterCategory = { onAction(ListAction.OnListFilterCriteriaChanged(state.listFilterCriteria.copy(searchCategory = it))) },
            showDayBlock = showDayBlock,
            modifier = Modifier
                .widthIn(max = 700.dp)
                .heightIn(min = 50.dp)
                .padding(end = 8.dp, start = if(icalEntry.isNote()) 44.dp else 0.dp)
                .then(modifier)
                .animateItem()
        )

        subtasks.forEach { subtask ->

            TaskListItem(
                icalEntry = subtask,
                isSelected = state.multiselectItems?.contains(subtask.id) == true,
                allowEditing = state.calendar.canWriteContent() && !subtask.syncState.isDeletedState() && !subtask.isRecurring(),
                onClick = {
                    if (state.multiselectItems == null)
                        onAction(ListAction.OnIcalEntryClicked(subtask.id))
                    else
                        onAction(ListAction.OnToggleMultiselectItem(subtask.id))
                },
                onLongClick = { onAction(ListAction.OnToggleMultiselectItem(subtask.id)) },
                onToggleProgress = { onAction(ListAction.OnToggleProgress(subtask.id)) },
                onFilterCategory = { onAction(ListAction.OnListFilterCriteriaChanged(state.listFilterCriteria.copy(searchCategory = it))) },
                modifier = Modifier.padding(start = 64.dp, end = 16.dp)
            )
        }
    }

    LaunchedEffect(state.scrollToDate) {
        state.scrollToDate?.let { scrollToIcsDateTime ->
            val scrollToDay = scrollToIcsDateTime.formatLocalized(IcsDateTimeFormat.DATE)

            // Walk the rendered sections (a header item plus its entries) to find the flat list index
            // of the first grouped entry on the requested day, honouring collapsed sections.
            var flatIndex = 0
            var targetIndex = -1
            for (section in state.sections) {
                if (section.entries.isEmpty()) continue
                if (section.header != null) flatIndex += 1
                val collapsed = section.header != null && section.key in state.listCollapsedGroups
                if (collapsed) continue

                if (section.kind == ListSection.Kind.GROUPED) {
                    val entryIndex = section.entries.indexOfFirst {
                        it.dtStart?.formatLocalized(IcsDateTimeFormat.DATE) == scrollToDay
                    }
                    if (entryIndex >= 0) {
                        targetIndex = flatIndex + entryIndex
                        break
                    }
                }
                flatIndex += section.entries.size
            }

            if (targetIndex != -1) {
                scope.launch {
                    lazyListState.animateScrollToItem(targetIndex)
                }
            }
            onAction(ListAction.OnGoToSelectedDate(null))
        }
    }


    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        state = lazyListState,
        modifier = modifier
    ) {

        listSections(
            sections = state.sections,
            collapsedGroups = state.listCollapsedGroups,
            onToggleGroup = { onAction(ListAction.OnToggleListGroupExpanded(it)) }
        ) { icalEntry, section, index ->

            if (section.kind == ListSection.Kind.GROUPED) {
                // Month section: round the block corners and show a day block on each new day.
                val isNewDay = index == 0 ||
                        icalEntry.dtStart?.toLocalDateTime()?.date != section.entries[index - 1].dtStart?.toLocalDateTime()?.date

                getListItem(
                    icalEntry = icalEntry,
                    subtasks = state.subtasks[icalEntry.uid] ?: emptyList(),
                    index = index,
                    lastIndex = section.entries.lastIndex,
                    overrideTopRoundedCornerSize = if (index == 0) 16.dp else 0.dp,
                    overrideBottomRoundedCornerSize = if (index == section.entries.lastIndex) 16.dp else 0.dp,
                    showDayBlock = icalEntry.isJournal() && (index == 0 || isNewDay)
                )
            } else {
                // Pinned / no-criteria / trashbin section (dimmed for the trashbin).
                getListItem(
                    icalEntry = icalEntry,
                    subtasks = state.subtasks[icalEntry.uid] ?: emptyList(),
                    index = index,
                    lastIndex = section.entries.lastIndex,
                    showDayBlock = icalEntry.isJournal(),
                    modifier = if (section.dimmed) Modifier.alpha(0.33f) else Modifier
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(112.dp))
        }
    }

    Crossfade(state.isDisplayEmpty) {
        if (it)
            EmptyListScreen(
                isEmptyFolder = state.icalEntries.isEmpty(),
                spectacledVariant = state.spectacledVariant,
                modifier = Modifier.fillMaxSize()
            )
    }

}


@Preview
@Composable
private fun JournalsListJournals_Preview() {

    var state = ListState()
    state = state.copy(
        spectacledVariant = SpectacledVariant.JOURNALS,
        listLayout = ListLayout.LIST,
        icalEntries = listOf(IcalEntry.getSampleJournal(), IcalEntry.getSampleJournal())
    )

    JournalsListJournals(
        state = state,
        onAction = {}
    )
}

@Preview
@Composable
private fun JournalsListJournals_Search_Preview() {

    var state = ListState()
    state = state.copy(
        listFilterCriteria = ListFilterCriteria(searchQuery = "Lorem"),
        icalEntries = listOf(IcalEntry.getSampleIcalEntry(), IcalEntry.getSampleIcalEntry())
    )

    JournalsListJournals(
        state = state,
        onAction = {}
    )
}

@Preview
@Composable
private fun JournalsListJournals_empty_Preview() {

    JournalsListJournals(
        state = ListState(),
        onAction = {}
    )
}

