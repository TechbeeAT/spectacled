package at.techbee.spectacled.screens.list.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import at.techbee.spectacled.screens.list.presentation.components.TaskListItem
import at.techbee.spectacled.screens.list.presentation.datastructures.ListGrouping
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.pinned
import spectacled.shared.generated.resources.trashbin

@Composable
fun ListScreenTasks(
    state: ListState,
    dragAndDropList: List<IcalEntry>,
    onAction: (ListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    val hapticFeedback = LocalHapticFeedback.current

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onAction(ListAction.OnUpdateOrderNo(from.index, to.index))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }


    LazyColumn(
        state = lazyListState,
        modifier = modifier
    ) {

        // using mutableStateList instead of grouped list for drag and drop
        // this allows us to directly manipulate the list and avoid jitter
        if(state.listSortedBy == ListSortedBy.DRAGANDDROP
            && state.searchQuery == null
            && state.searchCategory == null
        ) {
            items (dragAndDropList, key = { note -> note.uid }) { icalEntry ->

                ReorderableItem(
                    state = reorderableLazyListState,
                    key = icalEntry.uid,
                    enabled = true
                ) { isDragging ->

                    if(isDragging)
                        onAction(ListAction.OnDraggingIcalEntry(icalEntry.id))
                    else if (state.draggingIcalEntryId == icalEntry.id && !isDragging) {
                        onAction(ListAction.OnPersistOrderNo)
                        onAction(ListAction.OnDraggingIcalEntry(null))
                    }

                    TaskListItem(
                        icalEntry = icalEntry,
                        isSelected = state.multiselectItems?.contains(icalEntry.id) == true,
                        onClick = {
                            if (state.multiselectItems == null)
                                onAction(ListAction.OnIcalEntryClicked(icalEntry.id))
                            else
                                onAction(ListAction.OnToggleMultiselectItem(icalEntry.id))
                        },
                        onLongClick = { onAction(ListAction.OnToggleMultiselectItem(icalEntry.id)) },
                        onToggleProgress = { onAction(ListAction.OnToggleProgress(icalEntry.id)) },
                        dragHandle = {
                            if (state.listSortedBy == ListSortedBy.DRAGANDDROP)
                                ListDragHandle(this)
                        },
                        onFilterCategory = { onAction(ListAction.OnCategoryFilterChanged(it))},
                        modifier = Modifier
                            .widthIn(max = 700.dp)
                            .heightIn(min = 50.dp)
                            .animateItem()
                    )
                }
            }
        } else {

            if(state.pinned.isNotEmpty()) {
                item {
                    ListGroupHeader(
                        appPreferencesTag = LIST_COLLAPSED_GROUP_PINNED,
                        headerText = stringResource(Res.string.pinned) + "  " + IcalEntry.PINNED_CATEGORY,
                        isCollapsed = LIST_COLLAPSED_GROUP_PINNED in state.listCollapsedGroups,
                        onToggleListGroupExpanded = { onAction(ListAction.OnToggleListGroupExpanded(it)) }
                    )
                }

                if (LIST_COLLAPSED_GROUP_PINNED !in state.listCollapsedGroups) {
                    items(
                        items = state.pinned,
                        key = { icalEntry -> icalEntry.uid }
                    ) { icalEntry ->

                        TaskListItem(
                            icalEntry = icalEntry,
                            isSelected = state.multiselectItems?.contains(icalEntry.id) == true,
                            onClick = {
                                if (state.multiselectItems == null)
                                    onAction(ListAction.OnIcalEntryClicked(icalEntry.id))
                                else
                                    onAction(ListAction.OnToggleMultiselectItem(icalEntry.id))
                            },
                            onLongClick = { onAction(ListAction.OnToggleMultiselectItem(icalEntry.id)) },
                            onFilterCategory = { onAction(ListAction.OnCategoryFilterChanged(it))},
                            onToggleProgress = { onAction(ListAction.OnToggleProgress(icalEntry.id)) },
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

                    item {
                        ListGroupHeader(
                            appPreferencesTag = dayGroup,
                            headerText = dayGroup,
                            isCollapsed = dayGroup in state.listCollapsedGroups,
                            onToggleListGroupExpanded = { onAction(ListAction.OnToggleListGroupExpanded(it)) }
                        )
                    }

                    if (dayGroup !in state.listCollapsedGroups) {
                        items(
                            items = groupedByDay[dayGroup]!!,
                            key = { icalEntry -> icalEntry.uid }
                        ) { icalEntry ->

                            TaskListItem(
                                icalEntry = icalEntry,
                                isSelected = state.multiselectItems?.contains(icalEntry.id) == true,
                                onClick = {
                                    if (state.multiselectItems == null)
                                        onAction(ListAction.OnIcalEntryClicked(icalEntry.id))
                                    else
                                        onAction(ListAction.OnToggleMultiselectItem(icalEntry.id))
                                },
                                onLongClick = { onAction(ListAction.OnToggleMultiselectItem(icalEntry.id)) },
                                onFilterCategory = { onAction(ListAction.OnCategoryFilterChanged(it))},
                                onToggleProgress = { onAction(ListAction.OnToggleProgress(icalEntry.id)) },
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
                            item {
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
                            items(state.displayMap[grouping]!!, key = { icalEntry -> icalEntry.uid }) { icalEntry ->

                                TaskListItem(
                                    icalEntry = icalEntry,
                                    isSelected = state.multiselectItems?.contains(icalEntry.id) == true,
                                    onClick = {
                                        if (state.multiselectItems == null)
                                            onAction(ListAction.OnIcalEntryClicked(icalEntry.id))
                                        else
                                            onAction(ListAction.OnToggleMultiselectItem(icalEntry.id))
                                    },
                                    onLongClick = { onAction(ListAction.OnToggleMultiselectItem(icalEntry.id)) },
                                    onFilterCategory = { onAction(ListAction.OnCategoryFilterChanged(it))},
                                    onToggleProgress = { onAction(ListAction.OnToggleProgress(icalEntry.id)) },
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

        if(state.trashbin.isNotEmpty() &&  LIST_COLLAPSED_GROUP_TRASHBIN in state.listCollapsedGroups) {
            if (state.trashbin.isEmpty())
                item { Text(
                    text = "Nothing here",
                    fontStyle = FontStyle.Italic
                ) }
            else
                items(state.trashbin, key = { note -> note.uid }) { icalEntry ->

                    TaskListItem(
                        icalEntry = icalEntry,
                        isSelected = state.multiselectItems?.contains(icalEntry.id) == true,
                        onClick = {
                            if (state.multiselectItems == null)
                                onAction(ListAction.OnIcalEntryClicked(icalEntry.id))
                            else
                                onAction(ListAction.OnToggleMultiselectItem(icalEntry.id))
                        },
                        onLongClick = { onAction(ListAction.OnToggleMultiselectItem(icalEntry.id)) },
                        onFilterCategory = { onAction(ListAction.OnCategoryFilterChanged(it))},
                        onToggleProgress = { onAction(ListAction.OnToggleProgress(icalEntry.id)) },
                        modifier = Modifier
                            .widthIn(max = 700.dp)
                            .heightIn(min = 50.dp)
                            .animateItem()
                            .alpha(0.33f)
                    )
                }
        }

        item {
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

    var state = ListState()
    state = state.copy(searchQuery = "test")

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

