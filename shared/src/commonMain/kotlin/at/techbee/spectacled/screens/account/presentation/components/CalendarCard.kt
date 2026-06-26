package at.techbee.spectacled.screens.account.presentation.components

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
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncDisabled
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.account.presentation.AccountListAction
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.components.SpecialRoundedCard
import at.techbee.spectacled.theme.AppTheme
import at.techbee.spectacled.theme.getColorSchemeForSeedColor
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.edit
import spectacled.shared.generated.resources.folders
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.open_foldername
import spectacled.shared.generated.resources.read_only
import spectacled.shared.generated.resources.sync_disabled
import spectacled.shared.generated.resources.sync_enabled
import spectacled.shared.generated.resources.sync_problem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarCard(
    principal: Principal,
    homeCollection: HomeCollection,
    calendar: Calendar,
    editEditFoldersModeEnabled: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onAction: (AccountListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    val smallIconSize = 20.dp

    var dropdownExpanded by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = getColorSchemeForSeedColor(calendar.color)) {

        SpecialRoundedCard(
            overrideTopRoundedCornerSize = if (isFirst) 16.dp else 0.dp,
            overrideBottomRoundedCornerSize = if (isLast) 16.dp else 0.dp,
            onClick = {
                if (!editEditFoldersModeEnabled)
                    onAction(AccountListAction.OnCalendarClicked(calendar.id))
            },
            enabled = calendar.calendarSyncStatus?.type != CalendarSyncStatusType.DISABLED,
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
                        if (syncInProgress)
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
                        text = calendar.displayName ?: calendar.url.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                        //fontWeight = FontWeight.Bold
                    )

                    if (calendar.calendarDescription?.isNotBlank() == true)
                        Text(
                            text = calendar.calendarDescription,
                            fontStyle = FontStyle.Italic,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                }

                if (!calendar.canWriteProperties()) {
                    TextButton(
                        onClick = { },
                        enabled = false
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
                        onClick = { onAction(AccountListAction.OnShowSyncInfoDialog(principal, calendar)) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SyncProblem,
                            contentDescription = stringResource(Res.string.sync_problem),
                            modifier = Modifier.size(smallIconSize)
                        )
                    }
                }

                AnimatedVisibility(calendar.calendarSyncStatus?.type == CalendarSyncStatusType.DISABLED) {
                    IconButton(
                        onClick = { },
                        enabled = false
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SyncDisabled,
                            contentDescription = stringResource(Res.string.sync_disabled),
                            modifier = Modifier.size(smallIconSize)
                        )
                    }
                }

                AnimatedVisibility(editEditFoldersModeEnabled) {
                    Row {
                        TextButton(
                            onClick = {
                                onAction(AccountListAction.OnShowCreateOrUpdateCalendarBottomSheet(principal, homeCollection, calendar))
                            },
                            enabled = calendar.canWriteProperties()
                        ) {
                            Text(stringResource(Res.string.edit))
                        }

                        TextButton(
                            onClick = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            Icon(Icons.Outlined.MoreVert, stringResource(Res.string.more))

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {

                                DropdownMenuItem(
                                    text = {
                                        Crossfade(calendar.calendarSyncStatus?.type == CalendarSyncStatusType.DISABLED) { isDisabled ->
                                            if (isDisabled)
                                                Text(stringResource(Res.string.sync_disabled))
                                            else
                                                Text(stringResource(Res.string.sync_enabled))
                                        }
                                    },
                                    leadingIcon = {
                                        Crossfade(calendar.calendarSyncStatus?.type == CalendarSyncStatusType.DISABLED) { isDisabled ->
                                            if (isDisabled)
                                                Icon(Icons.Outlined.SyncDisabled, stringResource(Res.string.sync_disabled))
                                            else
                                                Icon(Icons.Outlined.Sync, stringResource(Res.string.sync_enabled))
                                        }
                                    },
                                    trailingIcon = {
                                        Switch(
                                            checked = calendar.calendarSyncStatus?.type != CalendarSyncStatusType.DISABLED,
                                            onCheckedChange = { checkedState ->
                                                onAction(AccountListAction.OnToggleSyncEnabled(calendar.id, checkedState))
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    },
                                    onClick = {
                                        if (calendar.calendarSyncStatus?.type == CalendarSyncStatusType.DISABLED)
                                            onAction(AccountListAction.OnToggleSyncEnabled(calendar.id, true))
                                        else
                                            onAction(AccountListAction.OnToggleSyncEnabled(calendar.id, false))
                                    },
                                    enabled = true
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.delete)) },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteForever, null) },
                                    onClick = {
                                        onAction(AccountListAction.OnShowDeleteCalendarDialog(principal, calendar))
                                    },
                                    enabled = homeCollection.canUnbind(),
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.error,
                                        leadingIconColor = MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(!editEditFoldersModeEnabled) {
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
                            contentDescription = stringResource(
                                Res.string.open_foldername,
                                calendar.displayName ?: calendar.url.toString()
                            ),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
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
        editEditFoldersModeEnabled = false,
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
        editEditFoldersModeEnabled = false,
        isFirst = true,
        isLast = true,
        onAction = {}
    )
}

@Preview
@Composable
private fun FolderCard_no_edit_colored_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        CalendarCard(
            principal = Principal.getPrincipalForPreview(),
            homeCollection = HomeCollection.getHomeCollectionForPreview(),
            calendar = Calendar.getCalendarForPreview().copy(color = Color.Yellow),
            editEditFoldersModeEnabled = false,
            isFirst = true,
            isLast = false,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun FolderCard_edit_disabled_Preview() {
    CalendarCard(
        principal = Principal.getPrincipalForPreview(),
        homeCollection = HomeCollection.getHomeCollectionForPreview(),
        calendar = Calendar.getCalendarForPreview().copy(
            calDavPrivileges = listOf(CalDavPrivilege.WRITE),
            calendarSyncStatus = CalendarSyncStatus(type = CalendarSyncStatusType.DISABLED)
        ),
        editEditFoldersModeEnabled = true,
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
        editEditFoldersModeEnabled = true,
        isFirst = false,
        isLast = true,
        onAction = {}
    )
}

@Preview
@Composable
private fun FolderCard_sync_in_progress_edit_Preview() {
    CalendarCard(
        principal = Principal.getPrincipalForPreview(),
        homeCollection = HomeCollection.getHomeCollectionForPreview(),
        calendar = Calendar.getCalendarForPreview().copy(calendarSyncStatus = CalendarSyncStatus(CalendarSyncStatusType.IN_PROGRESS)),
        editEditFoldersModeEnabled = true,
        isFirst = false,
        isLast = false,
        onAction = {}
    )
}


