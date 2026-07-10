package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NoEncryption
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.insecure_connection_connect_anyway
import spectacled.shared.generated.resources.insecure_connection_message
import spectacled.shared.generated.resources.insecure_connection_title

@Composable
fun InsecureConnectionWarningDialog(
    server: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        icon = { Icon(Icons.Outlined.NoEncryption, null) },
        title = { Text(stringResource(Res.string.insecure_connection_title, server))},
        text = { Text(stringResource(Res.string.insecure_connection_message)) },
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onConfirm() }
            ) {
                Text(
                    text = stringResource(Res.string.insecure_connection_connect_anyway),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Preview
@Composable
private fun DeleteCalendarDialog_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            InsecureConnectionWarningDialog(
                server = "http://insecure.server.com",
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

