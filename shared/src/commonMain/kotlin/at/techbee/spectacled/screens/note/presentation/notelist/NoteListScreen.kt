package at.techbee.spectacled.screens.note.presentation.notelist

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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.Route.NoteDetails
import at.techbee.spectacled.screens.core.presentation.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.ColorSelectorElement
import at.techbee.spectacled.screens.core.presentation.CustomBottomSnackbarHost
import at.techbee.spectacled.screens.note.domain.Note
import at.techbee.spectacled.screens.note.presentation.notelist.components.DeleteSelectedItemsDialog
import at.techbee.spectacled.screens.note.presentation.notelist.components.EmptyListScreen
import at.techbee.spectacled.screens.note.presentation.notelist.components.NoteDragHandle
import at.techbee.spectacled.screens.note.presentation.notelist.components.NoteListItem
import at.techbee.spectacled.screens.note.presentation.notelist.components.NoteListTopBar
import at.techbee.spectacled.screens.note.presentation.notelist.datastructures.ListGrouping
import at.techbee.spectacled.screens.note.presentation.notelist.datastructures.ListLayout
import at.techbee.spectacled.screens.note.presentation.notelist.datastructures.ListSortedBy
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces
import at.techbee.spectacled.theme.getThemeForColoredSurfaces
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_note
import spectacled.shared.generated.resources.collapse
import spectacled.shared.generated.resources.expand
import spectacled.shared.generated.resources.read_only
import spectacled.shared.generated.resources.search_in_notes
import spectacled.shared.generated.resources.trashbin
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState
import spectacled.shared.generated.resources.category


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreenRoot(
    noteListViewModel: NoteListViewModel,
    onNavigate: (Route) -> Unit,
    onNavigateUp: () -> Unit
) {

    //val drawerState = rememberDrawerState(DrawerValue.Closed)
    val state = noteListViewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    val customColors = getThemeForColoredSurfaces(state.calendar.color)
    val contentColor = getContentColorForColoredSurfaces(state.calendar.color)

    LaunchedEffect(state.snackbarText) {
        state.snackbarText?.let { message ->
            snackbarHostState.showSnackbar(message)
            noteListViewModel.onAction(NoteListAction.OnUpdateSnackbar(null))
        }
    }

    LaunchedEffect(state.navigateUp) {
        if(state.navigateUp) {
            onNavigateUp()
            noteListViewModel.onAction(NoteListAction.OnNavigateUp(false))
        }
    }

    if(state.showDeleteSelectedItemsDialog && state.multiselectItems?.isNotEmpty() == true) {
        DeleteSelectedItemsDialog(
            multiselectItems = state.multiselectItems,
            onConfirm = { noteListViewModel.onAction(NoteListAction.OnDeleteSelectedItems) },
            onDismiss = { noteListViewModel.onAction(NoteListAction.OnShowDeleteSelectedItemsDialog(false)) }
        )
    }

    if(state.showUpdateColorOfSelectedBottomSheet) {
        BottomSheetWithMenu(
            onDismiss = { noteListViewModel.onAction(NoteListAction.OnShowUpdateColorOfSelectedBottomSheet(false)) },
        ) {
            ColorSelectorElement(
                recentColors = state.notes
                    .mapNotNull { it.color }
                    .distinct(),
                preselectedColor = state.notes
                    .filter { note -> state.multiselectItems?.contains(note.id) == true }
                    .map { it.color }
                    .distinct()
                    .let { colorList -> if(colorList.size == 1) colorList.first() else Color.Transparent },
                onColorChanged = { noteListViewModel.onAction(NoteListAction.OnUpdateColorOfSelected(it)) },
                skipPartialSelection = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    CompositionLocalProvider(LocalContentColor provides customColors.onSurface) {
        MaterialTheme(colorScheme = customColors) {

            Scaffold(
                topBar = {
                    NoteListTopBar(
                        calendar = state.calendar,
                        //drawerState = drawerState,
                        isSearchBarExpanded = state.isSearchBarExpanded,
                        listSortedBy = state.listSortedBy,
                        sortedAscending = state.listSortedByAscending,
                        listLayout = state.listLayout,
                        multiselectItems = state.multiselectItems,
                        onAction = { action -> noteListViewModel.onAction(action) },
                        onSurfaceTint = contentColor,
                        modifier = Modifier.fillMaxWidth(1f)
                    )
                },
                floatingActionButton = {
                    // only shown if multiselect is null
                    if(state.multiselectItems == null) {
                        if (state.calendar.canWriteContent()) {
                            ExtendedFloatingActionButton(
                                onClick = { onNavigate(Route.AddNote(state.calendar.id)) },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                                        contentDescription = stringResource(Res.string.add_note),
                                        tint = getContentColorForColoredSurfaces(noteListViewModel.state.calendar.color, true)
                                    )
                                    Text(
                                        text = "Add note",
                                        color = getContentColorForColoredSurfaces(noteListViewModel.state.calendar.color, true)
                                    )
                                }
                            }
                        } else {
                            ExtendedFloatingActionButton(
                                onClick = { noteListViewModel.onAction(NoteListAction.OnUpdateSnackbar("No write access to this collection")) },
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
                                        tint = getContentColorForColoredSurfaces(noteListViewModel.state.calendar.color, true)
                                    )
                                    Text(
                                        text = stringResource(Res.string.read_only),
                                        color = getContentColorForColoredSurfaces(noteListViewModel.state.calendar.color, true)
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

                    NoteListScreen(
                        state = state,
                        dragAndDropList = noteListViewModel.dragAndDropList,
                        onAction = { action ->
                            when (action) {
                                is NoteListAction.OnNoteClicked -> onNavigate(NoteDetails(action.id))
                                else -> noteListViewModel.onAction(action)
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
fun NoteListScreen(
    state: NoteListState,
    dragAndDropList: List<Note>,
    onAction: (NoteListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    val searchBarFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val hapticFeedback = LocalHapticFeedback.current

    val lazyStaggeredGridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
    val reorderableLazyListState = rememberReorderableLazyStaggeredGridState(lazyStaggeredGridState) { from, to ->
        onAction(NoteListAction.OnUpdateOrderNo(from.index, to.index))
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
                placeholder = { Text(stringResource(Res.string.search_in_notes)) },
                value = state.searchQuery,
                onValueChange = { onAction(NoteListAction.OnSearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .focusRequester(searchBarFocusRequester)
            )
        }

        val allCategories = state.notes.flatMap { it.categories }.distinct()

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
                                onAction(NoteListAction.OnCategoryFilterChanged(""))
                            else
                                onAction(NoteListAction.OnCategoryFilterChanged(category))
                        },
                        label = { Text(category) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }


        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(NoteListAction.OnTriggerSync)  },
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
                                onAction(NoteListAction.OnPersistOrderNo)
                                draggingNoteId = null
                            }

                            val interactionSource = remember { MutableInteractionSource() }

                            NoteListItem(
                                note = note,
                                isFirst = draggingNoteId != null || (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,   // first and last are only used for list, not for the staggered grid
                                isLast = draggingNoteId != null || (state.listLayout == ListLayout.LIST && index == dragAndDropList.lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
                                isSelected = state.multiselectItems?.contains(note.id) == true,
                                onClick = {
                                    if (state.multiselectItems == null)
                                        onAction(NoteListAction.OnNoteClicked(note.id))
                                    else
                                        onAction(NoteListAction.OnToggleMultiselectItem(note.id))
                                },
                                onLongClick = { onAction(NoteListAction.OnToggleMultiselectItem(note.id)) },
                                interactionSource = interactionSource,
                                dragHandle = {
                                    if (state.listSortedBy == ListSortedBy.DRAGANDDROP)
                                        NoteDragHandle(this)
                                },
                                modifier = Modifier
                                    .widthIn(max = 700.dp)
                                    .heightIn(min = 50.dp)
                                    .animateItem()
                            )
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
                                    TextButton(
                                        onClick = {
                                            onAction(NoteListAction.OnToggleListGroupingExpanded(grouping))
                                        },
                                        colors = ButtonDefaults.textButtonColors().copy(contentColor = LocalContentColor.current),
                                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text =
                                                    if (grouping.stringResParam != null)
                                                        stringResource(grouping.stringRes, grouping.stringResParam)
                                                    else
                                                        stringResource(grouping.stringRes),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Crossfade(grouping in state.listExpandedSections) {
                                                if (!it)
                                                    Icon(Icons.Outlined.ExpandLess, stringResource(Res.string.expand))
                                                else
                                                    Icon(Icons.Outlined.ExpandMore, stringResource(Res.string.collapse))
                                            }
                                        }
                                    }
                                }
                            }

                            if (grouping in state.listExpandedSections) {
                                itemsIndexed(state.displayMap[grouping]!!, key = { _, note -> note.uid }) { index, note ->

                                        NoteListItem(
                                            note = note,
                                            isFirst = (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,   // first and last are only used for list, not for the staggered grid
                                            isLast = (state.listLayout == ListLayout.LIST && index == state.displayMap[grouping]!!.lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
                                            isSelected = state.multiselectItems?.contains(note.id) == true,
                                            onClick = {
                                                if (state.multiselectItems == null)
                                                    onAction(NoteListAction.OnNoteClicked(note.id))
                                                else
                                                    onAction(NoteListAction.OnToggleMultiselectItem(note.id))
                                            },
                                            onLongClick = { onAction(NoteListAction.OnToggleMultiselectItem(note.id)) },
                                            modifier = Modifier
                                                .widthIn(max = 700.dp)
                                                .heightIn(min = 50.dp)
                                                .animateItem()
                                        )

                                }
                            }
                        }
                }


                // TRASHBIN
                if(state.trashbin.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {

                        Row(
                            modifier = Modifier
                                .padding(top = 16.dp, bottom = 8.dp)
                                .fillMaxWidth()
                                .alpha(0.33f)
                        ) {

                            TextButton(
                                onClick = { onAction(NoteListAction.OnToggleListTrashbinExpanded) },
                                colors = ButtonDefaults.textButtonColors().copy(contentColor = LocalContentColor.current)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Icon(Icons.Outlined.DeleteOutline, null)
                                    Text(
                                        text = stringResource(Res.string.trashbin) + " (${state.trashbin.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Crossfade(state.listTrashbinExpanded) {
                                        if (!it)
                                            Icon(Icons.Outlined.ExpandLess, stringResource(Res.string.expand))
                                        else
                                            Icon(Icons.Outlined.ExpandMore, stringResource(Res.string.collapse))
                                    }

                                }
                            }
                        }
                    }
                }

                if(state.listTrashbinExpanded) {
                    if (state.trashbin.isEmpty())
                        item { Text(
                            text = "Nothing here",
                            fontStyle = FontStyle.Italic
                        ) }
                    else
                        itemsIndexed(state.trashbin, key = { _, note -> note.uid }) { index, note ->

                            NoteListItem(
                                note = note,
                                isFirst = (state.listLayout == ListLayout.LIST && index == 0) || state.listLayout == ListLayout.STAGGERED_GRID,   // first and last are only used for list, not for the staggered grid
                                isLast = (state.listLayout == ListLayout.LIST && index == state.trashbin.lastIndex) || state.listLayout == ListLayout.STAGGERED_GRID,
                                isSelected = state.multiselectItems?.contains(note.id) == true,
                                onClick = {
                                    if (state.multiselectItems == null)
                                        onAction(NoteListAction.OnNoteClicked(note.id))
                                    else
                                        onAction(NoteListAction.OnToggleMultiselectItem(note.id))
                                },
                                onLongClick = { onAction(NoteListAction.OnToggleMultiselectItem(note.id)) },
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
                        isEmptyFolder = state.notes.isEmpty(),
                        modifier = Modifier.fillMaxSize()
                    )
            }
        }
    }
}

@Preview
@Composable
private fun NoteListScreenRoot_Preview() {
    NoteListScreenRoot(
        noteListViewModel = koinViewModel<NoteListViewModel>(),
        onNavigate = { },
        onNavigateUp = { }
    )
}


@Preview
@Composable
private fun NoteListScreen_Preview() {

    var state = NoteListState()
    state = state.copy(
        searchQuery = "test",
        isSearchBarExpanded = true
    )

    NoteListScreen(
        state = state,
        dragAndDropList = emptyList(),
        onAction = {}
    )
}

@Preview
@Composable
private fun NoteListScreen_empty_Preview() {

    NoteListScreen(
        state = NoteListState(),
        dragAndDropList = emptyList(),
        onAction = {}
    )
}

