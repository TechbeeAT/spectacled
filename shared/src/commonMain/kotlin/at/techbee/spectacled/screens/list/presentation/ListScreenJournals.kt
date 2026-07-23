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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_TRASHBIN
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.list.presentation.components.EmptyListScreen
import at.techbee.spectacled.screens.list.presentation.components.ListGroupHeader
import at.techbee.spectacled.screens.list.presentation.components.ListItem
import at.techbee.spectacled.screens.list.presentation.components.MonthHeader
import at.techbee.spectacled.screens.list.presentation.components.TaskListItem
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.nothing_here
import spectacled.shared.generated.resources.trashbin


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
                allowEditing = state.calendar.canWriteContent() && !subtask.syncState.isDeletedState(),
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
            val scrollToDayGroup = scrollToIcsDateTime.formatLocalized(IcsDateTimeFormat.DATE)

            var targetIndex = -1
            var currentIndex = 0
            val groupedByDay = state.displayMapByDtStartDay

            for (dayGroup in groupedByDay.keys) {
                val entries = groupedByDay[dayGroup] ?: continue
                if (entries.isEmpty()) continue

                if (dayGroup == scrollToDayGroup) {
                    targetIndex = currentIndex
                    break
                }

                currentIndex++     // Add 1 for the day group header
                if (dayGroup !in state.listCollapsedGroups)
                    currentIndex += entries.size   // Add size of entries if group is expanded

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

        val groupedByMonth = state.displayMapByDtStartMonth
        groupedByMonth.keys.forEach { monthGroup ->
            if (groupedByMonth[monthGroup].isNullOrEmpty())
                return@forEach

            stickyHeader {
                MonthHeader(
                    icsDateTime = groupedByMonth[monthGroup]?.firstOrNull()?.dtStart,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (monthGroup !in state.listCollapsedGroups) {
                itemsIndexed(
                    items = groupedByMonth[monthGroup]!!,
                    key = { _, icalEntry -> icalEntry.uid }
                ) { index, icalEntry ->

                    val isNewDay = index == 0 || icalEntry.dtStart?.toLocalDateTime()?.date != groupedByMonth[monthGroup]!![index - 1].dtStart?.toLocalDateTime()?.date

                    getListItem(
                        icalEntry = icalEntry,
                        subtasks = state.subtasks[icalEntry.uid] ?: emptyList(),
                        index = index,
                        lastIndex = groupedByMonth[monthGroup]!!.lastIndex,
                        overrideTopRoundedCornerSize = if (index == 0) 16.dp else 0.dp,
                        overrideBottomRoundedCornerSize = if (index == groupedByMonth[monthGroup]!!.lastIndex) 16.dp else 0.dp,
                        showDayBlock = icalEntry.isJournal() && (index == 0 || isNewDay)
                    )

                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // TRASHBIN
        if (state.trashbin.isNotEmpty()) {

            item {
                ListGroupHeader(
                    appPreferencesTag = LIST_COLLAPSED_GROUP_TRASHBIN,
                    headerText = stringResource(Res.string.trashbin) + " \uD83D\uDDD1 " + "(${state.trashbin.size})",
                    isCollapsed = LIST_COLLAPSED_GROUP_TRASHBIN in state.listCollapsedGroups,
                    onToggleListGroupExpanded = { onAction(ListAction.OnToggleListGroupExpanded(it)) },
                    modifier = Modifier.alpha(0.33f)
                )
            }
        }

        if (state.trashbin.isNotEmpty() && LIST_COLLAPSED_GROUP_TRASHBIN in state.listCollapsedGroups) {
            if (state.trashbin.isEmpty())
                item {
                    Text(
                        text = stringResource(Res.string.nothing_here),
                        fontStyle = FontStyle.Italic
                    )
                }
            else
                items(state.trashbin, key = { note -> note.uid }) { note ->
                    getListItem(
                        icalEntry = note,
                        subtasks = state.subtasks[note.uid] ?: emptyList(),
                        index = 0,
                        lastIndex = 0,
                        showDayBlock = true,
                        modifier = Modifier.alpha(0.33f)
                    )
                }
        }

        item {
            Spacer(modifier = Modifier.height(112.dp))
        }
    }

    Crossfade(state.displayMap.values.isEmpty()) {
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

