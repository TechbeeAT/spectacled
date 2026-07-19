package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import at.techbee.spectacled.screens.core.formatLocalized
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.entry_copy
import spectacled.shared.generated.resources.reload_calendars_folders
import spectacled.shared.generated.resources.retry
import spectacled.shared.generated.resources.show_more
import spectacled.shared.generated.resources.sync_info
import spectacled.shared.generated.resources.update_password

@Composable
fun CalendarSyncInfoDialog(
    calendarSyncStatus: CalendarSyncStatus,
    onRetry: () -> Unit,
    onShowPrincipalUpdatePassword: () -> Unit,
    onReloadCalendars: () -> Unit,
    onDismiss: () -> Unit
) {

    var showMore by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    onRetry()
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.retry))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
        icon = { Icon(Icons.Outlined.SyncProblem, null) },
        title = {
            Text(stringResource(Res.string.sync_info))
        },
        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {

                Text(calendarSyncStatus.icsDateTime.formatLocalized(IcsDateTimeFormat.DATE_TIME))

                calendarSyncStatus.message?.let {
                    Text(
                        text = it,
                        textAlign = TextAlign.Center)
                }

                if(calendarSyncStatus.type == CalendarSyncStatusType.NOT_AUTHORIZED) {
                    TextButton(
                        onClick = { onShowPrincipalUpdatePassword() }
                    ) { Text(stringResource(Res.string.update_password)) }
                }

                if(calendarSyncStatus.type == CalendarSyncStatusType.NOT_FOUND) {
                    TextButton(
                        onClick = { onReloadCalendars() }
                    ) { Text(stringResource(Res.string.reload_calendars_folders)) }
                }

                AnimatedVisibility(!calendarSyncStatus.details.isNullOrEmpty() && !showMore) {
                    TextButton(
                        onClick = { showMore = true }
                    ) {
                        Text(stringResource(Res.string.show_more))
                    }
                }

                AnimatedVisibility(showMore) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = calendarSyncStatus.details?:"",
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )

                        TextButton(
                            onClick = {
                                clipboard.setText(
                                    AnnotatedString(calendarSyncStatus.message + "\n" + calendarSyncStatus.details)
                                )
                            }
                        ) {
                            Text(stringResource(Res.string.entry_copy))
                        }
                    }

                }
            }



        }
    )
}

@Preview
@Composable
private fun CalendarSyncStatus_Preview_FAILED() {
    CalendarSyncInfoDialog(
        calendarSyncStatus = CalendarSyncStatus(
            type = CalendarSyncStatusType.FAILED,
            message = "Connection error. Please check your internet connection and try again.",
            details = "Details message"
        ),
        onRetry = { },
        onShowPrincipalUpdatePassword = { },
        onReloadCalendars = { },
        onDismiss = { }
    )
}

@Preview
@Composable
private fun CalendarSyncStatus_Preview_NOT_AUTHORIZED() {
    CalendarSyncInfoDialog(
        calendarSyncStatus = CalendarSyncStatus(
            type = CalendarSyncStatusType.NOT_AUTHORIZED,
            //message = "Connection error. Please check your internet connection and try again.",
            //details = "Details message"
        ),
        onRetry = { },
        onShowPrincipalUpdatePassword = { },
        onReloadCalendars = { },
        onDismiss = { }
    )
}

@Preview
@Composable
private fun CalendarSyncStatus_Preview_NOT_FOUND() {
    CalendarSyncInfoDialog(
        calendarSyncStatus = CalendarSyncStatus(
            type = CalendarSyncStatusType.NOT_FOUND,
            //message = "Connection error. Please check your internet connection and try again.",
            //details = "Details message"
        ),
        onRetry = { },
        onShowPrincipalUpdatePassword = { },
        onReloadCalendars = { },
        onDismiss = { }
    )
}



