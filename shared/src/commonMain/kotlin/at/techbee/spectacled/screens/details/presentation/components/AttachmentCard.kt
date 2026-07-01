package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.attachment
import spectacled.shared.generated.resources.delete

@Composable
fun AttachmentCard(
    attachment: Attachment,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {


    Card(
        onClick = { onAction(DetailsAction.OnOpenAttachment(attachment.id)) },
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        modifier = modifier
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(
                onClick = {},
                enabled = false
            ) {
                Icon(Icons.Outlined.Attachment, stringResource(Res.string.attachment))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName ?: "unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (attachment.size != null) {
                    Text(
                        text = "${attachment.size / 1024} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { onAction(DetailsAction.OnDeleteAttachment(attachment.id)) }) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(Res.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview
@Composable
private fun AttachmentCard_Preview() {
    AttachmentCard(
        attachment = Attachment(
            id = 1L,
            fileName = "test.pdf",
            mimeType = "application/pdf",
            size = 125000L
        ),
        onAction = {},
        modifier = Modifier.padding(8.dp)
    )
}
