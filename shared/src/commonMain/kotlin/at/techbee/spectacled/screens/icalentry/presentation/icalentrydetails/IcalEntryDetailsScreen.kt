package at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.PlatformInstantFormatter
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.presentation.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.ColorSelectorElement
import at.techbee.spectacled.screens.core.presentation.CustomBottomSnackbarHost
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import at.techbee.spectacled.screens.icalentry.domain.SyncState
import at.techbee.spectacled.screens.icalentry.presentation.MarkdownVisualTransformation
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components.DatePickerBottomSheet
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components.DeleteIcalEntryDialog
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components.IcalEntryDetailsCategorySelectionBottomSheet
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components.IcalEntryDetailsMoreBottomSheet
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components.IcalEntryDetailsTopBar
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components.ResolveSyncConflictDialog
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components.TimePickerBottomSheet
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components.TimeZonePickerBottomSheet
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces
import at.techbee.spectacled.theme.getThemeForColoredSurfaces
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_time
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.color
import spectacled.shared.generated.resources.description
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.no_timezone
import spectacled.shared.generated.resources.restore
import spectacled.shared.generated.resources.summary
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun IcalEntryDetailsScreenRoot(
    icalEntryDetailsViewModel: IcalEntryDetailsViewModel,
    onNavigateUp: () -> Unit
) {
    val detailsState = icalEntryDetailsViewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    val customColors = getThemeForColoredSurfaces(detailsState.icalEntry.color)
    val iconTint = getContentColorForColoredSurfaces(detailsState.icalEntry.color)


    LaunchedEffect(detailsState.snackbarText) {
        detailsState.snackbarText?.let { message ->
            snackbarHostState.showSnackbar(message)
            icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnUpdateSnackbar(null))
        }
    }

    LaunchedEffect(detailsState.navigateUp) {
        if (detailsState.navigateUp) {
            onNavigateUp()
            icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnNavigateUp(false))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnDispose)
        }
    }

    if (detailsState.showCategorySelectorBottomSheet) {
        IcalEntryDetailsCategorySelectionBottomSheet(
            allCategories = icalEntryDetailsViewModel.allCategories.toList(),
            initiallySelectedCategories = detailsState.icalEntry.categories,
            onCategoriesChanged = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnUpdateCategories(it)) },
            onDismiss = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnShowCategorySelectorBottomSheet(false)) }
        )
    }

    if (detailsState.showColorSelectorBottomSheet) {
        BottomSheetWithMenu(
            onDismiss = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnShowColorSelectorBottomSheet(false)) },
        ) {
            ColorSelectorElement(
                recentColors = icalEntryDetailsViewModel.allColors.toList(),
                preselectedColor = detailsState.icalEntry.color ?: Color.Transparent,
                onColorChanged = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnUpdateColor(if (it == Color.Transparent) null else it)) },
                skipPartialSelection = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (detailsState.showMoreBottomSheet) {
        IcalEntryDetailsMoreBottomSheet(
            onAction = { action -> icalEntryDetailsViewModel.onAction(action) },
            icalEntry = detailsState.icalEntry,
            canWriteContent = detailsState.allowEditing()
        )
    }

    if (detailsState.showDatePickerBottomSheet) {
        DatePickerBottomSheet(
            icsDateTime = detailsState.icalEntry.dtStart ?: IcsDateTime.now(),
            sheetState = rememberModalBottomSheetState(),
            onDateSelected = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnUpdateDtStart(it)) },
            onDismiss = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnShowDatePickerBottomSheet(false)) }
        )
    }

    if (detailsState.showTimePickerBottomSheet && detailsState.icalEntry.dtStart != null) {
        TimePickerBottomSheet(
            icsDateTime = detailsState.icalEntry.dtStart,
            sheetState = rememberModalBottomSheetState(),
            onTimeUpdated = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnUpdateDtStart(it)) },
            onDismiss = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnShowTimePickerBottomSheet(false)) }
        )
    }

    if (detailsState.showTimezonePickerBottomSheet && detailsState.icalEntry.dtStart != null) {
        TimeZonePickerBottomSheet(
            icsDateTime = detailsState.icalEntry.dtStart,
            sheetState = rememberModalBottomSheetState(),
            onTimeZoneUpdated = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnUpdateDtStart(it)) },
            onDismiss = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnShowTimezonePickerBottomSheet(false)) }
        )
    }

    if (detailsState.showDeleteDialog) {
        DeleteIcalEntryDialog(
            icalEntry = detailsState.icalEntry,
            onConfirm = {
                icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnDeleteIcalEntry)
            },
            onDismiss = {
                icalEntryDetailsViewModel.showDeleteDialog(false)
            }
        )
    }

    if(detailsState.icalEntry.syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED
        || detailsState.icalEntry.syncState == SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED
        || detailsState.icalEntry.syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED) {
        ResolveSyncConflictDialog(
            syncState = detailsState.icalEntry.syncState,
            onKeepLocalChanges = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnSyncConflictUpdateUserDecision(SyncState.USER_DECIDED_CLIENT_WINS)) },
            onLoadServerChanges = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnSyncConflictUpdateUserDecision(SyncState.USER_DECIDED_SERVER_WINS)) },
            onDeleteEntry = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnSyncConflictUpdateUserDecision(SyncState.REMOTE_DELETED_LOCAL_TRASHBIN)) }
        )
    }

    CompositionLocalProvider(LocalContentColor provides customColors.onSurface) {
        MaterialTheme(colorScheme = customColors) {

            Scaffold(
                topBar = {
                    IcalEntryDetailsTopBar(
                        canWriteContent = detailsState.allowEditing(),
                        contentColor = iconTint,
                        isLoading = detailsState.isLoading,
                        onAction = { action -> icalEntryDetailsViewModel.onAction(action) }
                    )
                },
                bottomBar = {
                    BottomAppBar {
                        TextButton(
                            onClick = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnShowColorSelectorBottomSheet(true)) },
                            enabled = detailsState.allowEditing() && !detailsState.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = stringResource(Res.string.color),
                                tint = if(detailsState.allowEditing() && !detailsState.isLoading) iconTint else LocalContentColor.current
                            )
                        }

                        TextButton(
                            onClick = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnShowCategorySelectorBottomSheet(true)) },
                            enabled = detailsState.allowEditing() && !detailsState.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Label,
                                contentDescription = stringResource(Res.string.category),
                                tint = if(detailsState.allowEditing() && !detailsState.isLoading) iconTint else LocalContentColor.current
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnShowMoreBottomSheet(true)) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(Res.string.more),
                                tint = iconTint
                            )
                        }
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(detailsState.allowRestore() && !detailsState.isLoading) {
                        ExtendedFloatingActionButton(
                            onClick = { icalEntryDetailsViewModel.onAction(IcalEntryDetailsAction.OnRestoreEntry) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Restore,
                                    contentDescription = stringResource(Res.string.restore),
                                    tint = iconTint
                                )
                                Text(
                                    text = stringResource(Res.string.restore),
                                    color = iconTint
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->

                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 0.dp)
                        .padding(paddingValues)
                        .imePadding()
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    IcalEntryDetailsScreen(
                        state = detailsState,
                        onAction = { action -> icalEntryDetailsViewModel.onAction(action) },
                        modifier = Modifier.fillMaxSize()
                    )

                    CustomBottomSnackbarHost(
                        snackbarHostState = snackbarHostState,
                        keepSpaceForFAB = false
                    )
                }
            }
        }
    }
}


@Composable
fun IcalEntryDetailsScreen(
    state: IcalEntryDetailsState,
    onAction: (IcalEntryDetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var summaryIsFocused by rememberSaveable { mutableStateOf(false) }
    var descriptionIsFocused by rememberSaveable { mutableStateOf(false) }


    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {

        state.icalEntry.dtStart?.let { dtStart ->

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                AssistChip(
                    onClick = { onAction(IcalEntryDetailsAction.OnShowDatePickerBottomSheet(true)) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) },
                    label = { Text(PlatformInstantFormatter(dtStart).formatLocalizedDate()) },
                    colors = AssistChipDefaults.assistChipColors()
                        .copy(leadingIconContentColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary)
                )

                AssistChip(
                    onClick = { onAction(IcalEntryDetailsAction.OnShowTimePickerBottomSheet(true)) },
                    leadingIcon = {
                        if (!dtStart.isDateOnly)
                            Icon(Icons.Outlined.Schedule, null)
                    },
                    label = {
                        if(dtStart.isDateOnly)
                            Icon(
                                imageVector = Icons.Outlined.MoreTime,
                                contentDescription = stringResource(Res.string.add_time),
                                tint = state.icalEntry.color ?: MaterialTheme.colorScheme.primary
                            )
                        else
                            Text(PlatformInstantFormatter(dtStart).formatLocalizedTime())
                    },
                    colors = AssistChipDefaults.assistChipColors()
                        .copy(leadingIconContentColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary)
                )

                AnimatedVisibility(!dtStart.isDateOnly) {
                    AssistChip(
                        onClick = { onAction(IcalEntryDetailsAction.OnShowTimezonePickerBottomSheet(true)) },
                        leadingIcon = {
                            if(dtStart.timeZone != null)
                                Icon(Icons.Outlined.Language, null)
                        },
                        label = {
                            if (dtStart.timeZone == null) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = stringResource(Res.string.no_timezone),
                                    tint = state.icalEntry.color ?: MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Column {
                                    Text(dtStart.timeZone.id)
                                    Text(
                                        text = PlatformInstantFormatter(dtStart.copy(timeZone = TimeZone.currentSystemDefault())).formatLocalizedDateTime() +
                                                " (${TimeZone.currentSystemDefault().id})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                            }
                        },
                        colors = AssistChipDefaults.assistChipColors()
                            .copy(leadingIconContentColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }

        AnimatedVisibility(state.icalEntry.categories.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                state.icalEntry.categories.sorted().forEach { category ->

                    Badge(
                        containerColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary,
                        contentColor = state.icalEntry.color?.let { getContentColorForColoredSurfaces(state.icalEntry.color) }
                            ?: MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        BasicTextField(
            value = if (!summaryIsFocused && state.icalEntry.summary.isNullOrEmpty()) stringResource(Res.string.summary) else state.icalEntry.summary
                ?: "",
            onValueChange = {
                onAction(IcalEntryDetailsAction.OnUpdateSummary(it))
            },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = if (!summaryIsFocused && state.icalEntry.summary.isNullOrEmpty()) LocalContentColor.current.copy(alpha = 0.5f) else LocalContentColor.current
            ),
            enabled = state.allowEditing(),
            visualTransformation = MarkdownVisualTransformation(LocalContentColor.current),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .onFocusChanged {
                    summaryIsFocused = it.isFocused
                }
        )

        BasicTextField(
            value = if (!descriptionIsFocused && state.icalEntry.description.isNullOrEmpty()) stringResource(Res.string.description) else state.icalEntry.description
                ?: "",
            onValueChange = {
                onAction(IcalEntryDetailsAction.OnUpdateDescription(it))
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (!descriptionIsFocused && state.icalEntry.description.isNullOrEmpty()) LocalContentColor.current.copy(alpha = 0.5f) else LocalContentColor.current
            ),
            enabled = state.allowEditing(),
            visualTransformation = MarkdownVisualTransformation(LocalContentColor.current),
            modifier = Modifier
                .heightIn(min = 200.dp)
                .fillMaxWidth()
                .fillMaxHeight(1f)
                .onFocusChanged {
                    descriptionIsFocused = it.isFocused
                }
        )
    }



}

@Preview
@Composable
private fun IcalEntryListScreenRoot_Preview() {
    IcalEntryDetailsScreenRoot(
        icalEntryDetailsViewModel = koinViewModel<IcalEntryDetailsViewModel>(),
        onNavigateUp = {}
    )
}


@Preview
@Composable
private fun IcalEntryListScreen_Preview() {
    IcalEntryDetailsScreen(
        state = IcalEntryDetailsState(
            icalEntry = IcalEntry.getSampleIcalEntry(),
            originalIcalEntry = IcalEntry.getSampleIcalEntry()
        ),
        onAction = {}
    )
}

@Preview
@Composable
private fun IcalEntryListScreen_with_dtstart_Preview() {
    IcalEntryDetailsScreen(
        state = IcalEntryDetailsState(
            icalEntry = IcalEntry.getSampleIcalEntry().copy(dtStart = IcsDateTime.now()),
            originalIcalEntry = IcalEntry.getSampleIcalEntry()
        ),
        onAction = {}
    )
}

@Preview
@Composable
private fun IcalEntryListScreen_with_dtstart_and_timezone_Preview() {
    IcalEntryDetailsScreen(
        state = IcalEntryDetailsState(
            icalEntry = IcalEntry.getSampleIcalEntry().copy(dtStart = IcsDateTime.now().copy(timeZone = TimeZone.of("Europe/Vienna"))),
            originalIcalEntry = IcalEntry.getSampleIcalEntry()
        ),
        onAction = {},
        modifier = Modifier.fillMaxHeight()
    )
}


