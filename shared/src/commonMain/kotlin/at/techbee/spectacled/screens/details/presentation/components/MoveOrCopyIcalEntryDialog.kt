package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.components.CalendarSelector
import at.techbee.spectacled.screens.core.presentation.components.MoveOrCopyMode
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.copy
import spectacled.shared.generated.resources.copy_entry_info
import spectacled.shared.generated.resources.copy_entry_x
import spectacled.shared.generated.resources.move
import spectacled.shared.generated.resources.move_entry_warning
import spectacled.shared.generated.resources.move_entry_x

/**
 * Lets the user pick the target collection for moving or copying [icalEntry].
 * Moving requires a different collection, copying may also target the current one.
 */
@Composable
fun MoveOrCopyIcalEntryDialog(
    icalEntry: IcalEntry,
    mode: MoveOrCopyMode,
    principals: List<Principal>,
    homeCollections: List<HomeCollection>,
    calendars: List<Calendar>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {

    // Only the user's own pick is remembered; until then the default follows [calendars], which
    // may still be loading when the dialog opens. The current collection is not offered when it
    // is read-only (copying out of it is allowed), so fall back to the first writable one.
    var userSelectedCalendarId by remember { mutableStateOf<Long?>(null) }
    val selectedCalendarId = userSelectedCalendarId
        ?: icalEntry.calendarId.takeIf { id -> calendars.any { it.id == id } }
        ?: calendars.firstOrNull()?.id

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { selectedCalendarId?.let { onConfirm(it) } },
                enabled = selectedCalendarId != null
                        && (mode == MoveOrCopyMode.COPY || icalEntry.calendarId != selectedCalendarId)
            ) {
                Text(stringResource(if (mode == MoveOrCopyMode.COPY) Res.string.copy else Res.string.move))
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
            Text(
                text = stringResource(
                    if (mode == MoveOrCopyMode.COPY) Res.string.copy_entry_x else Res.string.move_entry_x,
                    icalEntry.summary ?: icalEntry.description ?: ""),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
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
                    onCalendarIdSelected = { userSelectedCalendarId = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(if (mode == MoveOrCopyMode.COPY) Res.string.copy_entry_info else Res.string.move_entry_warning))
            }
        }
    )
}

@Preview
@Composable
private fun MoveOrCopyIcalEntryDialog_Move_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveOrCopyIcalEntryDialog(
                icalEntry = IcalEntry.getSampleIcalEntry(),
                mode = MoveOrCopyMode.MOVE,
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
private fun MoveOrCopyIcalEntryDialog_Copy_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveOrCopyIcalEntryDialog(
                icalEntry = IcalEntry.getSampleIcalEntry(),
                mode = MoveOrCopyMode.COPY,
                calendars = listOf(Calendar.getCalendarForPreview()),
                homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
                principals = listOf(Principal.getPrincipalForPreview()),
                onConfirm = { },
                onDismiss = {}
            )
        }
    }
}
