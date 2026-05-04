package at.techbee.spectacled.screens.list.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.PlatformInstantFormatter
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_TRASHBIN
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.list.presentation.components.EmptyListScreen
import at.techbee.spectacled.screens.list.presentation.components.IcalEntryListItem
import at.techbee.spectacled.screens.list.presentation.components.ListGroupHeader
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.trashbin


@Composable
fun JournalsListScreen(
    state: ListState,
    onAction: (ListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    val lazyStaggeredGridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.scrollToDate) {
        state.scrollToDate?.let { scrollToIcsDateTime ->
            val scrollToDayGroup = PlatformInstantFormatter(scrollToIcsDateTime).formatLocalizedDate()

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
                    lazyStaggeredGridState.animateScrollToItem(targetIndex)
                }
            }
            onAction(ListAction.OnGoToSelectedDate(null))
        }
    }


    LazyVerticalStaggeredGrid(
        //horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 1.dp,
        state = lazyStaggeredGridState,
        columns = StaggeredGridCells.Fixed(1),
        modifier = modifier
    ) {

        val groupedByDay = state.displayMapByDtStartDay
        groupedByDay.keys.forEach { dayGroup ->
            if (groupedByDay[dayGroup].isNullOrEmpty())
                return@forEach

            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = dayGroup,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, top = 11.dp, bottom = 0.dp)
                        /*
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            // Subtract 5dp from the bottom to act as negative padding and compensate for grid spacing
                            val reduction = 6.dp.roundToPx()
                            layout(placeable.width, placeable.height - reduction) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                         */
                )
            }

            if (dayGroup !in state.listCollapsedGroups) {
                itemsIndexed(
                    items = groupedByDay[dayGroup]!!,
                    key = { _, icalEntry -> icalEntry.uid }
                ) { index, icalEntry ->

                    IcalEntryListItem(
                        icalEntry = icalEntry,
                        //isFirst = (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,   // first and last are only used for list, not for the staggered grid
                        //isLast = (state.listLayout == ListLayout.LIST && index == groupedByDay[dayGroup]!!.lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
                        overrideTopRoundedCornerSize = if(index == 0) 16.dp else 0.dp,
                        overrideBottomRoundedCornerSize = if(index == groupedByDay[dayGroup]!!.lastIndex) 16.dp else 0.dp,
                        isSelected = state.multiselectItems?.contains(icalEntry.id) == true,
                        onClick = {
                            if (state.multiselectItems == null)
                                onAction(ListAction.OnIcalEntryClicked(icalEntry.id))
                            else
                                onAction(ListAction.OnToggleMultiselectItem(icalEntry.id))
                        },
                        onLongClick = { onAction(ListAction.OnToggleMultiselectItem(icalEntry.id)) },
                        modifier = Modifier
                            .widthIn(max = 700.dp)
                            .heightIn(min = 50.dp)
                            .animateItem()
                    )

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

        if(state.trashbin.isNotEmpty() && LIST_COLLAPSED_GROUP_TRASHBIN in state.listCollapsedGroups) {
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
                                onAction(ListAction.OnIcalEntryClicked(note.id))
                            else
                                onAction(ListAction.OnToggleMultiselectItem(note.id))
                        },
                        onLongClick = { onAction(ListAction.OnToggleMultiselectItem(note.id)) },
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
private fun JournalsListScreen_Preview() {

    var state = ListState()
    state = state.copy(
        spectacledVariant = SpectacledVariant.JOURNALS,
        listLayout = ListLayout.LIST,
        icalEntries = listOf(IcalEntry.getSampleJournal(), IcalEntry.getSampleJournal())
    )

    JournalsListScreen(
        state = state,
        onAction = {}
    )
}

@Preview
@Composable
private fun JournalsListScreen_Search_Preview() {

    var state = ListState()
    state = state.copy(
        searchQuery = "Lorem",
        isSearchBarExpanded = true,
        icalEntries = listOf(IcalEntry.getSampleIcalEntry(), IcalEntry.getSampleIcalEntry())
    )

    JournalsListScreen(
        state = state,
        onAction = {}
    )
}

@Preview
@Composable
private fun JournalsListScreen_empty_Preview() {

    JournalsListScreen(
        state = ListState(),
        onAction = {}
    )
}

