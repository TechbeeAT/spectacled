package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.components.CalendarSelector
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.move
import spectacled.shared.generated.resources.move_entry_copy_warning
import spectacled.shared.generated.resources.move_entry_warning
import spectacled.shared.generated.resources.move_entry_x

@Composable
fun MoveIcalEntryDialog(
    icalEntry: IcalEntry,
    principals: List<Principal>,
    homeCollections: List<HomeCollection>,
    calendars: List<Calendar>,
    onConfirm: (Long, Boolean) -> Unit,
    onDismiss: () -> Unit
) {

    var selectedCalendarId by remember { mutableStateOf(icalEntry.calendarId) }
    var keepOriginal by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedCalendarId, keepOriginal) },
                enabled = icalEntry.calendarId != selectedCalendarId
            ) {
                Text(stringResource(Res.string.move))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
        icon = { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, null) },
        title = {
            Text(stringResource(Res.string.move_entry_x, icalEntry.summary?: icalEntry.description?: ""))
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Keep oiginal (create copy)", fontWeight = FontWeight.Bold)
                    Switch(
                        checked = keepOriginal,
                        onCheckedChange = { keepOriginal = it}
                    )

                }

                Crossfade(keepOriginal) {
                    if(it)
                        Text(stringResource(Res.string.move_entry_copy_warning))
                    else
                        Text(stringResource(Res.string.move_entry_warning))
                }



            }

        }
    )
}

@Preview
@Composable
private fun MoveIcalEntryDialog_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveIcalEntryDialog(
                icalEntry = IcalEntry.getSampleIcalEntry(),
                calendars = listOf(Calendar.getCalendarForPreview()),
                homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
                principals = listOf(Principal.getPrincipalForPreview()),
                onConfirm = { _, _ -> },
                onDismiss = {}
            )
        }
    }
}
