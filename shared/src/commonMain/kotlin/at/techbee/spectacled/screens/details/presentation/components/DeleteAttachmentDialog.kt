package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.delete_attachment_warning
import spectacled.shared.generated.resources.delete_attachment_x
import spectacled.shared.generated.resources.unknown

@Composable
fun DeleteAttachmentDialog(
    attachment: Attachment,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    val fileName = attachment.fileName ?: stringResource(Res.string.unknown)

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
            Text(stringResource(Res.string.delete_attachment_x, fileName))
        },
        text = {
            Text(stringResource(Res.string.delete_attachment_warning))
        }
    )
}

@Preview
@Composable
private fun DeleteAttachmentDialog_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            DeleteAttachmentDialog(
                attachment = Attachment(
                    id = 1L,
                    fileName = "my document.pdf",
                    mimeType = "application/pdf",
                    size = 125000L
                ),
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}
