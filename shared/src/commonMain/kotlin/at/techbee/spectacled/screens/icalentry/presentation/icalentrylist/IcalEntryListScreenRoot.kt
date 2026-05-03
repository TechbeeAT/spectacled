package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.Route.IcalEntryDetails
import at.techbee.spectacled.screens.core.presentation.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.ColorSelectorElement
import at.techbee.spectacled.screens.core.presentation.CustomBottomSnackbarHost
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components.CategorySelectionBottomSheet
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.DeleteSelectedItemsDialog
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components.IcalEntryListTopBar
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces
import at.techbee.spectacled.theme.getThemeForColoredSurfaces
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_journal
import spectacled.shared.generated.resources.add_note
import spectacled.shared.generated.resources.add_task
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.ic_add_journal
import spectacled.shared.generated.resources.ic_add_note
import spectacled.shared.generated.resources.ic_add_task
import spectacled.shared.generated.resources.read_only
import spectacled.shared.generated.resources.search

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
    val pullToRefreshState = rememberPullToRefreshState()

    val customColors = getThemeForColoredSurfaces(state.calendar.color)
    val contentColor = getContentColorForColoredSurfaces(state.calendar.color)

    val searchBarFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current


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

    LaunchedEffect(state.isSearchBarExpanded) {
        if (state.isSearchBarExpanded) {
            searchBarFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            searchBarFocusRequester.freeFocus()
            keyboardController?.hide()
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
    
    if (state.showUpdateCategoryOfSelectedBottomSheet) {
        CategorySelectionBottomSheet(
            allCategories = state.allCategories.filter { it != IcalEntry.PINNED_CATEGORY },
            selectedCategories = state.multiselectItems?.let { selectedIds ->
                val selectedEntries = state.icalEntries.filter { it.id in selectedIds }
                if (selectedEntries.isEmpty()) return@let emptyList()

                selectedEntries
                    .map { it.categories.toSet() }
                    .reduce { acc, categories -> acc.intersect(categories) }
                    .filter { it != IcalEntry.PINNED_CATEGORY }
                    .toList()
            } ?: emptyList(),
            onCategoryAdded = { icalEntryListViewModel.onAction(IcalEntryListAction.OnUpdateCategoryOfSelected(it, "")) },
            onCategoryRemoved = { icalEntryListViewModel.onAction(IcalEntryListAction.OnUpdateCategoryOfSelected("", it)) },
            onDismiss = { icalEntryListViewModel.onAction(IcalEntryListAction.OnShowUpdateCategoryOfSelectedBottomSheet(false)) }
        )
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
                                        painter = painterResource(when(icalEntryListViewModel.spectacledVariant) {
                                            SpectacledVariant.JOURNALS -> Res.drawable.ic_add_journal
                                            SpectacledVariant.NOTES -> Res.drawable.ic_add_note
                                            SpectacledVariant.TASKS -> Res.drawable.ic_add_task
                                        }),
                                        contentDescription = stringResource(when(icalEntryListViewModel.spectacledVariant) {
                                            SpectacledVariant.JOURNALS -> Res.string.add_journal
                                            SpectacledVariant.NOTES -> Res.string.add_note
                                            SpectacledVariant.TASKS -> Res.string.add_task
                                        }),
                                        tint = getContentColorForColoredSurfaces(icalEntryListViewModel.state.calendar.color, true)
                                    )

                                    Text(
                                        text = stringResource(when(icalEntryListViewModel.spectacledVariant) {
                                            SpectacledVariant.JOURNALS -> Res.string.add_journal
                                            SpectacledVariant.NOTES -> Res.string.add_note
                                            SpectacledVariant.TASKS -> Res.string.add_task
                                        }),
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

                    Column(modifier = Modifier.fillMaxSize()) {

                        AnimatedVisibility(state.isSearchBarExpanded) {
                            TextField(
                                placeholder = { Text(stringResource(Res.string.search)) },
                                value = state.searchQuery,
                                onValueChange = { icalEntryListViewModel.onAction(IcalEntryListAction.OnSearchQueryChanged(it)) },
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
                                                icalEntryListViewModel.onAction(IcalEntryListAction.OnCategoryFilterChanged(""))
                                            else
                                                icalEntryListViewModel.onAction(IcalEntryListAction.OnCategoryFilterChanged(category))
                                        },
                                        label = { Text(category) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }


                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { icalEntryListViewModel.onAction(IcalEntryListAction.OnTriggerSync)  },
                            state = pullToRefreshState,
                            indicator = {
                                if(state.draggingIcalEntryId != null) {  // don't show indicator when dragging to avoid conflict
                                    return@PullToRefreshBox
                                } else {
                                    Indicator(
                                        modifier = Modifier.align(Alignment.TopCenter),
                                        isRefreshing = state.isRefreshing,
                                        state = pullToRefreshState,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {

                            if (icalEntryListViewModel.spectacledVariant == SpectacledVariant.JOURNALS) {
                                JournalsListScreen(
                                    state = state,
                                    onAction = { action ->
                                        when (action) {
                                            is IcalEntryListAction.OnIcalEntryClicked -> onNavigate(IcalEntryDetails(action.id))
                                            else -> icalEntryListViewModel.onAction(action)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
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
                            }
                        }

                    }
                    CustomBottomSnackbarHost(
                        snackbarHostState = snackbarHostState,
                        keepSpaceForFAB = true
                    )
                }
            }
        }
    }
}