package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.components.CalendarSelector
import at.techbee.spectacled.screens.core.presentation.components.MoveOrCopyMode
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.copy
import spectacled.shared.generated.resources.copy_selected
import spectacled.shared.generated.resources.copy_selected_info
import spectacled.shared.generated.resources.move
import spectacled.shared.generated.resources.move_selected
import spectacled.shared.generated.resources.move_selected_warning

/**
 * Lets the user pick the target collection for moving or copying the selected entries.
 * Moving requires a different collection, copying may also target the current one.
 */
@Composable
fun MoveOrCopySelectedItemsDialog(
    itemCount: Int,
    mode: MoveOrCopyMode,
    sourceCalendarId: Long,
    principals: List<Principal>,
    homeCollections: List<HomeCollection>,
    calendars: List<Calendar>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {

    // The current collection is not offered when it is read-only (copying out of it is allowed),
    // so preselect the first writable collection instead.
    var selectedCalendarId by remember {
        mutableStateOf(
            sourceCalendarId.takeIf { id -> calendars.any { it.id == id } } ?: calendars.firstOrNull()?.id
        )
    }
    // The move/copy runs in the background (it may first download attachments) and the dialog is
    // closed once it is done - until then, show progress and don't allow confirming again.
    var isInProgress by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isInProgress)
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                TextButton(
                    onClick = {
                        selectedCalendarId?.let {
                            isInProgress = true
                            onConfirm(it)
                        }
                    },
                    enabled = !isInProgress
                            && selectedCalendarId != null
                            && (mode == MoveOrCopyMode.COPY || sourceCalendarId != selectedCalendarId)
                ) {
                    Text(stringResource(if (mode == MoveOrCopyMode.COPY) Res.string.copy else Res.string.move))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
        icon = {
            Icon(
                if (mode == MoveOrCopyMode.COPY) Icons.Outlined.FileCopy else Icons.AutoMirrored.Outlined.DriveFileMove,
                null
            )
        },
        title = {
            Text(stringResource(if (mode == MoveOrCopyMode.COPY) Res.string.copy_selected else Res.string.move_selected))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                CalendarSelector(
                    principals = principals,
                    homeCollections = homeCollections,
                    calendars = calendars,
                    selectedCalendarId = selectedCalendarId,
                    onCalendarIdSelected = { selectedCalendarId = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    pluralStringResource(
                        if (mode == MoveOrCopyMode.COPY) Res.plurals.copy_selected_info else Res.plurals.move_selected_warning,
                        itemCount,
                        itemCount
                    )
                )
            }
        }
    )
}

@Preview
@Composable
private fun MoveOrCopySelectedItemsDialog_Move_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveOrCopySelectedItemsDialog(
                itemCount = 3,
                mode = MoveOrCopyMode.MOVE,
                sourceCalendarId = 0L,
                calendars = listOf(Calendar.getCalendarForPreview()),
                homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
                principals = listOf(Principal.getPrincipalForPreview()),
                onConfirm = { },
                onDismiss = {}
            )
        }
    }
}


@Preview
@Composable
private fun MoveOrCopySelectedItemsDialog_Copy_Single_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveOrCopySelectedItemsDialog(
                itemCount = 1,
                mode = MoveOrCopyMode.COPY,
                sourceCalendarId = 0L,
                calendars = listOf(Calendar.getCalendarForPreview()),
                homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
                principals = listOf(Principal.getPrincipalForPreview()),
                onConfirm = { },
                onDismiss = {}
            )
        }
    }
}
