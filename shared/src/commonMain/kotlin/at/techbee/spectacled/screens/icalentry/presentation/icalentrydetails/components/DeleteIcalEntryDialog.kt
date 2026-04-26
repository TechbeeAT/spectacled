package at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.delete_entry_warning
import spectacled.shared.generated.resources.delete_entry_x
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteIcalEntryDialog(
    icalEntry: IcalEntry,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onConfirm() }
            ) {
                Text(stringResource(Res.string.delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
        icon = { Icon(Icons.Outlined.Delete, null) },
        title = {
            Text(stringResource(Res.string.delete_entry_x, icalEntry.summary?: icalEntry.description?: ""))
        },
        text = {
            Text(stringResource(Res.string.delete_entry_warning))

        }
    )
}

@Preview
@Composable
private fun DeleteIcalEntryDialog_Preview() {
    DeleteIcalEntryDialog(
        icalEntry = IcalEntry.getSampleIcalEntry(),
        onConfirm = {},
        onDismiss = {}
    )
}
