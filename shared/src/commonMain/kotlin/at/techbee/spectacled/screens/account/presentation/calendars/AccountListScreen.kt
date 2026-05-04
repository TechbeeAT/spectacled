package at.techbee.spectacled.screens.account.presentation.calendars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.account.presentation.calendars.components.CalendarCard
import at.techbee.spectacled.screens.account.presentation.calendars.components.PrincipalListItem
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import io.ktor.http.Url


@Composable
fun AccountListScreen(
    state: AccountListState,
    onAction: (AccountListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    PullToRefreshBox(
        isRefreshing = state.processingState == ProcessingState.Processing,
        onRefresh = { onAction(AccountListAction.OnRerunAccountDiscovery(state.principals))  }
    ) {

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier.padding(8.dp)
    ) {

        state.principals.forEachIndexed { indexPrincipal, principal ->

            val principalHomeCollections = state.homeCollections.filter { it.principalId == principal.id }

            item {
                PrincipalListItem(
                    principal = principal,
                    homeCollections = principalHomeCollections,
                    calendars = state.calendars.filter { calendar -> principalHomeCollections.any { homeCollection -> homeCollection.id == calendar.homeCollectionId } },
                    folderEditEnabled = state.editFoldersOfPrincipal?.id == principal.id,
                    onAction = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 8.dp,
                            top = if (indexPrincipal != 0) 24.dp else 0.dp
                        )
                        .animateItem()
                )
            }

            principalHomeCollections.forEach { homeCollection ->
                val calendars = state.calendars.filter { it.homeCollectionId == homeCollection.id }
                if (calendars.isEmpty()) {
                    item {
                        Text(
                            text = "No compatible folders/calendars found",
                            fontStyle = FontStyle.Italic
                        )

                        if (homeCollection.canBind()) {
                            TextButton(
                                onClick = {
                                    onAction(
                                        AccountListAction.OnShowCreateOrUpdateCalendarBottomSheet(
                                            principal,
                                            homeCollection,
                                            Calendar.getNewCalendar(homeCollection)
                                        )
                                    )
                                }
                            ) {
                                Text("Add folder/calendar")
                            }
                        } else {
                            Text(
                                text = "Insufficient access rights to create a new folder/calendar.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                itemsIndexed(calendars, key = { _, calendar -> calendar.id }) { indexCalendar, calendar ->

                    CalendarCard(
                        principal = principal,
                        homeCollection = homeCollection,
                        calendar = calendar,
                        editEditFoldersModeEnabled = state.editFoldersOfPrincipal?.id == principal.id,
                        isFirst = indexCalendar == 0,
                        isLast = indexCalendar == calendars.lastIndex,
                        onAction = onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }
            }
        }

        item {
            TextButton(
                onClick = {
                    onAction(AccountListAction.OnAddLocalCalendar)
                }
            ) {
                Text("Add collection")
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
}



@Preview
@Composable
private fun FolderListScreen_edit_off_Preview() {
    AccountListScreen(
        state = AccountListState().copy(
            principals = listOf(
                Principal.getPrincipalForPreview().copy(
                    principalUrl = Url("https://example.com"),
                    displayName = "My Account"
                ),
                Principal.getPrincipalForPreview().copy(
                    principalUrl = Url("https://example2.com"),
                    displayName = "My Account2"
                ),
            ),
            homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
            calendars = listOf(Calendar.getCalendarForPreview())
        ),
        onAction = { }
    )
}

@Preview
@Composable
private fun FolderListScreen_edit_on_Preview() {
    AccountListScreen(
        state = AccountListState().copy(
            principals = listOf(
                Principal.getPrincipalForPreview().copy(
                    principalUrl = Url("https://example.com"),
                    displayName = "My Account"
                ),
            ),
            homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
            calendars = listOf(Calendar.getCalendarForPreview()),
            editFoldersOfPrincipal = Principal.getPrincipalForPreview()
        ),
        onAction = { }
    )
}

@Preview
@Composable
private fun FolderListScreen_edit_off_empty_without_rights_Preview() {
    AccountListScreen(
        state = AccountListState().copy(
            principals = listOf(
                Principal.getPrincipalForPreview().copy(
                    principalUrl = Url("https://example.com"),
                    displayName = "My Account"
                ),
            ),
            homeCollections = listOf(HomeCollection.getHomeCollectionForPreview())
        ),
        onAction = { }
    )
}

@Preview
@Composable
private fun FolderListScreen_edit_off_empty_with_rights_Preview() {
    AccountListScreen(
        state = AccountListState().copy(
            principals = listOf(
                Principal.getPrincipalForPreview().copy(
                    principalUrl = Url("https://example.com"),
                    displayName = "My Account"
                ),
            ),
            homeCollections = listOf(HomeCollection.getHomeCollectionForPreview().copy(
                calDavPrivileges = listOf(CalDavPrivilege.WRITE)
            )),
        ),
        onAction = { }
    )
}