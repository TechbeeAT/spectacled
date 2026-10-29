package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.copy
import spectacled.shared.generated.resources.move

/**
 * Lets the user pick the target collection for moving or copying entries of [sourceCalendarId].
 * Moving requires a different collection, copying may also target the current one.
 * [calendars] are the selectable (writable) targets.
 */
@Composable
fun MoveOrCopyDialog(
    mode: MoveOrCopyMode,
    title: String,
    message: String,
    sourceCalendarId: Long,
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
        ?: sourceCalendarId.takeIf { id -> calendars.any { it.id == id } }
        ?: calendars.firstOrNull()?.id

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { selectedCalendarId?.let { onConfirm(it) } },
                enabled = selectedCalendarId != null
                        && (mode == MoveOrCopyMode.COPY || sourceCalendarId != selectedCalendarId)
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
                text = title,
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

                Text(message)
            }
        }
    )
}
