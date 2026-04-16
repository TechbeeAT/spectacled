package at.techbee.spectacled.screens.note.presentation.notedetails.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.screens.note.domain.Note
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.delete_note_warning
import spectacled.shared.generated.resources.delete_note_x
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteNoteDialog(
    note: Note,
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
            Text(stringResource(Res.string.delete_note_x, note.summary?: note.description?: ""))
        },
        text = {
            Text(stringResource(Res.string.delete_note_warning))

        }
    )
}

@Preview
@Composable
private fun DeleteNoteDialog_Preview() {
    DeleteNoteDialog(
        note = Note.getSampleNote(),
        onConfirm = {},
        onDismiss = {}
    )
}
