package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.list.presentation.ListAction
import at.techbee.spectacled.screens.list.presentation.ListState
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import at.techbee.spectacled.theme.AppTheme
import com.materialkolor.dynamicColorScheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.clear_selection
import spectacled.shared.generated.resources.date_selector
import spectacled.shared.generated.resources.delete_selected
import spectacled.shared.generated.resources.folders
import spectacled.shared.generated.resources.ic_gotodate
import spectacled.shared.generated.resources.ic_pin
import spectacled.shared.generated.resources.ic_unpin
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.pin
import spectacled.shared.generated.resources.refresh
import spectacled.shared.generated.resources.search
import spectacled.shared.generated.resources.select_all
import spectacled.shared.generated.resources.select_multiple
import spectacled.shared.generated.resources.sort_ascending
import spectacled.shared.generated.resources.sort_descending
import spectacled.shared.generated.resources.unpin
import spectacled.shared.generated.resources.update_category
import spectacled.shared.generated.resources.update_color
import spectacled.shared.generated.resources.x_selected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcalEntryListTopBar(
    state: ListState,
    onAction: (ListAction) -> Unit,
    allSelectedPinned: Boolean,
    modifier: Modifier = Modifier,
    spectacledVariant: SpectacledVariant = koinInject<SpectacledVariant>()
) {

    var sortedByDropdownExpanded by remember { mutableStateOf(false) }
    var multiselectMoreDropdownExpanded by remember { mutableStateOf(false) }

    val calendar = state.calendar

    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {

            Crossfade(state.multiselectItems != null) { multiselectEnabled ->

                Row {
                    if (!multiselectEnabled) {
                        TextButton(
                            onClick = {
                                onAction(ListAction.OnNavigateUp(true))
                            },
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChevronLeft,
                                    contentDescription = stringResource(Res.string.folders)
                                )
                                Text(
                                    text = calendar.displayName?:calendar.url.toString(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp).padding(end = 4.dp)
                                )

                                AnimatedVisibility(calendar.calendarSyncStatus?.type == CalendarSyncStatusType.IN_PROGRESS) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    } else {
                        TextButton(
                            onClick = { onAction(ListAction.OnClearMultiselectItems) },
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(Res.string.clear_selection)
                                )
                                Text(
                                    text = stringResource(Res.string.x_selected, state.multiselectItems?.size ?: 0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                )
                            }
                        }



                        VerticalDivider(modifier = Modifier.padding(vertical = 8.dp).heightIn(max = 30.dp))

                        TextButton(
                            onClick = { onAction(ListAction.OnSelectAllMultiselectItems) },
                        ) {
                            Text(
                                text = stringResource(Res.string.select_all),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 120.dp)
                            )
                        }
                    }
                }
            }
        },
        actions = {

            AnimatedVisibility(state.multiselectItems == null) {

                Row {
                    TextButton(
                        onClick = {
                            if (state.isSearchBarExpanded) {
                                onAction(ListAction.OnListFilterCriteriaChanged(state.listFilterCriteria.cleared()))
                            } else {
                                onAction(ListAction.OnListFilterCriteriaChanged(state.listFilterCriteria.copy(searchQuery = "")))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (state.isSearchBarExpanded) Icons.Outlined.SearchOff else Icons.Outlined.Search,
                            contentDescription = stringResource(Res.string.search)
                        )
                    }

                    // No sorting option for Journals
                    if(spectacledVariant != SpectacledVariant.JOURNALS) {
                        TextButton(
                            onClick = { sortedByDropdownExpanded = !sortedByDropdownExpanded }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = stringResource(state.listSortedBy.displayName)
                            )

                            DropdownMenu(
                                expanded = sortedByDropdownExpanded,
                                onDismissRequest = { sortedByDropdownExpanded = false }
                            ) {
                                ListSortedBy.entriesFor(spectacledVariant).forEach { sortedByOption ->

                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(sortedByOption.displayName))
                                        },
                                        onClick = {
                                            // toggle ascending if the same item is selected again
                                            if (state.listSortedBy.name == sortedByOption.name && state.listSortedBy != ListSortedBy.DRAGANDDROP)
                                                onAction(ListAction.OnSortedByChanged(sortedByOption, !state.listSortedByAscending))
                                            else
                                                onAction(ListAction.OnSortedByChanged(sortedByOption, true))
                                        },
                                        trailingIcon = {
                                            if (state.listSortedBy.name == sortedByOption.name && state.listSortedByAscending)
                                                Icon(
                                                    imageVector = Icons.Default.ArrowCircleDown,
                                                    contentDescription = stringResource(Res.string.sort_ascending)
                                                )
                                            else if (state.listSortedBy.name == sortedByOption.name)
                                                Icon(
                                                    imageVector = Icons.Outlined.ArrowCircleUp,
                                                    contentDescription = stringResource(Res.string.sort_descending)
                                                )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // No staggered grid option for Journals
                    if(spectacledVariant == SpectacledVariant.NOTES) {
                        TextButton(
                            onClick = {
                                onAction(
                                    ListAction.OnViewModeChanged(
                                        when (state.listLayout) {
                                            ListLayout.LIST -> ListLayout.STAGGERED_GRID
                                            ListLayout.STAGGERED_GRID -> ListLayout.LIST
                                        }
                                    )
                                )
                            }
                        ) {
                            Icon(
                                imageVector = state.listLayout.displayIcon,
                                contentDescription = stringResource(state.listLayout.displayName)
                            )
                        }
                    }

                    if (spectacledVariant == SpectacledVariant.JOURNALS) {
                        TextButton(
                            onClick = { onAction(ListAction.OnShowDateSelectorBottomSheet(true)) }
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_gotodate),
                                contentDescription = stringResource(Res.string.date_selector)
                            )
                        }
                    }

                    if (getPlatform().platform == Platforms.DESKTOP || getPlatform().platform == Platforms.WASM) {

                        TextButton(
                            onClick = { onAction(ListAction.OnToggleMultiselectItem(null)) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = stringResource(Res.string.select_multiple)
                            )
                        }

                        TextButton(
                            onClick = { onAction(ListAction.OnTriggerSync) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(Res.string.refresh)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(state.multiselectItems != null) {
                Row {

                    TextButton(
                        onClick = { onAction(ListAction.OnTogglePinEntry(!allSelectedPinned)) },
                        enabled = state.multiselectItems?.isNotEmpty() == true
                    ) {
                        if(allSelectedPinned)
                            Icon(
                                painter = painterResource(Res.drawable.ic_unpin),
                                contentDescription = stringResource(Res.string.unpin)
                            )
                        else
                            Icon(
                                painter = painterResource(Res.drawable.ic_pin),
                                contentDescription = stringResource(Res.string.pin)
                            )
                    }

                    TextButton(
                        onClick = { multiselectMoreDropdownExpanded = !multiselectMoreDropdownExpanded },
                        enabled = state.multiselectItems?.isNotEmpty() == true
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(Res.string.more)
                        )
                    }

                    DropdownMenu(
                        expanded = multiselectMoreDropdownExpanded,
                        onDismissRequest = { multiselectMoreDropdownExpanded = false }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(Res.string.update_color)
                                )
                            },
                            enabled = state.multiselectItems?.isNotEmpty() == true,
                            onClick = { onAction(ListAction.OnShowUpdateColorOfSelectedBottomSheet(true)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = stringResource(Res.string.update_color)
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(Res.string.update_category))
                            },
                            enabled = state.multiselectItems?.isNotEmpty() == true,
                            onClick = { onAction(ListAction.OnShowUpdateCategoryOfSelectedBottomSheet(true)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Label,
                                    contentDescription = stringResource(Res.string.update_category)
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(8.dp))

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(Res.string.delete_selected)
                                )
                            },
                            enabled = state.multiselectItems?.isNotEmpty() == true,
                            onClick = { onAction(ListAction.OnShowDeleteSelectedItemsDialog(true))  },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(Res.string.delete_selected)
                                )
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}


@Preview
@Composable
private fun IcalEntrySearchBar_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {

        IcalEntryListTopBar(
            state = ListState(
                calendar = Calendar.getCalendarForPreview().copy(displayName = "Personal Notes"),
                listSortedBy = ListSortedBy.CREATED,
                listSortedByAscending = true,
                listLayout = ListLayout.LIST,
                multiselectItems = null,
                listFilterCriteria = ListFilterCriteria()
            ),
            onAction = {},
            spectacledVariant = SpectacledVariant.NOTES,
            allSelectedPinned = false

        )
    }
}

@Preview
@Composable
private fun IcalEntrySearchBar_blue_Preview() {
        MaterialTheme(colorScheme = dynamicColorScheme(Color.Blue, false)) {

            IcalEntryListTopBar(
                state = ListState(
                    calendar = Calendar.getCalendarForPreview().copy(displayName = "This is a very long folder name that shouldn't cause troubles"),
                    listSortedBy = ListSortedBy.CREATED,
                    listSortedByAscending = true,
                    listLayout = ListLayout.STAGGERED_GRID,
                    multiselectItems = null,
                    listFilterCriteria = ListFilterCriteria(searchQuery = "")
                    ),
                onAction = {},
                spectacledVariant = SpectacledVariant.NOTES,
                allSelectedPinned = false
            )
        }
}


@Preview
@Composable
private fun IcalEntrySearchBar_Multiselect_Preview() {
        AppTheme(spectacledVariant = SpectacledVariant.NOTES) {

            IcalEntryListTopBar(
                state = ListState(
                    calendar = Calendar.getCalendarForPreview().copy(displayName = "Personal Notes"),
                    listSortedBy = ListSortedBy.CREATED,
                    listSortedByAscending = true,
                    listLayout = ListLayout.LIST,
                    multiselectItems = listOf(1, 2, 3),
                    listFilterCriteria = ListFilterCriteria()
                ),
                onAction = {},
                spectacledVariant = SpectacledVariant.NOTES,
                allSelectedPinned = false
            )
        }
}

@Preview
@Composable
private fun IcalEntrySearchBar_yellow_Multiselect_Preview() {

        MaterialTheme(colorScheme = dynamicColorScheme(Color.Yellow, false)) {

            IcalEntryListTopBar(
                state = ListState(
                    calendar = Calendar.getCalendarForPreview().copy(displayName = "This is a very long folder name that shouldn't cause troubles"),
                    listSortedBy = ListSortedBy.CREATED,
                    listSortedByAscending = true,
                    listLayout = ListLayout.STAGGERED_GRID,
                    multiselectItems = emptyList(),
                    listFilterCriteria = ListFilterCriteria(searchQuery = "")
                ),
                onAction = {},
                spectacledVariant = SpectacledVariant.NOTES,
                allSelectedPinned = true
            )
        }
}


@Preview
@Composable
private fun IcalEntrySearchBar_sync_in_progress_Preview() {
        MaterialTheme {

        IcalEntryListTopBar(
            state = ListState(
                calendar = Calendar.getCalendarForPreview().copy(
                    displayName = "This is a very long folder name that shouldn't cause troubles",
                    calendarSyncStatus = CalendarSyncStatus(CalendarSyncStatusType.IN_PROGRESS)
                ),
                listSortedBy = ListSortedBy.CREATED,
                listSortedByAscending = true,
                listLayout = ListLayout.STAGGERED_GRID,
                multiselectItems = null,
                listFilterCriteria = ListFilterCriteria()
            ),
            onAction = {},
            spectacledVariant = SpectacledVariant.NOTES,
            allSelectedPinned = false
        )
    }
}

