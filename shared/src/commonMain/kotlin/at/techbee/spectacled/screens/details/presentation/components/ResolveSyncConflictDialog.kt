package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.domain.SyncState
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.sync_conflict_delete_local_entry
import spectacled.shared.generated.resources.sync_conflict_detected
import spectacled.shared.generated.resources.sync_conflict_entry_deleted_while_server_modified
import spectacled.shared.generated.resources.sync_conflict_entry_modified_while_server_deleted
import spectacled.shared.generated.resources.sync_conflict_entry_modified_while_server_modified
import spectacled.shared.generated.resources.sync_conflict_keep_local_changes
import spectacled.shared.generated.resources.sync_conflict_load_server_changes

@Composable
fun ResolveSyncConflictDialog(
    syncState: SyncState,
    onKeepLocalChanges: () -> Unit,
    onLoadServerChanges: () -> Unit,
    onDeleteEntry: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { },
        confirmButton = { },
        dismissButton = { },
        icon = { Icon(Icons.Outlined.SyncProblem, null) },
        title = {
            Text(stringResource(Res.string.sync_conflict_detected))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val text = when(syncState) {
                    SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED -> {
                        stringResource(Res.string.sync_conflict_entry_modified_while_server_modified)
                    }
                    SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED -> {
                        stringResource(Res.string.sync_conflict_entry_deleted_while_server_modified)
                    }
                    SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED -> {
                        stringResource(Res.string.sync_conflict_entry_modified_while_server_deleted)
                    }
                    else -> ""
                }

                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if(syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED || syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED)
                    TextButton(
                        onClick = { onKeepLocalChanges() }
                    ) {
                        Text(stringResource(Res.string.sync_conflict_keep_local_changes))
                    }

                if(syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED || syncState == SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED)
                    TextButton(
                        onClick = { onLoadServerChanges() }
                    ) {
                        Text(stringResource(Res.string.sync_conflict_load_server_changes))
                    }

                if(syncState == SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED || syncState == SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED)
                    TextButton(
                        onClick = { onDeleteEntry() }
                    ) {
                        Text(stringResource(Res.string.sync_conflict_delete_local_entry))
                    }
            }
        }
    )
}

@Preview
@Composable
private fun ResolveSyncConflictDialog_Preview_CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED() {
    ResolveSyncConflictDialog(
        syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED,
        onKeepLocalChanges = {},
        onLoadServerChanges = {},
        onDeleteEntry = {},
        //onDismiss = {}
    )
}

@Preview
@Composable
private fun ResolveSyncConflictDialog_Preview_CONFLICT_LOCAL_DELETED_SERVER_MODIFIED() {
    ResolveSyncConflictDialog(
        syncState = SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED,
        onKeepLocalChanges = {},
        onLoadServerChanges = {},
        onDeleteEntry = {},
        //onDismiss = {}
    )
}

@Preview
@Composable
private fun ResolveSyncConflictDialog_Preview_CONFLICT_LOCAL_MODIFIED_SERVER_DELETED() {
    ResolveSyncConflictDialog(
        syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED,
        onKeepLocalChanges = {},
        onLoadServerChanges = {},
        onDeleteEntry = {},
        //onDismiss = {}
    )
}

