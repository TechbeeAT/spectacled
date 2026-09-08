package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.local_data_delete_confirm_text
import spectacled.shared.generated.resources.local_data_delete_confirm_title


/**
 * Confirms wiping everything this browser holds. Unlike [RemovePrincipalDialog], which takes one
 * account out of the list, this also drops the settings and the offline copy of the entries - the
 * "I am done on this computer" action.
 */
@Composable
fun DeleteLocalDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(Res.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.cancel))
            }
        },
        icon = { Icon(Icons.Outlined.DeleteForever, null) },
        title = { Text(stringResource(Res.string.local_data_delete_confirm_title)) },
        text = { Text(stringResource(Res.string.local_data_delete_confirm_text)) }
    )
}


@Preview
@Composable
private fun DeleteLocalDataDialog_Preview() {
    Box(modifier = Modifier.fillMaxSize()) {
        DeleteLocalDataDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
