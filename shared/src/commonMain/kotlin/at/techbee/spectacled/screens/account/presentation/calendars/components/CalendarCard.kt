package at.techbee.spectacled.screens.account.presentation.calendars.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.account.presentation.calendars.CalendarListAction
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.SpecialRoundedCard
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.edit
import spectacled.shared.generated.resources.folders
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.open_foldername
import spectacled.shared.generated.resources.read_only
import spectacled.shared.generated.resources.refresh
import spectacled.shared.generated.resources.sync_problem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarCard(
    principal: Principal,
    homeCollection: HomeCollection,
    calendar: Calendar,
    isEditMode: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onAction: (CalendarListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    //var showSelectColor by remember { mutableStateOf(false) }
    var calendarMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val smallIconSize = 20.dp

    SpecialRoundedCard(
        isFirst = isFirst,
        isLast = isLast,
        onClick = {
            if (!isEditMode)
                onAction(CalendarListAction.OnCalendarClicked(calendar.id))
        },
        colors = CardDefaults.cardColors(
            containerColor = calendar.color ?: Color.Unspecified,
            contentColor = calendar.color?.let { getContentColorForColoredSurfaces(it) } ?: Color.Unspecified
        ),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(8.dp).heightIn(min = 48.dp)
        ) {

            Crossfade(calendar.calendarSyncStatus?.type == CalendarSyncStatusType.IN_PROGRESS) { syncInProgress ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).padding(8.dp)
                ) {
                    if(syncInProgress)
                        CircularProgressIndicator(
                            color = LocalContentColor.current,
                            modifier = Modifier.size(20.dp)
                        )
                    else
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = stringResource(Res.string.folders),
                            tint = LocalContentColor.current
                        )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = calendar.displayName?:calendar.url.toString(),
                    style = MaterialTheme.typography.titleMedium
                    //fontWeight = FontWeight.Bold
                )

                if(calendar.calendarDescription?.isNotBlank() == true)
                    Text(
                        text = calendar.calendarDescription,
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.labelSmall
                    )
            }

            if(!calendar.canWriteProperties()) {
                TextButton(
                    onClick = { },
                    enabled = false,
                    colors = ButtonDefaults.textButtonColors().copy(
                        disabledContentColor = calendar.color?.let { getContentColorForColoredSurfaces(it).copy(alpha = 0.5f) } ?: ButtonDefaults.textButtonColors().disabledContentColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditOff,
                        contentDescription = stringResource(Res.string.read_only),
                        modifier = Modifier.size(smallIconSize)
                    )
                }
            }

            AnimatedVisibility(calendar.calendarSyncStatus?.type?.isErrorType() == true) {
                IconButton(
                    onClick = { onAction(CalendarListAction.OnShowSyncInfoDialog(principal, calendar)) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SyncProblem,
                        contentDescription = stringResource(Res.string.sync_problem),
                        modifier = Modifier.size(smallIconSize)
                    )
                }
            }

            AnimatedVisibility(isEditMode) {
                Row {

                    if(calendar.canWriteProperties()) {
                        TextButton(
                            onClick = {
                                onAction(CalendarListAction.OnShowCreateOrUpdateCalendarBottomSheet(principal, homeCollection, calendar))
                            },
                            colors = ButtonDefaults.textButtonColors().copy(
                                contentColor = calendar.color?.let { getContentColorForColoredSurfaces(it) } ?: Color.Unspecified
                            )
                        ) {
                            Text(stringResource(Res.string.edit))
                        }
                    }


                    TextButton(
                        onClick = {
                            calendarMenuExpanded = true
                        },
                        colors = ButtonColors(
                            contentColor = calendar.color?.let { getContentColorForColoredSurfaces(it) } ?: Color.Unspecified,
                            containerColor = Color.Unspecified,
                            disabledContainerColor = Color.Unspecified,
                            disabledContentColor = Color.Unspecified
                        )
                    ) {
                        Icon(Icons.Outlined.MoreVert, stringResource(Res.string.more))

                        DropdownMenu(
                            expanded = calendarMenuExpanded,
                            onDismissRequest = { calendarMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.refresh)) },
                                onClick = {
                                    calendarMenuExpanded = false
                                    onAction(CalendarListAction.OnSyncCalendars(listOf(calendar)))
                                },
                                leadingIcon = { Icon(Icons.Outlined.Sync, null) }
                            )

                            if(homeCollection.canUnbind()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.delete)) },
                                    onClick = {
                                        calendarMenuExpanded = false
                                        onAction(CalendarListAction.OnShowDeleteCalendarDialog(principal, calendar))
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(!isEditMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    /*
                    Text(
                        text = calendar.notes.size.toString(),
                        //style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.alpha(0.7f).padding(horizontal = 4.dp)
                    )
                     */

                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = stringResource(Res.string.open_foldername, calendar.displayName?:calendar.url.toString()),
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}



@Preview
@Composable
private fun FolderCard_no_edit_Preview() {
    CalendarCard(
        principal = Principal.getPrincipalForPreview(),
        homeCollection = HomeCollection.getHomeCollectionForPreview(),
        calendar = Calendar.getCalendarForPreview().copy(
            calDavPrivileges = listOf(CalDavPrivilege.WRITE)
        ),
        isEditMode = false,
        isFirst = true,
        isLast = true,
        onAction = {}
    )
}

@Preview
@Composable
private fun FolderCard_no_edit_readonly_Preview() {
    CalendarCard(
        principal = Principal.getPrincipalForPreview(),
        homeCollection = HomeCollection.getHomeCollectionForPreview(),
        calendar = Calendar.getCalendarForPreview(),
        isEditMode = false,
        isFirst = true,
        isLast = true,
        onAction = {}
    )
}

@Preview
@Composable
private fun FolderCard_no_edit_colored_Preview() {
    CalendarCard(
        principal = Principal.getPrincipalForPreview(),
        homeCollection = HomeCollection.getHomeCollectionForPreview(),
        calendar = Calendar.getCalendarForPreview().copy(color = Color.Yellow),
        isEditMode = false,
        isFirst = true,
        isLast = false,
        onAction = {}
    )
}

@Preview
@Composable
private fun FolderCard_edit_Preview() {
    CalendarCard(
        principal = Principal.getPrincipalForPreview(),
        homeCollection = HomeCollection.getHomeCollectionForPreview(),
        calendar = Calendar.getCalendarForPreview().copy(
            calDavPrivileges = listOf(CalDavPrivilege.WRITE)
        ),
        isEditMode = true,
        isFirst = false,
        isLast = false,
        onAction = {}
    )
}

@Preview
@Composable
private fun FolderCard_edit_colored_Preview() {
    CalendarCard(
        principal = Principal.getPrincipalForPreview(),
        homeCollection = HomeCollection.getHomeCollectionForPreview().copy(
            calDavPrivileges = listOf(CalDavPrivilege.UNBIND),
            url = Url("https://localhost/home-collection")
        ),
        calendar = Calendar.getCalendarForPreview().copy(
            color = Color.Blue, 
            calendarSyncStatus = CalendarSyncStatus(CalendarSyncStatusType.FAILED),
            calDavPrivileges = listOf(CalDavPrivilege.WRITE)
        ),
        isEditMode = true,
        isFirst = false,
        isLast = true,
        onAction = {}
    )
}

@Preview
@Composable
private fun FolderCard_sync_in_progress_Preview() {
    CalendarCard(
        principal = Principal.getPrincipalForPreview(),
        homeCollection = HomeCollection.getHomeCollectionForPreview(),
        calendar = Calendar.getCalendarForPreview().copy(calendarSyncStatus = CalendarSyncStatus(CalendarSyncStatusType.IN_PROGRESS)),
        isEditMode = false,
        isFirst = false,
        isLast = false,
        onAction = {}
    )
}


