package at.techbee.spectacled.screens.list.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_PINNED
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_TRASHBIN
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.list.presentation.components.EmptyListScreen
import at.techbee.spectacled.screens.list.presentation.components.ListDragHandle
import at.techbee.spectacled.screens.list.presentation.components.ListGroupHeader
import at.techbee.spectacled.screens.list.presentation.components.ListItem
import at.techbee.spectacled.screens.list.presentation.components.TaskListItem
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.pinned
import spectacled.shared.generated.resources.trashbin

@Composable
fun ListScreenNotes(
    state: ListState,
    dragAndDropList: List<IcalEntry>,
    onAction: (ListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    val hapticFeedback = LocalHapticFeedback.current

    val lazyStaggeredGridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
    val reorderableLazyListState = rememberReorderableLazyStaggeredGridState(lazyStaggeredGridState) { from, to ->
        onAction(ListAction.OnUpdateOrderNo(from.index, to.index))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    @Composable
    fun LazyStaggeredGridItemScope.getNoteListItem(
        icalEntry: IcalEntry,
        subtasks: List<IcalEntry> = emptyList(),
        index: Int,
        lastIndex: Int,
        isDragging: Boolean = false,
        dragHandle: @Composable () -> Unit = {},
        interactionSource: MutableInteractionSource? = null,
        modifier: Modifier = Modifier
    ) {

        val isFirst = state.draggingIcalEntryId != null || (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID
        val isLast = subtasks.isEmpty() || state.draggingIcalEntryId != null || (state.listLayout == ListLayout.LIST && index == lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID


        Column {
            ListItem(
                icalEntry = icalEntry,
                showDayBlock = true,
                isFirst = isFirst,
                isLast = isLast,
                isSelected = state.multiselectItems?.contains(icalEntry.id) == true || isDragging,
                interactionSource = interactionSource,
                onClick = {
                    if (state.multiselectItems == null)
                        onAction(ListAction.OnIcalEntryClicked(icalEntry.id))
                    else
                        onAction(ListAction.OnToggleMultiselectItem(icalEntry.id))
                },
                onLongClick = { onAction(ListAction.OnToggleMultiselectItem(icalEntry.id)) },
                dragHandle = dragHandle,
                onFilterCategory = { onAction(ListAction.OnListFilterCriteriaChanged(state.listFilterCriteria.copy(searchCategory = it))) },
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .heightIn(min = 50.dp)
                    .then(modifier)
                    .animateItem()
            )

            subtasks.forEach { subtask ->

                TaskListItem(
                    icalEntry = subtask,
                    onClick = {
                        if (state.multiselectItems == null)
                            onAction(ListAction.OnIcalEntryClicked(subtask.id))
                        else
                            onAction(ListAction.OnToggleMultiselectItem(subtask.id))
                    },
                    isSelected = state.multiselectItems?.contains(subtask.id) == true || isDragging,
                    onLongClick = { onAction(ListAction.OnToggleMultiselectItem(subtask.id)) },
                    onToggleProgress = { onAction(ListAction.OnToggleProgress(subtask.id)) },
                    onFilterCategory = { onAction(ListAction.OnListFilterCriteriaChanged(state.listFilterCriteria.copy(searchCategory = it))) },
                    modifier = Modifier.padding(start = 48.dp)
                )
            }
        }
    }


    LazyVerticalStaggeredGrid(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 2.dp,
        state = lazyStaggeredGridState,
        columns = when (state.listLayout) {
            ListLayout.LIST -> StaggeredGridCells.Fixed(1)
            ListLayout.STAGGERED_GRID -> StaggeredGridCells.Adaptive(150.dp)
        },
        modifier = modifier
    ) {

        // using mutableStateList instead of grouped list for drag and drop
        // this allows us to directly manipulate the list and avoid jitter
        if(state.listSortedBy == ListSortedBy.DRAGANDDROP
            && !state.listFilterCriteria.anyFilterActive()
        ) {
            itemsIndexed(dragAndDropList, key = { _, note -> note.uid }) { index, icalEntry ->

                ReorderableItem(
                    state = reorderableLazyListState,
                    key = icalEntry.uid,
                    enabled = true
                ) { isDragging ->

                    LaunchedEffect(isDragging) {
                        if (isDragging) {
                            onAction(ListAction.OnDraggingIcalEntry(icalEntry.id))
                        } else if (state.draggingIcalEntryId == icalEntry.id) {
                            onAction(ListAction.OnPersistOrderNo)
                            onAction(ListAction.OnDraggingIcalEntry(null))
                        }
                    }

                    getNoteListItem(
                        icalEntry = icalEntry,
                        subtasks = state.subtasks[icalEntry.uid] ?: emptyList(),
                        index = index,
                        lastIndex = dragAndDropList.lastIndex,
                        isDragging = isDragging,
                        interactionSource = remember { MutableInteractionSource() },
                        dragHandle = {
                            if (state.listSortedBy == ListSortedBy.DRAGANDDROP)
                                ListDragHandle(this)
                        }
                    )
                }
            }
        } else {

            if(state.pinned.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    ListGroupHeader(
                        appPreferencesTag = LIST_COLLAPSED_GROUP_PINNED,
                        headerText = stringResource(Res.string.pinned) + "  " + IcalEntry.PINNED_CATEGORY,
                        isCollapsed = LIST_COLLAPSED_GROUP_PINNED in state.listCollapsedGroups,
                        onToggleListGroupExpanded = { onAction(ListAction.OnToggleListGroupExpanded(it)) }
                    )
                }

                if (LIST_COLLAPSED_GROUP_PINNED !in state.listCollapsedGroups) {
                    itemsIndexed(
                        items = state.pinned,
                        key = { _, icalEntry -> icalEntry.uid }
                    ) { index, icalEntry ->
                        getNoteListItem(icalEntry = icalEntry, index = index, lastIndex = state.pinned.lastIndex)
                    }
                }
            }



            if (state.listSortedBy == ListSortedBy.DATE) {
                val groupedByDay = state.displayMapByDtStartDay
                groupedByDay.keys.forEach { dayGroup ->
                    if (groupedByDay[dayGroup].isNullOrEmpty())
                        return@forEach

                    item(span = StaggeredGridItemSpan.FullLine) {
                        ListGroupHeader(
                            appPreferencesTag = dayGroup,
                            headerText = dayGroup,
                            isCollapsed = dayGroup in state.listCollapsedGroups,
                            onToggleListGroupExpanded = { onAction(ListAction.OnToggleListGroupExpanded(it)) }
                        )
                    }

                    if (dayGroup !in state.listCollapsedGroups) {
                        itemsIndexed(
                            items = groupedByDay[dayGroup]!!,
                            key = { _, icalEntry -> icalEntry.uid }
                        ) { index, icalEntry ->
                            getNoteListItem(
                                icalEntry = icalEntry,
                                subtasks = state.subtasks[icalEntry.uid] ?: emptyList(),
                                index = index,
                                lastIndex = groupedByDay[dayGroup]!!.lastIndex
                            )
                        }
                    }
                }

            } else {
                // No drag and drop. We build the grouped list based on the map

                state.displayMap.keys.forEach { grouping ->

                        if (state.displayMap[grouping].isNullOrEmpty())
                            return@forEach

                        if (grouping.stringRes != null) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                ListGroupHeader(
                                    appPreferencesTag = grouping.name,
                                    headerText = if (grouping.stringResParam != null)
                                        stringResource(grouping.stringRes, grouping.stringResParam)
                                    else
                                        stringResource(grouping.stringRes),
                                    isCollapsed = grouping.name in state.listCollapsedGroups,
                                    onToggleListGroupExpanded = { onAction(ListAction.OnToggleListGroupExpanded(it)) }
                                )
                            }
                        }

                        if (grouping.name !in state.listCollapsedGroups) {
                            itemsIndexed(state.displayMap[grouping]!!, key = { _, icalEntry -> icalEntry.uid }) { index, icalEntry ->
                                getNoteListItem(
                                    icalEntry = icalEntry,
                                    subtasks = state.subtasks[icalEntry.uid] ?: emptyList(),
                                    index = index,
                                    lastIndex = state.displayMap[grouping]!!.lastIndex
                                )
                            }
                        }
                    }
            }

        }
        // TRASHBIN
        if(state.trashbin.isNotEmpty()) {

            item(span = StaggeredGridItemSpan.FullLine) {
                ListGroupHeader(
                    appPreferencesTag = LIST_COLLAPSED_GROUP_TRASHBIN,
                    headerText = stringResource(Res.string.trashbin) + " \uD83D\uDDD1 " + "(${state.trashbin.size})",
                    isCollapsed = LIST_COLLAPSED_GROUP_TRASHBIN in state.listCollapsedGroups,
                    onToggleListGroupExpanded = { onAction(ListAction.OnToggleListGroupExpanded(it)) },
                    modifier = Modifier.alpha(0.33f)
                )
            }
        }

        if(state.trashbin.isNotEmpty() &&  LIST_COLLAPSED_GROUP_TRASHBIN in state.listCollapsedGroups) {
            if (state.trashbin.isEmpty())
                item { Text(
                    text = "Nothing here",
                    fontStyle = FontStyle.Italic
                ) }
            else
                itemsIndexed(state.trashbin, key = { _, note -> note.uid }) { index, note ->
                    getNoteListItem(
                        icalEntry = note,
                        subtasks = state.subtasks[note.uid] ?: emptyList(),
                        index = index,
                        lastIndex = state.trashbin.lastIndex,
                        modifier = Modifier.alpha(0.33f)
                    )
                }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(112.dp))
        }
    }

        Crossfade (state.displayMap.values.isEmpty()) {
            if(it)
                EmptyListScreen(
                    isEmptyFolder = state.icalEntries.isEmpty(),
                    spectacledVariant = state.spectacledVariant,
                    modifier = Modifier.fillMaxSize()
                )
        }

}

@Preview
@Composable
private fun ListScreenRoot_Preview() {
    ListScreenRoot(
        listViewModel = koinViewModel<ListViewModel>(),
        onNavigate = { },
        onNavigateUp = { }
    )
}


@Preview
@Composable
private fun ListScreen_Notes_Preview() {

    val state = ListState(
        listFilterCriteria = ListFilterCriteria(searchQuery = "test")
    )

    ListScreenNotes(
        state = state,
        dragAndDropList = emptyList(),
        onAction = {}
    )
}

@Preview
@Composable
private fun ListScreen_Notes_empty_Preview() {

    ListScreenNotes(
        state = ListState(),
        dragAndDropList = emptyList(),
        onAction = {}
    )
}

