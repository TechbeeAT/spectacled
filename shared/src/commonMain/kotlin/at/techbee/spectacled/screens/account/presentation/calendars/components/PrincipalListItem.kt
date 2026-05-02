package at.techbee.spectacled.screens.account.presentation.calendars.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.GroupRemove
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.account.presentation.calendars.CalendarListAction
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.create_folder
import spectacled.shared.generated.resources.done
import spectacled.shared.generated.resources.edit_folders
import spectacled.shared.generated.resources.ic_folder_managed
import spectacled.shared.generated.resources.ic_folder_match
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.refresh_all
import spectacled.shared.generated.resources.remove_account

@Composable
fun PrincipalListItem(
    principal: Principal,
    homeCollections: List<HomeCollection>,
    calendars: List<Calendar>,
    folderEditEnabled: Boolean,
    onAction: (CalendarListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    var iCalCollectionMenuExpanded by rememberSaveable { mutableStateOf(false) }


    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.heightIn(min = 48.dp).weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = principal.displayName ?: principal.principalUrl.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Crossfade(folderEditEnabled) { enabled ->
            if (enabled) {
                TextButton(
                    onClick = { onAction(CalendarListAction.OnEditAccountFolders(null)) }
                ) {
                    Text(stringResource(Res.string.done))
                }
            } else {
                TextButton(
                    onClick = { iCalCollectionMenuExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(Res.string.more),
                    )

                    DropdownMenu(
                        expanded = iCalCollectionMenuExpanded,
                        onDismissRequest = { iCalCollectionMenuExpanded = false }
                    ) {

                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.create_folder)) },
                            onClick = {
                                iCalCollectionMenuExpanded = false

                                onAction(CalendarListAction.OnShowCreateOrUpdateCalendarBottomSheet(
                                    principal = principal,
                                    homeCollection = homeCollections.first(),
                                    calendar = Calendar.getNewCalendar(homeCollections.first())
                                ))
                            },
                            leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.edit_folders)) },
                            onClick = {
                                iCalCollectionMenuExpanded = false

                                onAction(CalendarListAction.OnEditAccountFolders(principal = principal))
                            },
                            leadingIcon = { Icon(painterResource(Res.drawable.ic_folder_managed), null) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(8.dp))


                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.refresh_all)) },
                            onClick = {
                                iCalCollectionMenuExpanded = false
                                onAction(CalendarListAction.OnSyncCalendars(calendars))
                            },
                            leadingIcon = { Icon(Icons.Outlined.Sync, null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Reload folders") },
                            onClick = {
                                iCalCollectionMenuExpanded = false
                                onAction(CalendarListAction.OnRerunAccountDiscovery(listOf(principal)))
                            },
                            leadingIcon = { Icon(painterResource(Res.drawable.ic_folder_match), null) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(8.dp))


                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.remove_account)) },
                            onClick = {
                                iCalCollectionMenuExpanded = false
                                onAction(CalendarListAction.OnShowRemovePrincipalDialog(principal))
                            },
                            leadingIcon = { Icon(Icons.Outlined.GroupRemove, null) }
                        )


                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PrincipalListItem_Preview() {
    PrincipalListItem(
        principal = Principal.getPrincipalForPreview(),
        homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
        calendars = listOf(Calendar.getCalendarForPreview()),
        folderEditEnabled = false,
        onAction = {}
    )
}

@Preview
@Composable
private fun PrincipalListItem_folder_edit_Preview() {
    PrincipalListItem(
        principal = Principal.getPrincipalForPreview(),
        homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
        calendars = listOf(Calendar.getCalendarForPreview()),
        folderEditEnabled = true,
        onAction = {}
    )
}

