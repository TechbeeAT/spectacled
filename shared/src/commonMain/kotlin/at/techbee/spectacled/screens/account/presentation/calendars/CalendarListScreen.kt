package at.techbee.spectacled.screens.account.presentation.calendars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.Route.NoteList
import at.techbee.spectacled.screens.about.presentation.AboutBottomSheet
import at.techbee.spectacled.screens.account.presentation.calendars.components.AddPrincipalBottomSheet
import at.techbee.spectacled.screens.account.presentation.calendars.components.CalendarCard
import at.techbee.spectacled.screens.account.presentation.calendars.components.CalendarSyncInfoDialog
import at.techbee.spectacled.screens.account.presentation.calendars.components.CreateOrUpdateCalendarBottomSheet
import at.techbee.spectacled.screens.account.presentation.calendars.components.DeleteCalendarDialog
import at.techbee.spectacled.screens.account.presentation.calendars.components.PrincipalListItem
import at.techbee.spectacled.screens.account.presentation.calendars.components.PrincipalListTopBar
import at.techbee.spectacled.screens.account.presentation.calendars.components.RemovePrincipalDialog
import at.techbee.spectacled.screens.account.presentation.calendars.components.UpdatePrincipalPasswordBottomSheet
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.CustomBottomSnackbarHost
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_account
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarListScreenRoot(
    viewModel: CalendarListViewModel,
    onNavigate: (Route) -> Unit
) {

    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }
    val aboutBottomSheetState = rememberModalBottomSheetState()
    val addPrincipalBottomSheetState = rememberModalBottomSheetState()
    val createCalendarBottomSheetState = rememberModalBottomSheetState()
    val updatePrincipalPasswordBottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()


    LaunchedEffect(state.snackbarText) {
        state.snackbarText?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(CalendarListAction.OnUpdateSnackbar(null))
        }
    }

    LaunchedEffect(state.showAboutBottomSheet) {
        scope.launch {
            if(state.showAboutBottomSheet) aboutBottomSheetState.show() else aboutBottomSheetState.hide()
        }
    }

    LaunchedEffect(state.showAddPrincipalBottomSheet) {
        scope.launch {
            if(state.showAddPrincipalBottomSheet) addPrincipalBottomSheetState.show() else addPrincipalBottomSheetState.hide()
        }
    }

    LaunchedEffect(state.showUpdatePrincipalPasswordBottomSheet) {
        scope.launch {
            if(state.showUpdatePrincipalPasswordBottomSheet != null) updatePrincipalPasswordBottomSheetState.show() else updatePrincipalPasswordBottomSheetState.hide()
        }
    }

    LaunchedEffect(state.showAddOrUpdateCalendarBottomSheet) {
        scope.launch {
            if (state.showAddOrUpdateCalendarBottomSheet != null)
                createCalendarBottomSheetState.show()
            else
                createCalendarBottomSheetState.hide()
        }
    }

    state.showRemovePrincipalDialog?.principal?.let { principal ->
        RemovePrincipalDialog(
            principal = principal,
            onDismiss = { viewModel.onAction(CalendarListAction.OnShowRemovePrincipalDialog(null)) },
            onConfirm = {
                viewModel.onAction(CalendarListAction.OnRemovePrincipal(principal))
            }
        )
    }

    state.showDeleteCalendarDialog?.let {
        if(it.calendar == null || it.principal == null)
            return@let

        DeleteCalendarDialog(
            principal = it.principal,
            calendar = it.calendar,
            processingState = state.processingState,
            onDismiss = { viewModel.onAction(CalendarListAction.OnShowDeleteCalendarDialog(null, null)) },
            onConfirm = { principal, calendar ->
                viewModel.onAction(CalendarListAction.OnDeleteCalendar(principal, calendar))
            }
        )
    }

    state.showSyncInfoDialog?.let {
        if(it.calendar.calendarSyncStatus?.type?.isErrorType() != true)
            return@let

        CalendarSyncInfoDialog(
            calendarSyncStatus = it.calendar.calendarSyncStatus,
            onRetry = {
                viewModel.onAction(CalendarListAction.OnSyncCalendars(listOf(it.calendar)))
            },
            onReloadCalendars = {
                viewModel.onAction(CalendarListAction.OnRerunAccountDiscovery(listOf(it.principal)))
            },
            onShowPrincipalUpdatePassword = {
                viewModel.onAction(CalendarListAction.OnShowUpdatePrincipalPasswordBottomSheet(it.principal))
            },
            onDismiss = {
                viewModel.onAction(CalendarListAction.OnDismissSyncInfoDialog)
            }
        )
    }

    Scaffold(
        topBar = {
            PrincipalListTopBar(
                onAction = { action -> viewModel.onAction(action) },
                state = state
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onAction(CalendarListAction.OnShowAddPrincipalBottomSheet(true)) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GroupAdd,
                        contentDescription = stringResource(Res.string.add_account)
                    )
                    Text(stringResource(Res.string.add_account))
                }
            }
        },
        modifier = Modifier.fillMaxSize()
        /*

        bottomBar = {
            BottomAppBar(
                actions = {

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (getPlatform().isIos()) 0.dp else 8.dp)
                    ) {
                        IconButton(
                            onClick = { showAboutDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(Res.string.about),
                                tint = if (getPlatform().isIos()) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }

                        Text(
                            text = "${state.notes.size} Notes",
                            style = MaterialTheme.typography.labelLarge
                        )

                        if(getPlatform().isIos())
                            IconButton(
                                onClick = {
                                    onNavigate(Route.AddNote)
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    stringResource(Res.string.add_note),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        else
                            FloatingActionButton(
                                onClick = {
                                    onNavigate(Route.AddNote)
                                }
                            ) {
                                Icon(Icons.Outlined.Add, stringResource(Res.string.add_note))
                            }
                    }
                },
                floatingActionButton = {

                }
            )

        },

         */

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 0.dp)
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {

            AnimatedVisibility(state.processingState == ProcessingState.Processing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            CalendarListScreen(
                state = state,
                onAction = { action ->
                    when (action) {
                        is CalendarListAction.OnCalendarClicked -> onNavigate(NoteList(action.calendarId))
                        else -> viewModel.onAction(action)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            CustomBottomSnackbarHost(
                snackbarHostState = snackbarHostState,
                keepSpaceForFAB = true
            )
        }


        if (addPrincipalBottomSheetState.isVisible) {
            AddPrincipalBottomSheet(
                sheetState = addPrincipalBottomSheetState,
                processingState = state.processingState,
                onAction = { viewModel.onAction(it) },
                onDismiss = { viewModel.onAction(CalendarListAction.OnShowAddPrincipalBottomSheet(false)) }
            )
        }

        if (updatePrincipalPasswordBottomSheetState.isVisible && state.showUpdatePrincipalPasswordBottomSheet != null) {
            state.showUpdatePrincipalPasswordBottomSheet?.principal?.let { principal ->
                UpdatePrincipalPasswordBottomSheet(
                    sheetState = updatePrincipalPasswordBottomSheetState,
                    processingState = state.processingState,
                    principal = principal,
                    onAction = { action -> viewModel.onAction(action) },
                    onDismiss = { viewModel.onAction(CalendarListAction.OnDismissUpdatePrincipalPasswordBottomSheet) }
                )
            }

        }


        if (aboutBottomSheetState.isVisible) {
            AboutBottomSheet(
                onDismiss = { viewModel.onAction(CalendarListAction.OnShowAboutBottomSheet(false)) },
                sheetState = aboutBottomSheetState
            )
        }

        viewModel.state.showAddOrUpdateCalendarBottomSheet?.let {
            CreateOrUpdateCalendarBottomSheet(
                sheetState = createCalendarBottomSheetState,
                principal = it.principal,
                homeCollection = it.homeCollection,
                calendar = it.calendar,
                processingState = state.processingState,
                recentColors = state.calendars.mapNotNull { calendar ->  calendar.color },
                onCreateOrUpdateCalendar = { principal, homeCollection, calendar ->
                    scope.launch {
                        viewModel.onAction(CalendarListAction.OnCreateOrUpdateCalendar(principal, homeCollection, calendar))
                    }
                },
                onDismiss = { viewModel.onAction(CalendarListAction.OnDismissCreateOrUpdateCalendarBottomSheet) }
            )
        }
    }
}


@Composable
fun CalendarListScreen(
    state: CalendarListState,
    onAction: (CalendarListAction) -> Unit,
    modifier: Modifier = Modifier
) {

    PullToRefreshBox(
        isRefreshing = state.processingState == ProcessingState.Processing,
        onRefresh = { onAction(CalendarListAction.OnRerunAccountDiscovery(state.principals))  }
    ) {

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.padding(8.dp)
    ) {

        state.principals.forEachIndexed { indexPrincipal, principal ->

            val principalHomeCollections = state.homeCollections.filter { it.principalId == principal.id }

            item {
                PrincipalListItem(
                    principal = principal,
                    homeCollections = principalHomeCollections,
                    calendars = state.calendars.filter { calendar -> principalHomeCollections.any { homeCollection -> homeCollection.id == calendar.homeCollectionId } },
                    isEditMode = state.isEditMode,
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
                                        CalendarListAction.OnShowCreateOrUpdateCalendarBottomSheet(
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
                        isEditMode = state.isEditMode,
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
                    onAction(CalendarListAction.OnAddLocalCalendar)
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
    CalendarListScreen(
        state = CalendarListState().copy(
            principals = listOf(
                Principal.getPrincipalForPreview().copy(
                    principalUrl = Url("https://example.com"),
                    displayName = "My Account"
                ),
            ),
            homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
            calendars = listOf(Calendar.getCalendarForPreview()),
            isEditMode = false
        ),
        onAction = { }
    )
}

@Preview
@Composable
private fun FolderListScreen_edit_on_Preview() {
    CalendarListScreen(
        state = CalendarListState().copy(
            principals = listOf(
                Principal.getPrincipalForPreview().copy(
                    principalUrl = Url("https://example.com"),
                    displayName = "My Account"
                ),
            ),
            homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
            calendars = listOf(Calendar.getCalendarForPreview()),
            isEditMode = true
        ),
        onAction = { }
    )
}

@Preview
@Composable
private fun FolderListScreen_edit_off_empty_without_rights_Preview() {
    CalendarListScreen(
        state = CalendarListState().copy(
            principals = listOf(
                Principal.getPrincipalForPreview().copy(
                    principalUrl = Url("https://example.com"),
                    displayName = "My Account"
                ),
            ),
            homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
            isEditMode = false
        ),
        onAction = { }
    )
}

@Preview
@Composable
private fun FolderListScreen_edit_off_empty_with_rights_Preview() {
    CalendarListScreen(
        state = CalendarListState().copy(
            principals = listOf(
                Principal.getPrincipalForPreview().copy(
                    principalUrl = Url("https://example.com"),
                    displayName = "My Account"
                ),
            ),
            homeCollections = listOf(HomeCollection.getHomeCollectionForPreview().copy(
                calDavPrivileges = listOf(CalDavPrivilege.WRITE)
            )),
            isEditMode = false
        ),
        onAction = { }
    )
}