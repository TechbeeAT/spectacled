package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.IcalEntryListAction
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListLayout
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures.ListSortedBy
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces
import at.techbee.spectacled.theme.getThemeForColoredSurfaces
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.clear_selection
import spectacled.shared.generated.resources.delete_selected
import spectacled.shared.generated.resources.folders
import spectacled.shared.generated.resources.ic_pin
import spectacled.shared.generated.resources.ic_unpin
import spectacled.shared.generated.resources.pin
import spectacled.shared.generated.resources.refresh
import spectacled.shared.generated.resources.search
import spectacled.shared.generated.resources.select_all
import spectacled.shared.generated.resources.select_multiple
import spectacled.shared.generated.resources.sort_ascending
import spectacled.shared.generated.resources.sort_descending
import spectacled.shared.generated.resources.unpin
import spectacled.shared.generated.resources.update_color
import spectacled.shared.generated.resources.x_selected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcalEntryListTopBar(
    calendar: Calendar,
    isSearchBarExpanded: Boolean,
    listSortedBy: ListSortedBy,
    sortedAscending: Boolean,
    listLayout: ListLayout,
    multiselectItems: List<Long>?,
    onSurfaceTint: Color,
    onAction: (IcalEntryListAction) -> Unit,
    allSelectedPinned: Boolean,
    modifier: Modifier = Modifier,
    spectacledVariant: SpectacledVariant = koinInject<SpectacledVariant>()
) {

    var sortedByDropdownExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {

            Crossfade(multiselectItems != null) { multiselectEnabled ->

                Row {
                    if (!multiselectEnabled) {
                        TextButton(
                            onClick = {
                                onAction(IcalEntryListAction.OnNavigateUp(true))
                            },
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChevronLeft,
                                    contentDescription = stringResource(Res.string.folders),
                                    tint = onSurfaceTint
                                )
                                Text(
                                    text = calendar.displayName?:calendar.url.toString(),
                                    color = onSurfaceTint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp).padding(end = 4.dp)
                                )

                                AnimatedVisibility(calendar.calendarSyncStatus?.type == CalendarSyncStatusType.IN_PROGRESS) {
                                    CircularProgressIndicator(
                                        color = if(onSurfaceTint == Color.Unspecified) ProgressIndicatorDefaults.circularColor else onSurfaceTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        TextButton(
                            onClick = { onAction(IcalEntryListAction.OnClearMultiselectItems) },
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(Res.string.clear_selection),
                                    tint = onSurfaceTint
                                )
                                Text(
                                    text = stringResource(Res.string.x_selected, multiselectItems?.size ?: 0),
                                    color = onSurfaceTint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                )
                            }
                        }



                        VerticalDivider(modifier = Modifier.padding(vertical = 8.dp).heightIn(max = 30.dp))

                        TextButton(
                            onClick = { onAction(IcalEntryListAction.OnSelectAllMultiselectItems) },
                        ) {
                            Text(
                                text = stringResource(Res.string.select_all),
                                color = onSurfaceTint,
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

            AnimatedVisibility(multiselectItems == null) {

                Row {
                    TextButton(
                        onClick = {
                            if (isSearchBarExpanded) {
                                onAction(IcalEntryListAction.OnSearchQueryChanged(""))
                                onAction(IcalEntryListAction.OnCategoryFilterChanged(""))
                            }
                            onAction(IcalEntryListAction.OnSearchBarExpanded(!isSearchBarExpanded))
                        }
                    ) {
                        Icon(
                            imageVector = if (isSearchBarExpanded) Icons.Outlined.SearchOff else Icons.Outlined.Search,
                            contentDescription = stringResource(Res.string.search),
                            tint = onSurfaceTint
                        )
                    }

                    TextButton(
                        onClick = { sortedByDropdownExpanded = !sortedByDropdownExpanded }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = stringResource(listSortedBy.displayName),
                            tint = onSurfaceTint
                        )

                        DropdownMenu(
                            expanded = sortedByDropdownExpanded,
                            onDismissRequest = { sortedByDropdownExpanded = false }
                        ) {
                            ListSortedBy.entriesFor(spectacledVariant).forEach { sortedByOption ->

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(sortedByOption.displayName),
                                            color = onSurfaceTint
                                        )
                                    },
                                    onClick = {
                                        // toggle ascending if the same item is selected again
                                        if (listSortedBy.name == sortedByOption.name && listSortedBy != ListSortedBy.DRAGANDDROP)
                                            onAction(IcalEntryListAction.OnSortedByChanged(sortedByOption, !sortedAscending))
                                        else
                                            onAction(IcalEntryListAction.OnSortedByChanged(sortedByOption, true))
                                    },
                                    trailingIcon = {
                                        if (listSortedBy.name == sortedByOption.name && sortedAscending)
                                            Icon(
                                                imageVector = Icons.Default.ArrowCircleDown,
                                                contentDescription = stringResource(Res.string.sort_ascending),
                                                tint = onSurfaceTint
                                            )
                                        else if (listSortedBy.name == sortedByOption.name)
                                            Icon(
                                                imageVector = Icons.Outlined.ArrowCircleUp,
                                                contentDescription = stringResource(Res.string.sort_descending),
                                                tint = onSurfaceTint
                                            )
                                    }
                                )
                            }
                        }
                    }

                    // No staggered grid option for Journals
                    if(spectacledVariant != SpectacledVariant.JOURNALS) {
                        TextButton(
                            onClick = {
                                onAction(
                                    IcalEntryListAction.OnViewModeChanged(
                                        when (listLayout) {
                                            ListLayout.LIST -> ListLayout.STAGGERED_GRID
                                            ListLayout.STAGGERED_GRID -> ListLayout.LIST
                                        }
                                    )
                                )
                            }
                        ) {
                            Icon(
                                imageVector = listLayout.displayIcon,
                                contentDescription = stringResource(listLayout.displayName),
                                tint = onSurfaceTint
                            )
                        }
                    }

                    if (getPlatform().platform == Platforms.DESKTOP || getPlatform().platform == Platforms.WASM) {

                        TextButton(
                            onClick = { onAction(IcalEntryListAction.OnToggleMultiselectItem(null)) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = stringResource(Res.string.select_multiple),
                                tint = onSurfaceTint
                            )
                        }

                        TextButton(
                            onClick = { onAction(IcalEntryListAction.OnTriggerSync) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(Res.string.refresh),
                                tint = onSurfaceTint
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(multiselectItems != null) {
                Row {
                    TextButton(
                        onClick = { onAction(IcalEntryListAction.OnShowUpdateColorOfSelectedBottomSheet(true)) },
                        enabled = multiselectItems?.isNotEmpty() == true
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = stringResource(Res.string.update_color),
                            tint = onSurfaceTint.copy(alpha = if(multiselectItems?.isNotEmpty()== true) 1f else 0.5f)
                        )
                    }

                    TextButton(
                        onClick = { onAction(IcalEntryListAction.OnShowDeleteSelectedItemsDialog(true)) },
                        enabled = multiselectItems?.isNotEmpty() == true
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(Res.string.delete_selected),
                            tint = onSurfaceTint.copy(alpha = if(multiselectItems?.isNotEmpty()== true) 1f else 0.5f)
                        )
                    }

                    TextButton(
                        onClick = { onAction(IcalEntryListAction.OnTogglePinEntry(if(allSelectedPinned) false else true)) },
                        enabled = multiselectItems?.isNotEmpty() == true
                    ) {
                        if(allSelectedPinned)
                            Icon(
                                painter = painterResource(Res.drawable.ic_unpin),
                                contentDescription = stringResource(Res.string.unpin),
                                tint = onSurfaceTint.copy(alpha = if(multiselectItems?.isNotEmpty()== true) 1f else 0.5f)
                            )
                        else
                            Icon(
                                painter = painterResource(Res.drawable.ic_pin),
                                contentDescription = stringResource(Res.string.pin),
                                tint = onSurfaceTint.copy(alpha = if(multiselectItems?.isNotEmpty()== true) 1f else 0.5f)
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
    CompositionLocalProvider(LocalContentColor provides Color.Unspecified) {
        MaterialTheme(colorScheme = getThemeForColoredSurfaces(Color.Unspecified)) {

            IcalEntryListTopBar(
                calendar = Calendar.getCalendarForPreview().copy(displayName = "Personal Notes"),
                listSortedBy = ListSortedBy.CREATED,
                sortedAscending = true,
                listLayout = ListLayout.LIST,
                isSearchBarExpanded = false,
                multiselectItems = null,
                onSurfaceTint = Color.Unspecified,
                onAction = {},
                spectacledVariant = SpectacledVariant.NOTES,
                allSelectedPinned = false

            )
        }
    }
}

@Preview
@Composable
private fun IcalEntrySearchBar_blue_Preview() {
    CompositionLocalProvider(LocalContentColor provides Color.Blue) {
        MaterialTheme(colorScheme = getThemeForColoredSurfaces(Color.Blue)) {

            IcalEntryListTopBar(
                calendar = Calendar.getCalendarForPreview().copy(displayName = "This is a very long folder name that shouldn't cause troubles"),
                listSortedBy = ListSortedBy.CREATED,
                sortedAscending = true,
                listLayout = ListLayout.STAGGERED_GRID,
                isSearchBarExpanded = true,
                multiselectItems = null,
                onSurfaceTint = getContentColorForColoredSurfaces(Color.Blue),
                onAction = {},
                spectacledVariant = SpectacledVariant.NOTES,
                allSelectedPinned = false
            )
        }
    }
}


@Preview
@Composable
private fun IcalEntrySearchBar_Multiselect_Preview() {
    CompositionLocalProvider(LocalContentColor provides Color.Unspecified) {
        MaterialTheme(colorScheme = getThemeForColoredSurfaces(Color.Unspecified)) {

            IcalEntryListTopBar(
                calendar = Calendar.getCalendarForPreview().copy(displayName = "Personal Notes"),
                //drawerState = rememberDrawerState(DrawerValue.Closed),
                listSortedBy = ListSortedBy.CREATED,
                sortedAscending = true,
                listLayout = ListLayout.LIST,
                isSearchBarExpanded = false,
                multiselectItems = listOf(1, 2, 3),
                onSurfaceTint = Color.Unspecified,
                onAction = {},
                spectacledVariant = SpectacledVariant.NOTES,
                allSelectedPinned = false
            )
        }
    }
}

@Preview
@Composable
private fun IcalEntrySearchBar_yellow_Multiselect_Preview() {
    CompositionLocalProvider(LocalContentColor provides Color.Yellow) {
        MaterialTheme(colorScheme = getThemeForColoredSurfaces(Color.Yellow)) {

            IcalEntryListTopBar(
                calendar = Calendar.getCalendarForPreview().copy(displayName = "This is a very long folder name that shouldn't cause troubles"),
                listSortedBy = ListSortedBy.CREATED,
                sortedAscending = true,
                listLayout = ListLayout.STAGGERED_GRID,
                isSearchBarExpanded = true,
                multiselectItems = emptyList(),
                onSurfaceTint = getContentColorForColoredSurfaces(Color.Yellow),
                onAction = {},
                spectacledVariant = SpectacledVariant.NOTES,
                allSelectedPinned = true
            )
        }
    }
}


@Preview
@Composable
private fun IcalEntrySearchBar_sync_in_progress_Preview() {
    CompositionLocalProvider(LocalContentColor provides Color.Unspecified) {
        MaterialTheme {

            IcalEntryListTopBar(
                calendar = Calendar.getCalendarForPreview().copy(
                    displayName = "This is a very long folder name that shouldn't cause troubles",
                    calendarSyncStatus = CalendarSyncStatus(CalendarSyncStatusType.IN_PROGRESS)
                ),
                listSortedBy = ListSortedBy.CREATED,
                sortedAscending = true,
                listLayout = ListLayout.STAGGERED_GRID,
                isSearchBarExpanded = false,
                multiselectItems = null,
                onSurfaceTint = Color.Unspecified,
                onAction = {},
                spectacledVariant = SpectacledVariant.NOTES,
                allSelectedPinned = false
            )
        }
    }
}

