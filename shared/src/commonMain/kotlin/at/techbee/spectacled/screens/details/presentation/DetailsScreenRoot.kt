package at.techbee.spectacled.screens.details.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.Route.IcalEntryDetails
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.components.ColorSelectorElement
import at.techbee.spectacled.screens.core.presentation.components.CustomBottomSnackbarHost
import at.techbee.spectacled.screens.details.presentation.components.AddSubtaskBottomSheet
import at.techbee.spectacled.screens.details.presentation.components.CategorySelectionBottomSheet
import at.techbee.spectacled.screens.details.presentation.components.DeleteIcalEntryDialog
import at.techbee.spectacled.screens.details.presentation.components.DetailsMoreBottomSheet
import at.techbee.spectacled.screens.details.presentation.components.DetailsTopBar
import at.techbee.spectacled.screens.details.presentation.components.JournalStatusPickerBottomSheet
import at.techbee.spectacled.screens.details.presentation.components.ResolveSyncConflictDialog
import at.techbee.spectacled.screens.details.presentation.components.TaskStatusProgressPickerBottomSheet
import at.techbee.spectacled.theme.getThemeForSeedColor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.color
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.restore
import spectacled.shared.generated.resources.subtask
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DetailsScreenRoot(
    detailsViewModel: DetailsViewModel,
    onNavigate: (Route) -> Unit,
    onNavigateUp: () -> Unit
) {
    val detailsState by detailsViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    MaterialTheme(colorScheme = getThemeForSeedColor(detailsState.icalEntry.color ?: detailsState.calendar?.color)) {


        LaunchedEffect(detailsState.snackbarText) {
            detailsState.snackbarText?.let { message ->
                snackbarHostState.showSnackbar(message)
                detailsViewModel.onAction(DetailsAction.OnUpdateSnackbar(null))
            }
        }

        LaunchedEffect(detailsState.navigateUp) {
            if (detailsState.navigateUp) {
                onNavigateUp()
                detailsViewModel.onAction(DetailsAction.OnNavigateUp(false))
            }
        }

        LaunchedEffect(detailsState.navigateToIcalEntryId) {
            detailsState.navigateToIcalEntryId?.let {
                onNavigate(IcalEntryDetails(it))
                detailsViewModel.onAction(DetailsAction.OnNavigateToIcalEntryId(null))
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                detailsViewModel.onAction(DetailsAction.OnDispose)
            }
        }

        if (detailsState.showCategorySelectorBottomSheet) {
            CategorySelectionBottomSheet(
                allCategories = detailsState.allCategories.filter { it != IcalEntry.PINNED_CATEGORY },
                selectedCategories = detailsState.icalEntry.categories.filter { it != IcalEntry.PINNED_CATEGORY },
                onCategoryAdded = { detailsViewModel.onAction(DetailsAction.OnUpdateCategories(it, null)) },
                onCategoryRemoved = { detailsViewModel.onAction(DetailsAction.OnUpdateCategories(null, it)) },
                onDismiss = { detailsViewModel.onAction(DetailsAction.OnShowCategorySelectorBottomSheet(false)) }
            )
        }

        if (detailsState.showColorSelectorBottomSheet) {
            BottomSheetWithMenu(
                onDismiss = { detailsViewModel.onAction(DetailsAction.OnShowColorSelectorBottomSheet(false)) },
            ) {
                ColorSelectorElement(
                    recentColors = detailsState.allColors,
                    preselectedColor = detailsState.icalEntry.color,
                    onColorChanged = { detailsViewModel.onAction(DetailsAction.OnUpdateColor(it)) },
                    skipPartialSelection = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (detailsState.showMoreBottomSheet) {
            DetailsMoreBottomSheet(
                onAction = { action -> detailsViewModel.onAction(action) },
                icalEntry = detailsState.icalEntry,
                canWriteContent = detailsState.allowEditing()
            )
        }

        if (detailsState.showJournalStatusPickerBottomSheet) {
            JournalStatusPickerBottomSheet(
                status = detailsState.icalEntry.status,
                sheetState = rememberModalBottomSheetState(),
                onStatusUpdated = { detailsViewModel.onAction(DetailsAction.OnUpdateStatus(it)) },
                onDismiss = { detailsViewModel.onAction(DetailsAction.OnShowJournalStatusPickerBottomSheet(false)) }
            )
        }

        if (detailsState.showTaskStatusProgressPickerBottomSheet) {
            TaskStatusProgressPickerBottomSheet(
                status = detailsState.icalEntry.status,
                percentComplete = detailsState.icalEntry.percentComplete,
                sheetState = rememberModalBottomSheetState(),
                onStatusUpdated = { detailsViewModel.onAction(DetailsAction.OnUpdateStatus(it)) },
                onProgressUpdated = { detailsViewModel.onAction(DetailsAction.OnUpdateProgress(it)) },
                onDismiss = { detailsViewModel.onAction(DetailsAction.OnShowTaskStatusProgressPickerBottomSheet(false)) }
            )
        }

        if (detailsState.showDeleteDialog) {
            DeleteIcalEntryDialog(
                icalEntry = detailsState.icalEntry,
                onConfirm = {
                    detailsViewModel.onAction(DetailsAction.OnDelete)
                },
                onDismiss = {
                    detailsViewModel.showDeleteDialog(false)
                }
            )
        }

        if (detailsState.showAddSubtaskBottomSheet) {
            AddSubtaskBottomSheet(
                onSubtaskAdded = { detailsViewModel.onAction(DetailsAction.OnAddSubtask(it)) },
                onDismiss = {
                    detailsViewModel.onAction(DetailsAction.OnShowAddSubtaskBottomSheet(false))
                }
            )
        }

        if (detailsState.icalEntry.syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED
            || detailsState.icalEntry.syncState == SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED
            || detailsState.icalEntry.syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED
        ) {
            ResolveSyncConflictDialog(
                syncState = detailsState.icalEntry.syncState,
                onKeepLocalChanges = { detailsViewModel.onAction(DetailsAction.OnSyncConflictUpdateUserDecision(SyncState.USER_DECIDED_CLIENT_WINS)) },
                onLoadServerChanges = { detailsViewModel.onAction(DetailsAction.OnSyncConflictUpdateUserDecision(SyncState.USER_DECIDED_SERVER_WINS)) },
                onDeleteEntry = { detailsViewModel.onAction(DetailsAction.OnSyncConflictUpdateUserDecision(SyncState.REMOTE_DELETED_LOCAL_TRASHBIN)) }
            )
        }

        Scaffold(
            topBar = {
                DetailsTopBar(
                    canWriteContent = detailsState.allowEditing(),
                    isLoading = detailsState.isLoading,
                    isPinned = detailsState.icalEntry.categories.any { it == IcalEntry.PINNED_CATEGORY },
                    onAction = { action -> detailsViewModel.onAction(action) }
                )
            },
            bottomBar = {
                BottomAppBar {
                    TextButton(
                        onClick = { detailsViewModel.onAction(DetailsAction.OnShowColorSelectorBottomSheet(true)) },
                        enabled = detailsState.allowEditing() && !detailsState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = stringResource(Res.string.color)
                        )
                    }

                    TextButton(
                        onClick = { detailsViewModel.onAction(DetailsAction.OnShowCategorySelectorBottomSheet(true)) },
                        enabled = detailsState.allowEditing() && !detailsState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Label,
                            contentDescription = stringResource(Res.string.category)
                        )
                    }

                    if (detailsState.icalEntry.isJournal()) {

                        TextButton(
                            onClick = { detailsViewModel.onAction(DetailsAction.OnShowJournalStatusPickerBottomSheet(!detailsState.showJournalStatusPickerBottomSheet)) },
                            enabled = detailsState.allowEditing() && !detailsState.isLoading
                        ) {
                            Icon(
                                imageVector = detailsState.icalEntry.status?.vectorIcon ?: Status.FINAL.vectorIcon!!,
                                contentDescription = stringResource(Res.string.more),
                                tint = when (detailsState.icalEntry.status) {
                                    Status.DRAFT -> MaterialTheme.colorScheme.error
                                    Status.CANCELLED -> MaterialTheme.colorScheme.onSurface
                                    else -> LocalContentColor.current
                                }
                            )
                        }
                    }

                    if (detailsState.icalEntry.isTask()) {

                        TextButton(
                            onClick = { detailsViewModel.onAction(DetailsAction.OnShowTaskStatusProgressPickerBottomSheet(!detailsState.showTaskStatusProgressPickerBottomSheet)) },
                            enabled = detailsState.allowEditing() && !detailsState.isLoading
                        ) {

                            Box(contentAlignment = Alignment.Center) {

                                Text(
                                    text = detailsState.icalEntry.percentComplete.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp
                                )

                                CircularProgressIndicator(
                                    progress = { detailsState.icalEntry.percentComplete.toFloat() / 100 },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (detailsState.icalEntry.isTask()) {

                        TextButton(
                            onClick = { detailsViewModel.onAction(DetailsAction.OnShowAddSubtaskBottomSheet(!detailsState.showTaskStatusProgressPickerBottomSheet)) },
                            enabled = detailsState.allowEditing() && !detailsState.isLoading
                        ) {
                            Icon(Icons.Outlined.AddTask, stringResource(Res.string.subtask))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = { detailsViewModel.onAction(DetailsAction.OnShowMoreBottomSheet(true)) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(Res.string.more)
                        )
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(detailsState.allowRestore() && !detailsState.isLoading) {
                    ExtendedFloatingActionButton(
                        onClick = { detailsViewModel.onAction(DetailsAction.OnRestoreEntry) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = stringResource(Res.string.restore)
                            )
                            Text(
                                text = stringResource(Res.string.restore)
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
                DetailsScreen(
                    state = detailsState,
                    onAction = { action -> detailsViewModel.onAction(action) },
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


@Preview
@Composable
private fun ListScreenRoot_Preview() {
    DetailsScreenRoot(
        detailsViewModel = koinViewModel<DetailsViewModel>(),
        onNavigate = {},
        onNavigateUp = {}
    )
}

