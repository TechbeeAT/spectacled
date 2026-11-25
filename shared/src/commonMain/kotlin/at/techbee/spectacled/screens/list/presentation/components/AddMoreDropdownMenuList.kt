package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DatasetLinked
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.Route.IcalEntryDetails
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.SpectacledDropdownMenu
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.presentation.components.SquareDropdownMenuItem
import at.techbee.spectacled.screens.details.presentation.DetailsInitialAction
import at.techbee.spectacled.screens.list.presentation.ListAction
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.ai_assisted
import spectacled.shared.generated.resources.ai_create_entries
import spectacled.shared.generated.resources.ai_provider_not_configured
import spectacled.shared.generated.resources.attachment_by_url
import spectacled.shared.generated.resources.attachment_file
import spectacled.shared.generated.resources.attachment_from_gallery
import spectacled.shared.generated.resources.attachment_photo
import spectacled.shared.generated.resources.drawing
import spectacled.shared.generated.resources.file_attachment
import spectacled.shared.generated.resources.image_attachment

@OptIn(ExperimentalGridApi::class)
@Composable
fun AddMoreDropdownMenuList(
    addMoreExpanded: Boolean,
    calendarId: Long,
    isAiProviderConfigured: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (Route) -> Unit,
    onAction: (ListAction) -> Unit,
) {
    SpectacledDropdownMenu(
        expanded = addMoreExpanded,
        onDismissRequest = onDismiss,
    ) {

        Text(
            text = stringResource(Res.string.ai_assisted),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SquareDropdownMenuItem(
                text = stringResource(Res.string.ai_create_entries),
                icon = Icons.Outlined.AutoAwesome,
                enabled = isAiProviderConfigured,
                onClick = {
                    onAction(ListAction.OnShowDeriveEntriesBottomSheet(true))
                    onDismiss()
                }
            )

            if (!isAiProviderConfigured)
                Text(
                    text = stringResource(Res.string.ai_provider_not_configured),
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(8.dp).widthIn(max = 176.dp)
                )
        }

        Text(
            text = stringResource(Res.string.file_attachment),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SquareDropdownMenuItem(
                text = stringResource(Res.string.attachment_file),
                icon = Icons.Outlined.Attachment,
                onClick = {
                    onNavigate(IcalEntryDetails(0L, calendarId, detailsInitialAction = DetailsInitialAction.ADD_ATTACHMENT.name))
                    onDismiss()
                }
            )

            SquareDropdownMenuItem(
                text = stringResource(Res.string.attachment_by_url),
                icon = Icons.Outlined.DatasetLinked,
                onClick = {
                    onNavigate(IcalEntryDetails(0L, calendarId, detailsInitialAction = DetailsInitialAction.ADD_ATTACHMENT_URL.name))
                    onDismiss()
                },
            )


        }

        Text(
            text = stringResource(Res.string.image_attachment),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (getPlatform().platform in listOf(Platforms.IOS, Platforms.ANDROID)) {
                SquareDropdownMenuItem(
                    text = stringResource(Res.string.attachment_photo),
                    icon = Icons.Outlined.PhotoCamera,
                    onClick = {
                        onNavigate(IcalEntryDetails(0L, calendarId, detailsInitialAction = DetailsInitialAction.ADD_PHOTO.name))
                        onDismiss()
                    },
                )
            }

            SquareDropdownMenuItem(
                text = stringResource(Res.string.attachment_from_gallery) ,
                icon = Icons.Outlined.Image,
                onClick = {
                    onNavigate(IcalEntryDetails(0L, calendarId, detailsInitialAction = DetailsInitialAction.ADD_FROM_GALLERY.name))
                    onDismiss()
                },
            )

            SquareDropdownMenuItem(
                text = stringResource(Res.string.drawing) ,
                icon = Icons.Outlined.Gesture,
                onClick = {
                    onNavigate(IcalEntryDetails(0L, calendarId, detailsInitialAction = DetailsInitialAction.ADD_DRAWING.name))
                    onDismiss()
                },
            )
        }
    }
}


@Preview
@Composable
private fun AddMoreDropdownMenu_Task_Preview() {

    AppTheme(SpectacledVariant.TASKS) {
        Scaffold {
            Column {
                AddMoreDropdownMenuList(
                    addMoreExpanded = true,
                    calendarId = 0L,
                    onDismiss = {},
                    onNavigate = {},
                    onAction = {},
                    isAiProviderConfigured = false
                )
            }

        }
    }
}

