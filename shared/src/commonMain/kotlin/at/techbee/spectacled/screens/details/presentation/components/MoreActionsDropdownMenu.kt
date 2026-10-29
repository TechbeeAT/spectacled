package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.SpectacledDropdownMenu
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.presentation.components.SquareDropdownMenuItem
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import at.techbee.spectacled.screens.details.presentation.DetailsSheetOrDialog
import at.techbee.spectacled.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.clipboard
import spectacled.shared.generated.resources.copied_to_clipboard
import spectacled.shared.generated.resources.copy
import spectacled.shared.generated.resources.created
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.label_value
import spectacled.shared.generated.resources.last_modified
import spectacled.shared.generated.resources.move
import spectacled.shared.generated.resources.send_as_email
import spectacled.shared.generated.resources.share
import spectacled.shared.generated.resources.sharing

@OptIn(ExperimentalGridApi::class)
@Composable
fun MoreActionsDropdownMenu(
    expanded: Boolean,
    icalEntry: IcalEntry,
    onDismiss: () -> Unit,
    onAction: (DetailsAction) -> Unit,
    canWriteContent: Boolean
) {

    val copiedToClipboardText = stringResource(Res.string.copied_to_clipboard)
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val shareText = icalEntry.getAnnotatedStringForShare()

    SpectacledDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {

        Text(
            text = "Actions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            SquareDropdownMenuItem(
                text = stringResource(Res.string.copy),
                icon = Icons.Outlined.FileCopy,
                onClick = {
                    onAction(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.COPY))
                    onDismiss()
                },
                // No write access needed: the copy may target any writable collection, even from a
                // read-only one. Recurring entries can't be copied, like in the list, and an
                // untouched new entry has nothing to copy yet.
                enabled = !icalEntry.isRecurring() && icalEntry.syncState != SyncState.LOCAL_NEW
            )

            SquareDropdownMenuItem(
                text = stringResource(Res.string.move),
                icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                onClick = {
                    onAction(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.MOVE))
                    onDismiss()
                },
                enabled = canWriteContent
            )

            SquareDropdownMenuItem(
                text = stringResource(Res.string.delete),
                icon = Icons.Outlined.Delete,
                onClick = {
                    onAction(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.DELETE))
                    onDismiss()
                },
                highlightContainerColors = true,
                enabled = canWriteContent
            )


        }

        Text(
            text = stringResource(Res.string.sharing),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            SquareDropdownMenuItem(
                text = stringResource(Res.string.clipboard),
                icon = Icons.Outlined.ContentPaste,
                onClick = {
                    scope.launch { clipboard.setText(shareText) }
                    onAction(DetailsAction.OnUpdateSnackbar(copiedToClipboardText))
                    onDismiss()
                },
            )

            SquareDropdownMenuItem(
                text = when (getPlatform().platform) {
                    Platforms.ANDROID, Platforms.IOS -> stringResource(Res.string.share)
                    Platforms.DESKTOP, Platforms.WASM -> stringResource(Res.string.send_as_email)
                },
                icon = when (getPlatform().platform) {
                    Platforms.ANDROID -> Icons.Outlined.Share
                    Platforms.IOS -> Icons.Outlined.IosShare
                    Platforms.DESKTOP, Platforms.WASM -> Icons.Outlined.Email
                },
                onClick = {
                    onAction(DetailsAction.OnShare)
                    onDismiss()
                },
            )
        }


        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp).fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    Res.string.label_value,
                    stringResource(Res.string.created),
                    icalEntry.created.formatLocalized(IcsDateTimeFormat.DATE_TIME)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.5f)
            )
            Text(
                text = stringResource(
                    Res.string.label_value,
                    stringResource(Res.string.last_modified),
                    (icalEntry.lastModified ?: icalEntry.created).formatLocalized(IcsDateTimeFormat.DATE_TIME)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.5f)
            )
            /*
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sync status: ${icalEntry.syncState.name}",
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.5f)
            )
             */
        }

    }
}


@Preview
@Composable
private fun MoreActionsDropdownMenu_Task_Preview() {

    AppTheme(SpectacledVariant.TASKS) {
        Scaffold {
            Column {
                MoreActionsDropdownMenu(
                    expanded = true,
                    icalEntry = IcalEntry.getSampleIcalEntry(),
                    onDismiss = {},
                    onAction = {},
                    canWriteContent = true
                )
            }

        }
    }
}

@Preview
@Composable
private fun MoreActionsDropdownMenu_readonly_Task_Preview() {

    AppTheme(SpectacledVariant.TASKS) {
        Scaffold {
            Column {
                MoreActionsDropdownMenu(
                    expanded = true,
                    icalEntry = IcalEntry.getSampleIcalEntry(),
                    onDismiss = {},
                    onAction = {},
                    canWriteContent = false
                )
            }

        }
    }
}