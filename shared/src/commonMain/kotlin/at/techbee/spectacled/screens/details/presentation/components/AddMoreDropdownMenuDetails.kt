package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAlarm
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.AddLocation
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.DatasetLinked
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.SpectacledDropdownMenu
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.presentation.components.SquareDropdownMenuItem
import at.techbee.spectacled.screens.details.presentation.AttachmentPickerAction
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import at.techbee.spectacled.screens.details.presentation.DetailsSheetOrDialog
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.alarm
import spectacled.shared.generated.resources.attachment_by_url
import spectacled.shared.generated.resources.attachment_file
import spectacled.shared.generated.resources.attachment_from_gallery
import spectacled.shared.generated.resources.attachment_photo
import spectacled.shared.generated.resources.drawing
import spectacled.shared.generated.resources.file_attachment
import spectacled.shared.generated.resources.image_attachment
import spectacled.shared.generated.resources.location
import spectacled.shared.generated.resources.other_attachment
import spectacled.shared.generated.resources.subtask
import spectacled.shared.generated.resources.subtasks_not_supported_in_collection
import spectacled.shared.generated.resources.url

@OptIn(ExperimentalGridApi::class)
@Composable
fun AddMoreDropdownMenuDetails(
    addMoreExpanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (DetailsAction) -> Unit,
    hasLocation: Boolean,
    hasUrl: Boolean,
    isTask: Boolean,
    calendarSupportsTasks: Boolean
) {
    SpectacledDropdownMenu(
        expanded = addMoreExpanded,
        onDismissRequest = onDismiss,
    ) {

        Text(
            text = stringResource(Res.string.file_attachment),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3
        ) {
            SquareDropdownMenuItem(
                text = stringResource(Res.string.attachment_file),
                icon = Icons.Outlined.Attachment,
                onClick = {
                    onAction(DetailsAction.OnLaunchPicker(AttachmentPickerAction.FILE))
                    onDismiss()
                }
            )

            SquareDropdownMenuItem(
                text = stringResource(Res.string.attachment_by_url),
                icon = Icons.Outlined.DatasetLinked,
                onClick = {
                    onAction(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.ADD_ATTACHMENT_URL))
                    onDismiss()
                },
            )


        }

        Text(
            text = stringResource(Res.string.image_attachment),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3
        ) {

            if (getPlatform().platform in listOf(Platforms.IOS, Platforms.ANDROID)) {
                SquareDropdownMenuItem(
                    text = stringResource(Res.string.attachment_photo),
                    icon = Icons.Outlined.PhotoCamera,
                    onClick = {
                        onAction(DetailsAction.OnLaunchPicker(AttachmentPickerAction.PHOTO))
                        onDismiss()
                    },
                )
            }

            SquareDropdownMenuItem(
                text = stringResource(Res.string.attachment_from_gallery) ,
                icon = Icons.Outlined.Image,
                onClick = {
                    onAction(DetailsAction.OnLaunchPicker(AttachmentPickerAction.GALLERY))
                    onDismiss()
                },
            )

            SquareDropdownMenuItem(
                text = stringResource(Res.string.drawing) ,
                icon = Icons.Outlined.Gesture,
                onClick = {
                    onAction(DetailsAction.OnShowDrawingCanvasBottomSheet(true, null, null))
                    onDismiss()
                },
            )
        }

        Text(
            text = stringResource(Res.string.other_attachment),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3
        ) {

            SquareDropdownMenuItem(
                text = stringResource(Res.string.url),
                icon = Icons.Outlined.AddLink,
                enabled = !hasUrl,
                onClick = {
                    onAction(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.EDIT_URL))
                    onDismiss()
                },
            )

            if(isTask) {
                SquareDropdownMenuItem(
                    text = stringResource(Res.string.location),
                    icon = Icons.Outlined.AddLocation,
                    enabled = !hasLocation,
                    onClick = {
                        onAction(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.EDIT_LOCATION))
                        onDismiss()
                    },
                )

                SquareDropdownMenuItem(
                    text = stringResource(Res.string.alarm),
                    icon = Icons.Outlined.AddAlarm,
                    onClick = {
                        onAction(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.ADD_ALARM))
                        onDismiss()
                    },
                )
            }

            SquareDropdownMenuItem(
                text = stringResource(Res.string.subtask),
                icon = Icons.Outlined.AddTask,
                onClick = {
                    onAction(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.ADD_SUBTASKS))
                    onDismiss()
                },
                enabled = calendarSupportsTasks
            )
        }

        if (!calendarSupportsTasks)
            Text(
                text = stringResource(Res.string.subtasks_not_supported_in_collection),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
    }
}


@Preview
@Composable
private fun AddMoreDropdownMenu_Details_Task_Preview() {

    AppTheme(SpectacledVariant.TASKS) {
        Scaffold {
            Column {
                AddMoreDropdownMenuDetails(
                    addMoreExpanded = true,
                    onDismiss = {},
                    onAction = {},
                    hasLocation = false,
                    hasUrl = false,
                    isTask = true,
                    calendarSupportsTasks = true
                )
            }

        }
    }
}
