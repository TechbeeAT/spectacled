package at.techbee.spectacled.screens.list.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.presentation.components.SplashScreen
import at.techbee.spectacled.screens.list.presentation.components.ListDragHandle
import at.techbee.spectacled.screens.list.presentation.components.TaskListItem
import at.techbee.spectacled.screens.list.presentation.components.listSections
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSection
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.no_entries_found_in_this_folder
import spectacled.shared.generated.resources.no_matching_entries_found

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

    @Composable
    fun LazyItemScope.getTaskListItem(
        icalEntry: IcalEntry,
        subtasks: List<IcalEntry> = emptyList(),
        isDragging: Boolean = false,
        dragHandle: @Composable () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        TaskListItem(
            icalEntry = icalEntry,
            isSelected = state.multiselectItems?.contains(icalEntry.id) == true || isDragging,
            allowEditing = state.calendar.canWriteContent() && !icalEntry.syncState.isDeletedState() && !icalEntry.isRecurring(),
            onClick = {
                if (state.multiselectItems == null)
                    onAction(ListAction.OnIcalEntryClicked(icalEntry.id))
                else
                    onAction(ListAction.OnToggleMultiselectItem(icalEntry.id))
            },
            onLongClick = { onAction(ListAction.OnToggleMultiselectItem(icalEntry.id)) },
            onToggleProgress = { onAction(ListAction.OnToggleProgress(icalEntry.id)) },
            dragHandle = dragHandle,
            onFilterCategory = { onAction(ListAction.OnListFilterCriteriaChanged(state.listFilterCriteria.copy(searchCategory = it))) },
            modifier = Modifier
                .widthIn(max = 700.dp)
                .heightIn(min = 50.dp)
                .then(modifier)
                .animateItem()
        )

        subtasks.forEach { subtask ->
            getTaskListItem(
                icalEntry = subtask,
                isDragging = isDragging,
                modifier = Modifier.padding(start = 48.dp)
            )
        }
    }


    LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier
    ) {

        // using mutableStateList instead of grouped list for drag and drop
        // this allows us to directly manipulate the list and avoid jitter
        if(state.listSortedBy == ListSortedBy.DRAGANDDROP && !state.showSearchBar) {
            items (dragAndDropList, key = { note -> note.uid }) { icalEntry ->

                ReorderableItem(
                    state = reorderableLazyListState,
                    key = icalEntry.uid,
                    enabled = state.calendar.canWriteContent() && !icalEntry.syncState.isDeletedState(),
                ) { isDragging ->

                    LaunchedEffect(isDragging) {
                        if (isDragging) {
                            onAction(ListAction.OnDraggingIcalEntry(icalEntry.id))
                        } else if (state.draggingIcalEntryId == icalEntry.id) {
                            onAction(ListAction.OnPersistOrderNo)
                            onAction(ListAction.OnDraggingIcalEntry(null))
                        }
                    }
                    
                    val listScope = this

                    Column(modifier = Modifier.fillMaxWidth()) {
                        this@items.getTaskListItem(
                            icalEntry = icalEntry,
                            subtasks = state.subtasks[icalEntry.uid] ?: emptyList(),
                            isDragging = isDragging,
                            dragHandle = {
                                if (state.listSortedBy == ListSortedBy.DRAGANDDROP)
                                    ListDragHandle(listScope)
                            }
                        )
                    }
                }
            }
        } else {
            listSections(
                sections = state.sections.filter { it.kind != ListSection.Kind.TRASHBIN },
                collapsedGroups = state.listCollapsedGroups,
                onToggleGroup = { onAction(ListAction.OnToggleListGroupExpanded(it)) }
            ) { icalEntry, section, _ ->
                getTaskListItem(
                    icalEntry = icalEntry,
                    subtasks = state.subtasks[icalEntry.uid] ?: emptyList(),
                    modifier = if (section.dimmed) Modifier.alpha(0.33f) else Modifier
                )
            }
        }

        // TRASHBIN (shown below both the drag-and-drop and the grouped body)
        listSections(
            sections = state.sections.filter { it.kind == ListSection.Kind.TRASHBIN },
            collapsedGroups = state.listCollapsedGroups,
            onToggleGroup = { onAction(ListAction.OnToggleListGroupExpanded(it)) }
        ) { icalEntry, section, _ ->
            getTaskListItem(
                icalEntry = icalEntry,
                subtasks = state.subtasks[icalEntry.uid] ?: emptyList(),
                modifier = if (section.dimmed) Modifier.alpha(0.33f) else Modifier
            )
        }

        item {
            Spacer(modifier = Modifier.height(112.dp))
        }
    }

        Crossfade (state.isDisplayEmpty) {
            if(it)
                SplashScreen(
                    spectacledVariant = state.spectacledVariant,
                    text = stringResource(
                        if (state.icalEntries.isEmpty())
                            Res.string.no_entries_found_in_this_folder
                        else
                            Res.string.no_matching_entries_found
                    ),
                    reducedAlpha = true,
                    modifier = Modifier.fillMaxSize()
                )
        }

}


@Preview
@Composable
private fun ListScreen_Tasks_empty_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold(modifier = Modifier.fillMaxSize()) {
            ListScreenTasks(
                state = ListState(),
                dragAndDropList = emptyList(),
                onAction = {}
            )
        }
    }
}

