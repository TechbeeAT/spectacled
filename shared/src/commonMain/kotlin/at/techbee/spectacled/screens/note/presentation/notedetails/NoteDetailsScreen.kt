package at.techbee.spectacled.screens.note.presentation.notedetails

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
import at.techbee.spectacled.screens.note.domain.Note
import at.techbee.spectacled.screens.note.domain.SyncState
import at.techbee.spectacled.screens.note.presentation.MarkdownVisualTransformation
import at.techbee.spectacled.screens.note.presentation.notedetails.components.DatePickerBottomSheet
import at.techbee.spectacled.screens.note.presentation.notedetails.components.DeleteNoteDialog
import at.techbee.spectacled.screens.note.presentation.notedetails.components.NoteDetailsCategorySelectionBottomSheet
import at.techbee.spectacled.screens.note.presentation.notedetails.components.NoteDetailsMoreBottomSheet
import at.techbee.spectacled.screens.note.presentation.notedetails.components.NoteDetailsTopBar
import at.techbee.spectacled.screens.note.presentation.notedetails.components.ResolveSyncConflictDialog
import at.techbee.spectacled.screens.note.presentation.notedetails.components.TimePickerBottomSheet
import at.techbee.spectacled.screens.note.presentation.notedetails.components.TimeZonePickerBottomSheet
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces
import at.techbee.spectacled.theme.getThemeForColoredSurfaces
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_time
import spectacled.shared.generated.resources.color
import spectacled.shared.generated.resources.description
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.no_timezone
import spectacled.shared.generated.resources.restore
import spectacled.shared.generated.resources.summary
import spectacled.shared.generated.resources.category
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

import kotlin.time.ExperimentalTime


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun NoteDetailsScreenRoot(
    noteDetailsViewModel: NoteDetailsViewModel,
    onNavigateUp: () -> Unit
) {
    val detailsState = noteDetailsViewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    val customColors = getThemeForColoredSurfaces(detailsState.note.color)
    val iconTint = getContentColorForColoredSurfaces(detailsState.note.color)


    LaunchedEffect(detailsState.snackbarText) {
        detailsState.snackbarText?.let { message ->
            snackbarHostState.showSnackbar(message)
            noteDetailsViewModel.onAction(NoteDetailsAction.OnUpdateSnackbar(null))
        }
    }

    LaunchedEffect(detailsState.navigateUp) {
        if (detailsState.navigateUp) {
            onNavigateUp()
            noteDetailsViewModel.onAction(NoteDetailsAction.OnNavigateUp(false))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            noteDetailsViewModel.onAction(NoteDetailsAction.OnDispose)
        }
    }

    if (detailsState.showCategorySelectorBottomSheet) {
        NoteDetailsCategorySelectionBottomSheet(
            allCategories = noteDetailsViewModel.allCategories.toList(),
            initiallySelectedCategories = detailsState.note.categories,
            onCategoriesChanged = { noteDetailsViewModel.onAction(NoteDetailsAction.OnUpdateCategories(it)) },
            onDismiss = { noteDetailsViewModel.onAction(NoteDetailsAction.OnShowCategorySelectorBottomSheet(false)) }
        )
    }

    if (detailsState.showColorSelectorBottomSheet) {
        BottomSheetWithMenu(
            onDismiss = { noteDetailsViewModel.onAction(NoteDetailsAction.OnShowColorSelectorBottomSheet(false)) },
        ) {
            ColorSelectorElement(
                recentColors = noteDetailsViewModel.allColors.toList(),
                preselectedColor = detailsState.note.color ?: Color.Transparent,
                onColorChanged = { noteDetailsViewModel.onAction(NoteDetailsAction.OnUpdateColor(if (it == Color.Transparent) null else it)) },
                skipPartialSelection = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (detailsState.showMoreBottomSheet) {
        NoteDetailsMoreBottomSheet(
            onAction = { action -> noteDetailsViewModel.onAction(action) },
            note = detailsState.note,
            canWriteContent = detailsState.allowEditing()
        )
    }

    if (detailsState.showDatePickerBottomSheet) {
        DatePickerBottomSheet(
            icsDateTime = detailsState.note.dtStart ?: IcsDateTime.now(),
            sheetState = rememberModalBottomSheetState(),
            onDateSelected = { noteDetailsViewModel.onAction(NoteDetailsAction.OnUpdateDtStart(it)) },
            onDismiss = { noteDetailsViewModel.onAction(NoteDetailsAction.OnShowDatePickerBottomSheet(false)) }
        )
    }

    if (detailsState.showTimePickerBottomSheet && detailsState.note.dtStart != null) {
        TimePickerBottomSheet(
            icsDateTime = detailsState.note.dtStart,
            sheetState = rememberModalBottomSheetState(),
            onTimeUpdated = { noteDetailsViewModel.onAction(NoteDetailsAction.OnUpdateDtStart(it)) },
            onDismiss = { noteDetailsViewModel.onAction(NoteDetailsAction.OnShowTimePickerBottomSheet(false)) }
        )
    }

    if (detailsState.showTimezonePickerBottomSheet && detailsState.note.dtStart != null) {
        TimeZonePickerBottomSheet(
            icsDateTime = detailsState.note.dtStart,
            sheetState = rememberModalBottomSheetState(),
            onTimeZoneUpdated = { noteDetailsViewModel.onAction(NoteDetailsAction.OnUpdateDtStart(it)) },
            onDismiss = { noteDetailsViewModel.onAction(NoteDetailsAction.OnShowTimezonePickerBottomSheet(false)) }
        )
    }

    if (detailsState.showDeleteDialog) {
        //val message = stringResource(Res.string.note_deleted)
        DeleteNoteDialog(
            note = detailsState.note,
            onConfirm = {
                noteDetailsViewModel.onAction(NoteDetailsAction.OnDeleteNote)
            },
            onDismiss = {
                noteDetailsViewModel.showDeleteDialog(false)
            }
        )
    }

    if(detailsState.note.syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED
        || detailsState.note.syncState == SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED
        || detailsState.note.syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED) {
        ResolveSyncConflictDialog(
            syncState = detailsState.note.syncState,
            onKeepLocalChanges = { noteDetailsViewModel.onAction(NoteDetailsAction.OnSyncConflictUpdateUserDecision(SyncState.USER_DECIDED_CLIENT_WINS)) },
            onLoadServerChanges = { noteDetailsViewModel.onAction(NoteDetailsAction.OnSyncConflictUpdateUserDecision(SyncState.USER_DECIDED_SERVER_WINS)) },
            onDeleteEntry = { noteDetailsViewModel.onAction(NoteDetailsAction.OnSyncConflictUpdateUserDecision(SyncState.REMOTE_DELETED_LOCAL_TRASHBIN)) }
        )
    }

    CompositionLocalProvider(LocalContentColor provides customColors.onSurface) {
        MaterialTheme(colorScheme = customColors) {

            Scaffold(
                topBar = {
                    NoteDetailsTopBar(
                        canWriteContent = detailsState.allowEditing(),
                        contentColor = iconTint,
                        isLoading = detailsState.isLoading,
                        onAction = { action -> noteDetailsViewModel.onAction(action) }
                    )
                },
                bottomBar = {
                    BottomAppBar {
                        TextButton(
                            onClick = { noteDetailsViewModel.onAction(NoteDetailsAction.OnShowColorSelectorBottomSheet(true)) },
                            enabled = detailsState.allowEditing() && !detailsState.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = stringResource(Res.string.color),
                                tint = if(detailsState.allowEditing() && !detailsState.isLoading) iconTint else LocalContentColor.current
                            )
                        }

                        TextButton(
                            onClick = { noteDetailsViewModel.onAction(NoteDetailsAction.OnShowCategorySelectorBottomSheet(true)) },
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
                            onClick = { noteDetailsViewModel.onAction(NoteDetailsAction.OnShowMoreBottomSheet(true)) }
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
                            onClick = { noteDetailsViewModel.onAction(NoteDetailsAction.OnRestoreEntry) }
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
                    NoteDetailsScreen(
                        state = detailsState,
                        onAction = { action -> noteDetailsViewModel.onAction(action) },
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
fun NoteDetailsScreen(
    state: NoteDetailsState,
    onAction: (NoteDetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var summaryIsFocused by rememberSaveable { mutableStateOf(false) }
    var descriptionIsFocused by rememberSaveable { mutableStateOf(false) }


    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {

        state.note.dtStart?.let { dtStart ->

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                AssistChip(
                    onClick = { onAction(NoteDetailsAction.OnShowDatePickerBottomSheet(true)) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) },
                    label = { Text(PlatformInstantFormatter(dtStart).formatLocalizedDate()) },
                    colors = AssistChipDefaults.assistChipColors()
                        .copy(leadingIconContentColor = state.note.color ?: MaterialTheme.colorScheme.primary)
                )

                AssistChip(
                    onClick = { onAction(NoteDetailsAction.OnShowTimePickerBottomSheet(true)) },
                    leadingIcon = {
                        if (!dtStart.isDateOnly)
                            Icon(Icons.Outlined.Schedule, null)
                    },
                    label = {
                        if(dtStart.isDateOnly)
                            Icon(
                                imageVector = Icons.Outlined.MoreTime,
                                contentDescription = stringResource(Res.string.add_time),
                                tint = state.note.color ?: MaterialTheme.colorScheme.primary
                            )
                        else
                            Text(PlatformInstantFormatter(dtStart).formatLocalizedTime())
                    },
                    colors = AssistChipDefaults.assistChipColors()
                        .copy(leadingIconContentColor = state.note.color ?: MaterialTheme.colorScheme.primary)
                )

                AnimatedVisibility(!dtStart.isDateOnly) {
                    AssistChip(
                        onClick = { onAction(NoteDetailsAction.OnShowTimezonePickerBottomSheet(true)) },
                        leadingIcon = {
                            if(dtStart.timeZone != null)
                                Icon(Icons.Outlined.Language, null)
                        },
                        label = {
                            if (dtStart.timeZone == null) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = stringResource(Res.string.no_timezone),
                                    tint = state.note.color ?: MaterialTheme.colorScheme.primary
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
                            .copy(leadingIconContentColor = state.note.color ?: MaterialTheme.colorScheme.primary)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }

        AnimatedVisibility(state.note.categories.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                state.note.categories.sorted().forEach { category ->

                    Badge(
                        containerColor = state.note.color ?: MaterialTheme.colorScheme.primary,
                        contentColor = state.note.color?.let { getContentColorForColoredSurfaces(state.note.color) }
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
            value = if (!summaryIsFocused && state.note.summary.isNullOrEmpty()) stringResource(Res.string.summary) else state.note.summary
                ?: "",
            onValueChange = {
                onAction(NoteDetailsAction.OnUpdateSummary(it))
            },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = if (!summaryIsFocused && state.note.summary.isNullOrEmpty()) LocalContentColor.current.copy(alpha = 0.5f) else LocalContentColor.current
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
            value = if (!descriptionIsFocused && state.note.description.isNullOrEmpty()) stringResource(Res.string.description) else state.note.description
                ?: "",
            onValueChange = {
                onAction(NoteDetailsAction.OnUpdateDescription(it))
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (!descriptionIsFocused && state.note.description.isNullOrEmpty()) LocalContentColor.current.copy(alpha = 0.5f) else LocalContentColor.current
            ),
            enabled = state.allowEditing(),
            visualTransformation = MarkdownVisualTransformation(LocalContentColor.current),
            modifier = Modifier
                .heightIn(min = 50.dp)
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
private fun NoteListScreenRoot_Preview() {
    NoteDetailsScreenRoot(
        noteDetailsViewModel = koinViewModel<NoteDetailsViewModel>(),
        onNavigateUp = {}
    )
}


@Preview
@Composable
private fun NoteListScreen_Preview() {
    NoteDetailsScreen(
        state = NoteDetailsState(
            note = Note.getSampleNote(),
            originalNote = Note.getSampleNote()
        ),
        onAction = {}
    )
}

@Preview
@Composable
private fun NoteListScreen_with_dtstart_Preview() {
    NoteDetailsScreen(
        state = NoteDetailsState(
            note = Note.getSampleNote().copy(dtStart = IcsDateTime.now()),
            originalNote = Note.getSampleNote()
        ),
        onAction = {}
    )
}

@Preview
@Composable
private fun NoteListScreen_with_dtstart_and_timezone_Preview() {
    NoteDetailsScreen(
        state = NoteDetailsState(
            note = Note.getSampleNote().copy(dtStart = IcsDateTime.now().copy(timeZone = TimeZone.of("Europe/Vienna"))),
            originalNote = Note.getSampleNote()
        ),
        onAction = {}
    )
}


