package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.Route.IcalEntryDetails
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_PINNED
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_TRASHBIN
import at.techbee.spectacled.screens.core.presentation.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.ColorSelectorElement
import at.techbee.spectacled.screens.core.presentation.CustomBottomSnackbarHost
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.DeleteSelectedItemsDialog
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.EmptyListScreen
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.IcalEntryDragHandle
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.IcalEntryListItem
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.IcalEntryListTopBar
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.ListGroupHeader
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListGrouping
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListLayout
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListSortedBy
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces
import at.techbee.spectacled.theme.getThemeForColoredSurfaces
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_entry
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.pinned
import spectacled.shared.generated.resources.read_only
import spectacled.shared.generated.resources.search
import spectacled.shared.generated.resources.trashbin


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcalEntryListScreenRoot(
    icalEntryListViewModel: IcalEntryListViewModel,
    onNavigate: (Route) -> Unit,
    onNavigateUp: () -> Unit
) {

    //val drawerState = rememberDrawerState(DrawerValue.Closed)
    val state = icalEntryListViewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    val customColors = getThemeForColoredSurfaces(state.calendar.color)
    val contentColor = getContentColorForColoredSurfaces(state.calendar.color)

    LaunchedEffect(state.snackbarText) {
        state.snackbarText?.let { message ->
            snackbarHostState.showSnackbar(message)
            icalEntryListViewModel.onAction(IcalEntryListAction.OnUpdateSnackbar(null))
        }
    }

    LaunchedEffect(state.navigateUp) {
        if(state.navigateUp) {
            onNavigateUp()
            icalEntryListViewModel.onAction(IcalEntryListAction.OnNavigateUp(false))
        }
    }

    if(state.showDeleteSelectedItemsDialog && state.multiselectItems?.isNotEmpty() == true) {
        DeleteSelectedItemsDialog(
            multiselectItems = state.multiselectItems,
            onConfirm = { icalEntryListViewModel.onAction(IcalEntryListAction.OnDeleteSelectedItems) },
            onDismiss = { icalEntryListViewModel.onAction(IcalEntryListAction.OnShowDeleteSelectedItemsDialog(false)) }
        )
    }

    if(state.showUpdateColorOfSelectedBottomSheet) {
        BottomSheetWithMenu(
            onDismiss = { icalEntryListViewModel.onAction(IcalEntryListAction.OnShowUpdateColorOfSelectedBottomSheet(false)) },
        ) {
            ColorSelectorElement(
                recentColors = state.icalEntries
                    .mapNotNull { it.color }
                    .distinct(),
                preselectedColor = state.icalEntries
                    .filter { note -> state.multiselectItems?.contains(note.id) == true }
                    .map { it.color }
                    .distinct()
                    .let { colorList -> if(colorList.size == 1) colorList.first() else Color.Transparent },
                onColorChanged = { icalEntryListViewModel.onAction(IcalEntryListAction.OnUpdateColorOfSelected(it)) },
                skipPartialSelection = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    CompositionLocalProvider(LocalContentColor provides customColors.onSurface) {
        MaterialTheme(colorScheme = customColors) {

            Scaffold(
                topBar = {
                    IcalEntryListTopBar(
                        calendar = state.calendar,
                        //drawerState = drawerState,
                        isSearchBarExpanded = state.isSearchBarExpanded,
                        listSortedBy = state.listSortedBy,
                        sortedAscending = state.listSortedByAscending,
                        listLayout = state.listLayout,
                        multiselectItems = state.multiselectItems,
                        onAction = { action -> icalEntryListViewModel.onAction(action) },
                        onSurfaceTint = contentColor,
                        allSelectedPinned = state.multiselectItems?.all { selectedId ->
                            state.icalEntries.find { entry -> entry.id == selectedId }?.categories?.contains(IcalEntry.PINNED_CATEGORY) == true
                        } == true,
                        modifier = Modifier.fillMaxWidth(1f)
                    )
                },
                floatingActionButton = {
                    // only shown if multiselect is null
                    if(state.multiselectItems == null) {
                        if (state.calendar.canWriteContent()) {
                            ExtendedFloatingActionButton(
                                onClick = { onNavigate(Route.AddICalEntry(state.calendar.id)) },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                                        contentDescription = stringResource(Res.string.add_entry),
                                        tint = getContentColorForColoredSurfaces(icalEntryListViewModel.state.calendar.color, true)
                                    )
                                    Text(
                                        text = "Add note",
                                        color = getContentColorForColoredSurfaces(icalEntryListViewModel.state.calendar.color, true)
                                    )
                                }
                            }
                        } else {
                            ExtendedFloatingActionButton(
                                onClick = { icalEntryListViewModel.onAction(IcalEntryListAction.OnUpdateSnackbar("No write access to this collection")) },
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.EditOff,
                                        contentDescription = stringResource(Res.string.read_only),
                                        tint = getContentColorForColoredSurfaces(icalEntryListViewModel.state.calendar.color, true)
                                    )
                                    Text(
                                        text = stringResource(Res.string.read_only),
                                        color = getContentColorForColoredSurfaces(icalEntryListViewModel.state.calendar.color, true)
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    // WORKAROUND, keep the Bottom App Bar empty like this to color the lower part in the container color!
                    BottomAppBar(modifier = Modifier.height(32.dp)) {}
                }
                /*
               bottomBar = {
                   BottomAppBar(
                       actions = {

                           Row(
                               horizontalArrangement = Arrangement.SpaceBetween,
                               verticalAlignment = Alignment.CenterVertically,
                               modifier = Modifier
                                   .fillMaxWidth()
                                   .padding(horizontal = if (getPlatform().isIos()) 0.dp else 8.dp)
                           ) {

                               /*
                               IconButton(
                                   onClick = {
                                       //showAboutDialog = true
                                       }
                               ) {
                                   Icon(
                                       imageVector = Icons.Outlined.Info,
                                       contentDescription = stringResource(Res.string.about),
                                       tint = if (getPlatform().isIos()) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                   )
                               }

                                */

                               Text(
                                   text = "${state.notes.size} Notes",
                                   style = MaterialTheme.typography.labelLarge
                               )

                               if(getPlatform().isIos())
                                   TextButton(
                                       onClick = {
                                           onNavigate(Route.AddNote)
                                       }
                                   ) {
                                       Icon(
                                           Icons.Outlined.Add,
                                           stringResource(Res.string.add_note),
                                           )
                                   }
                               else
                                   FloatingActionButton(
                                       onClick = {
                                           onNavigate(Route.AddNote)
                                       }
                                   ) {
                                       Icon(Icons.Outlined.Add, stringResource(Res.string.add_note))
                                   }
                           }
                       },
                       floatingActionButton = {
                           FloatingActionButton(
                               onClick = {
                                   onNavigate(Route.AddNote)
                               }
                           ) {
                               Icon(Icons.Outlined.Add, stringResource(Res.string.add_note))
                           }
                       }
                   )

               },
               */

            ) { paddingValues ->

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {

                    IcalEntryListScreen(
                        state = state,
                        dragAndDropList = icalEntryListViewModel.dragAndDropList,
                        onAction = { action ->
                            when (action) {
                                is IcalEntryListAction.OnIcalEntryClicked -> onNavigate(IcalEntryDetails(action.id))
                                else -> icalEntryListViewModel.onAction(action)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    CustomBottomSnackbarHost(
                        snackbarHostState = snackbarHostState,
                        keepSpaceForFAB = true
                    )
                }
            }
        }
    }
}


@Composable
fun IcalEntryListScreen(
    state: IcalEntryListState,
    dragAndDropList: List<IcalEntry>,
    onAction: (IcalEntryListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    val searchBarFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val hapticFeedback = LocalHapticFeedback.current

    val lazyStaggeredGridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
    val reorderableLazyListState = rememberReorderableLazyStaggeredGridState(lazyStaggeredGridState) { from, to ->
        onAction(IcalEntryListAction.OnUpdateOrderNo(from.index, to.index))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    val pullToRefreshState = rememberPullToRefreshState()
    var draggingNoteId by remember { mutableStateOf(null as Long?) }

    LaunchedEffect(state.isSearchBarExpanded) {
        if (state.isSearchBarExpanded) {
            searchBarFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            searchBarFocusRequester.freeFocus()
            keyboardController?.hide()
        }
    }

    Column(modifier = modifier) {

        AnimatedVisibility(state.isSearchBarExpanded) {
            TextField(
                placeholder = { Text(stringResource(Res.string.search)) },
                value = state.searchQuery,
                onValueChange = { onAction(IcalEntryListAction.OnSearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .focusRequester(searchBarFocusRequester)
            )
        }

        val allCategories = state.icalEntries.flatMap { it.categories }.distinct()

        AnimatedVisibility(state.isSearchBarExpanded) {
            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                item {
                    Icon(
                        Icons.AutoMirrored.Outlined.Label,
                        stringResource(Res.string.category),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                items(allCategories, key = { it }) { category ->
                    ElevatedFilterChip(
                        selected = state.searchCategory.equals(category, ignoreCase = true),
                        onClick = {
                            if (category.equals(state.searchCategory, ignoreCase = true))
                                onAction(IcalEntryListAction.OnCategoryFilterChanged(""))
                            else
                                onAction(IcalEntryListAction.OnCategoryFilterChanged(category))
                        },
                        label = { Text(category) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }


        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(IcalEntryListAction.OnTriggerSync)  },
            state = pullToRefreshState,
            indicator = {
                if(draggingNoteId != null) {  // don't show indicator when dragging to avoid conflict
                    return@PullToRefreshBox
                } else {
                    Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = state.isRefreshing,
                        state = pullToRefreshState,
                    )
                }
            }
        ) {

            LazyVerticalStaggeredGrid(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 2.dp,
                state = lazyStaggeredGridState,
                columns = when (state.listLayout) {
                    ListLayout.LIST -> StaggeredGridCells.Fixed(1)
                    ListLayout.STAGGERED_GRID -> StaggeredGridCells.Adaptive(150.dp)
                },
                modifier = Modifier.fillMaxSize()
            ) {

                // using mutableStateList instead of grouped list for drag and drop
                // this allows us to directly manipulate the list and avoid jitter
                if(state.listSortedBy == ListSortedBy.DRAGANDDROP
                    && !state.isSearchBarExpanded
                    && state.searchCategory.isEmpty()
                ) {
                    itemsIndexed(dragAndDropList, key = { _, note -> note.uid }) { index, note ->

                        ReorderableItem(
                            state = reorderableLazyListState,
                            key = note.uid,
                            enabled = true
                        ) { isDragging ->

                            if(isDragging)
                                draggingNoteId = note.id
                            else if (draggingNoteId == note.id && !isDragging) {
                                onAction(IcalEntryListAction.OnPersistOrderNo)
                                draggingNoteId = null
                            }

                            val interactionSource = remember { MutableInteractionSource() }

                            IcalEntryListItem(
                                icalEntry = note,
                                isFirst = draggingNoteId != null || (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,   // first and last are only used for list, not for the staggered grid
                                isLast = draggingNoteId != null || (state.listLayout == ListLayout.LIST && index == dragAndDropList.lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
                                isSelected = state.multiselectItems?.contains(note.id) == true,
                                onClick = {
                                    if (state.multiselectItems == null)
                                        onAction(IcalEntryListAction.OnIcalEntryClicked(note.id))
                                    else
                                        onAction(IcalEntryListAction.OnToggleMultiselectItem(note.id))
                                },
                                onLongClick = { onAction(IcalEntryListAction.OnToggleMultiselectItem(note.id)) },
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

                if(LIST_COLLAPSED_GROUP_TRASHBIN in state.listCollapsedGroups) {
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

