package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NoAccounts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.account.presentation.ProcessingState
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.Principal
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.delete_folder_warning
import spectacled.shared.generated.resources.delete_folder_x
import spectacled.shared.generated.resources.warning_operation_cannot_be_undone

@Composable
fun DeleteCalendarDialog(
    principal: Principal,
    calendar: Calendar,
    processingState: ProcessingState,
    onConfirm: (Principal, Calendar) -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(principal, calendar) },
                enabled = processingState != ProcessingState.Processing
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
        icon = { Icon(Icons.Outlined.NoAccounts, null) },
        title = {
            Text(stringResource(Res.string.delete_folder_x, calendar.displayName ?: calendar.url))
        },
        text = {
            Column {
                Text(stringResource(Res.string.delete_folder_warning))
                Text(
                    text = stringResource(Res.string.warning_operation_cannot_be_undone),
                    color = MaterialTheme.colorScheme.error, 
                    fontStyle = FontStyle.Italic
                )

                AnimatedVisibility(processingState == ProcessingState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp)
                    )
                }

                val errorState = processingState as? ProcessingState.Error
                AnimatedVisibility(errorState != null) {
                    Text(
                        text = "Calendar could not be deleted: ${errorState?.message}",
                        color = MaterialTheme.colorScheme.error,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun DeleteCalendarDialog_Preview_Idle() {
    DeleteCalendarDialog(
        principal = Principal.getPrincipalForPreview(),
        calendar = Calendar.getCalendarForPreview(),
        processingState = ProcessingState.Idle,
        onConfirm = { _, _ -> },
        onDismiss = {}
    )
}


@Preview
@Composable
private fun DeleteCalendarDialog_Preview_Processing() {
    DeleteCalendarDialog(
        principal = Principal.getPrincipalForPreview(),
        calendar = Calendar.getCalendarForPreview(),
        processingState = ProcessingState.Processing,
        onConfirm = { _, _ -> },
        onDismiss = {}
    )
}

@Preview
@Composable
private fun DeleteCalendarDialog_Preview_Error() {
    DeleteCalendarDialog(
        principal = Principal.getPrincipalForPreview(),
        calendar = Calendar.getCalendarForPreview(),
        processingState = ProcessingState.Error("This is an error"),
        onConfirm = { _, _ -> },
        onDismiss = {}
    )
}
