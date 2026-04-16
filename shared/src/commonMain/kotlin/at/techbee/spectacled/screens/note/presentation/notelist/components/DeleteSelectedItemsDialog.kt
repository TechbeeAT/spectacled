package at.techbee.spectacled.screens.note.presentation.notelist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.delete_selected
import spectacled.shared.generated.resources.delete_selected_warning
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteSelectedItemsDialog(
    multiselectItems: List<Long>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onConfirm() },
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
        icon = { Icon(Icons.Outlined.DeleteOutline, null) },
        title = {
            Text(stringResource(Res.string.delete_selected))
        },
        text = {
            Column {
                Text(stringResource(Res.string.delete_selected_warning, multiselectItems.size))
            }
        }
    )
}

@Preview
@Composable
private fun DeleteSelectedItemsDialog_Preview() {
    DeleteSelectedItemsDialog(
        multiselectItems = listOf(1,2,3),
        onConfirm = {},
        onDismiss = {}
    )
}

