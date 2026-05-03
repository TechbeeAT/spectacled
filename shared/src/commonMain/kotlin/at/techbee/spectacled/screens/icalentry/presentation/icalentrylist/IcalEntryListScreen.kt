package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.EmptyListScreen
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.IcalEntryDragHandle
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.IcalEntryListItem
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.ListGroupHeader
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListGrouping
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListLayout
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListSortedBy
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.pinned
import spectacled.shared.generated.resources.trashbin

@Composable
fun IcalEntryListScreen(
    state: IcalEntryListState,
    dragAndDropList: List<IcalEntry>,
    onAction: (IcalEntryListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    val hapticFeedback = LocalHapticFeedback.current

    val lazyStaggeredGridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
    val reorderableLazyListState = rememberReorderableLazyStaggeredGridState(lazyStaggeredGridState) { from, to ->
        onAction(IcalEntryListAction.OnUpdateOrderNo(from.index, to.index))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
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
            && !state.isSearchBarExpanded
            && state.searchCategory.isEmpty()
        ) {
            itemsIndexed(dragAndDropList, key = { _, note -> note.uid }) { index, icalEntry ->

                ReorderableItem(
                    state = reorderableLazyListState,
                    key = icalEntry.uid,
                    enabled = true
                ) { isDragging ->

                    if(isDragging)
                        onAction(IcalEntryListAction.OnDraggingIcalEntry(icalEntry.id))
                    else if (state.draggingIcalEntryId == icalEntry.id && !isDragging) {
                        onAction(IcalEntryListAction.OnPersistOrderNo)
                        onAction(IcalEntryListAction.OnDraggingIcalEntry(null))
                    }

                    val interactionSource = remember { MutableInteractionSource() }

                    IcalEntryListItem(
                        icalEntry = icalEntry,
                        isFirst = state.draggingIcalEntryId != null || (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,   // first and last are only used for list, not for the staggered grid
                        isLast = state.draggingIcalEntryId != null || (state.listLayout == ListLayout.LIST && index == dragAndDropList.lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
                        isSelected = state.multiselectItems?.contains(icalEntry.id) == true,
                        onClick = {
                            if (state.multiselectItems == null)
                                onAction(IcalEntryListAction.OnIcalEntryClicked(icalEntry.id))
                            else
                                onAction(IcalEntryListAction.OnToggleMultiselectItem(icalEntry.id))
                        },
                        onLongClick = { onAction(IcalEntryListAction.OnToggleMultiselectItem(icalEntry.id)) },
                        interactionSource = interactionSource,
                        dragHandle = {
                            if (state.listSortedBy == ListSortedBy.DRAGANDDROP)
                                IcalEntryDragHandle(this)
                        },
                        modifier = Modifier
                            .widthIn(max = 700.dp)
                            .heightIn(min = 50.dp)
                            .animateItem()
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
                        onToggleListGroupExpanded = { onAction(IcalEntryListAction.OnToggleListGroupExpanded(it)) }
                    )
                }

                if (LIST_COLLAPSED_GROUP_PINNED !in state.listCollapsedGroups) {
                    itemsIndexed(
                        items = state.pinned,
                        key = { _, icalEntry -> icalEntry.uid }
                    ) { index, icalEntry ->

                        IcalEntryListItem(
                            icalEntry = icalEntry,
                            isFirst = (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,   // first and last are only used for list, not for the staggered grid
                            isLast = (state.listLayout == ListLayout.LIST && index == state.pinned.lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
                            isSelected = state.multiselectItems?.contains(icalEntry.id) == true,
                            onClick = {
                                if (state.multiselectItems == null)
                                    onAction(IcalEntryListAction.OnIcalEntryClicked(icalEntry.id))
                                else
                                    onAction(IcalEntryListAction.OnToggleMultiselectItem(icalEntry.id))
                            },
                            onLongClick = { onAction(IcalEntryListAction.OnToggleMultiselectItem(icalEntry.id)) },
                            modifier = Modifier
                                .widthIn(max = 700.dp)
                                .heightIn(min = 50.dp)
                                .animateItem()
                        )

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
                            onToggleListGroupExpanded = { onAction(IcalEntryListAction.OnToggleListGroupExpanded(it)) }
                        )
                    }

                    if (dayGroup !in state.listCollapsedGroups) {
                        itemsIndexed(
                            items = groupedByDay[dayGroup]!!,
                            key = { _, icalEntry -> icalEntry.uid }
                        ) { index, icalEntry ->

                            IcalEntryListItem(
                                icalEntry = icalEntry,
                                isFirst = (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,   // first and last are only used for list, not for the staggered grid
                                isLast = (state.listLayout == ListLayout.LIST && index == groupedByDay[dayGroup]!!.lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
                                isSelected = state.multiselectItems?.contains(icalEntry.id) == true,
                                onClick = {
                                    if (state.multiselectItems == null)
                                        onAction(IcalEntryListAction.OnIcalEntryClicked(icalEntry.id))
                                    else
                                        onAction(IcalEntryListAction.OnToggleMultiselectItem(icalEntry.id))
                                },
                                onLongClick = { onAction(IcalEntryListAction.OnToggleMultiselectItem(icalEntry.id)) },
                                modifier = Modifier
                                    .widthIn(max = 700.dp)
                                    .heightIn(min = 50.dp)
                                    .animateItem()
                            )

                        }
                    }
                }

            } else {
                // No drag and drop. We build the grouped list based on the map

                ListGrouping.entries
                    .let { if (state.listSortedByAscending) it else it.asReversed() }
                    .forEach { grouping ->

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
                                    onToggleListGroupExpanded = { onAction(IcalEntryListAction.OnToggleListGroupExpanded(it)) }
                                )
                            }
                        }

                        if (grouping.name !in state.listCollapsedGroups) {
                            itemsIndexed(state.displayMap[grouping]!!, key = { _, icalEntry -> icalEntry.uid }) { index, note ->

                                IcalEntryListItem(
                                    icalEntry = note,
                                    isFirst = (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,   // first and last are only used for list, not for the staggered grid
                                    isLast = (state.listLayout == ListLayout.LIST && index == state.displayMap[grouping]!!.lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
                                    isSelected = state.multiselectItems?.contains(note.id) == true,
                                    onClick = {
                                        if (state.multiselectItems == null)
                                            onAction(IcalEntryListAction.OnIcalEntryClicked(note.id))
                                        else
                                            onAction(IcalEntryListAction.OnToggleMultiselectItem(note.id))
                                    },
                                    onLongClick = { onAction(IcalEntryListAction.OnToggleMultiselectItem(note.id)) },
                                    modifier = Modifier
                                        .widthIn(max = 700.dp)
                                        .heightIn(min = 50.dp)
                                        .animateItem()
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
                    onToggleListGroupExpanded = { onAction(IcalEntryListAction.OnToggleListGroupExpanded(it)) },
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

                    IcalEntryListItem(
                        icalEntry = note,
                        isFirst = (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,   // first and last are only used for list, not for the staggered grid
                        isLast = (state.listLayout == ListLayout.LIST && index == state.trashbin.lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
                        isSelected = state.multiselectItems?.contains(note.id) == true,
                        onClick = {
                            if (state.multiselectItems == null)
                                onAction(IcalEntryListAction.OnIcalEntryClicked(note.id))
                            else
                                onAction(IcalEntryListAction.OnToggleMultiselectItem(note.id))
                        },
                        onLongClick = { onAction(IcalEntryListAction.OnToggleMultiselectItem(note.id)) },
                        modifier = Modifier
                            .widthIn(max = 700.dp)
                            .heightIn(min = 50.dp)
                            .animateItem()
                            .alpha(0.33f)
                    )
                }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

        Crossfade (state.displayMap.values.isEmpty()) {
            if(it)
                EmptyListScreen(
                    isEmptyFolder = state.icalEntries.isEmpty(),
                    modifier = Modifier.fillMaxSize()
                )
        }

}

@Preview
@Composable
private fun IcalEntryListScreenRoot_Preview() {
    IcalEntryListScreenRoot(
        icalEntryListViewModel = koinViewModel<IcalEntryListViewModel>(),
        onNavigate = { },
        onNavigateUp = { }
    )
}


@Preview
@Composable
private fun IcalEntryListScreen_Preview() {

    var state = IcalEntryListState()
    state = state.copy(
        searchQuery = "test",
        isSearchBarExpanded = true
    )

    IcalEntryListScreen(
        state = state,
        dragAndDropList = emptyList(),
        onAction = {}
    )
}

@Preview
@Composable
private fun IcalEntryListScreen_empty_Preview() {

    IcalEntryListScreen(
        state = IcalEntryListState(),
        dragAndDropList = emptyList(),
        onAction = {}
    )
}

