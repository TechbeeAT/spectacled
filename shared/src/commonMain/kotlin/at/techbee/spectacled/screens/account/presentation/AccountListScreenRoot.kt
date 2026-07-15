package at.techbee.spectacled.screens.account.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.Route.IcalEntryList
import at.techbee.spectacled.screens.about.presentation.AboutScreen
import at.techbee.spectacled.screens.about.presentation.AboutViewModel
import at.techbee.spectacled.screens.account.presentation.components.AddPrincipalBottomSheet
import at.techbee.spectacled.screens.account.presentation.components.CalendarSyncInfoDialog
import at.techbee.spectacled.screens.account.presentation.components.CreateOrUpdateCalendarBottomSheet
import at.techbee.spectacled.screens.account.presentation.components.DeleteCalendarDialog
import at.techbee.spectacled.screens.account.presentation.components.PrincipalListTopBar
import at.techbee.spectacled.screens.account.presentation.components.RemovePrincipalDialog
import at.techbee.spectacled.screens.account.presentation.components.SettingsBottomSheet
import at.techbee.spectacled.screens.account.presentation.components.UpdatePrincipalPasswordBottomSheet
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.components.CustomBottomSnackbarHost
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_account
import spectacled.shared.generated.resources.close


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreenRoot(
    viewModel: AccountListViewModel,
    onNavigate: (Route) -> Unit,
    keepSafeAreaPaddingValues: Boolean = false,
    modifier: Modifier = Modifier.fillMaxSize()
) {

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val aboutBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addPrincipalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val createCalendarBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val updatePrincipalPasswordBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsBottomSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()


    LaunchedEffect(state.snackbarText) {
        state.snackbarText?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(AccountListAction.OnUpdateSnackbar(null))
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

    LaunchedEffect(state.showSettingsBottomSheet) {
        scope.launch {
            if (state.showSettingsBottomSheet)
                settingsBottomSheet.show()
            else
                settingsBottomSheet.hide()
        }
    }

    state.showRemovePrincipalDialog?.principal?.let { principal ->
        RemovePrincipalDialog(
            principal = principal,
            onDismiss = { viewModel.onAction(AccountListAction.OnShowRemovePrincipalDialog(null)) },
            onConfirm = {
                viewModel.onAction(AccountListAction.OnRemovePrincipal(principal))
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
            onDismiss = { viewModel.onAction(AccountListAction.OnShowDeleteCalendarDialog(null, null)) },
            onConfirm = { principal, calendar ->
                viewModel.onAction(AccountListAction.OnDeleteCalendar(principal, calendar))
            }
        )
    }

    state.showSyncInfoDialog?.let {
        if(it.calendar.calendarSyncStatus?.type?.isErrorType() != true)
            return@let

        CalendarSyncInfoDialog(
            calendarSyncStatus = it.calendar.calendarSyncStatus,
            onRetry = {
                viewModel.onAction(AccountListAction.OnSyncCalendars(listOf(it.calendar)))
            },
            onReloadCalendars = {
                viewModel.onAction(AccountListAction.OnRerunAccountDiscovery(listOf(it.principal)))
            },
            onShowPrincipalUpdatePassword = {
                viewModel.onAction(AccountListAction.OnShowUpdatePrincipalPasswordBottomSheet(it.principal))
            },
            onDismiss = {
                viewModel.onAction(AccountListAction.OnDismissSyncInfoDialog)
            }
        )
    }

    Scaffold(
        topBar = {
            PrincipalListTopBar(onAction = { action -> viewModel.onAction(action) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onAction(AccountListAction.OnShowAddPrincipalBottomSheet(true)) }
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
        modifier = modifier

    ) { paddingValues ->

        val currentPaddingValues = if(keepSafeAreaPaddingValues)
            paddingValues
        else
            PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 0.dp,
                start = 0.dp,
                end = 0.dp
            )

        Box(
            modifier = Modifier
                .padding(currentPaddingValues)
                .consumeWindowInsets(currentPaddingValues)
                .imePadding()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {

            AnimatedVisibility(state.processingState == ProcessingState.Processing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            AccountListScreen(
                state = state,
                onAction = { action ->
                    when (action) {
                        is AccountListAction.OnCalendarClicked -> onNavigate(IcalEntryList(action.calendarId))
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
                onDismiss = { viewModel.onAction(AccountListAction.OnShowAddPrincipalBottomSheet(false)) }
            )
        }

        if (updatePrincipalPasswordBottomSheetState.isVisible && state.showUpdatePrincipalPasswordBottomSheet != null) {
            state.showUpdatePrincipalPasswordBottomSheet?.principal?.let { principal ->
                UpdatePrincipalPasswordBottomSheet(
                    sheetState = updatePrincipalPasswordBottomSheetState,
                    processingState = state.processingState,
                    principal = principal,
                    onAction = { action -> viewModel.onAction(action) },
                    onDismiss = { viewModel.onAction(AccountListAction.OnDismissUpdatePrincipalPasswordBottomSheet) }
                )
            }

        }


        if (aboutBottomSheetState.isVisible) {
            BottomSheetWithMenu(
                sheetState = aboutBottomSheetState,
                onDismiss = { viewModel.onAction(AccountListAction.OnShowAboutBottomSheet(false)) },
                menuActionRight = {
                    TextButton(
                        onClick = { viewModel.onAction(AccountListAction.OnShowAboutBottomSheet(false)) },
                    ) {
                        Text(stringResource(Res.string.close))
                    }
                },
                modifier = Modifier.fillMaxWidth()  // override padding
            ) {
                AboutScreen(koinInject<AboutViewModel>())
            }
        }

        if (settingsBottomSheet.isVisible) {
            SettingsBottomSheet(
                sheetState = settingsBottomSheet,
                userAppPreferencesStore = viewModel.userAppPreferencesStore,
                onDismiss = { viewModel.onAction(AccountListAction.OnShowSettingsBottomSheet(false)) }
            )
        }

        state.showAddOrUpdateCalendarBottomSheet?.let {
            CreateOrUpdateCalendarBottomSheet(
                sheetState = createCalendarBottomSheetState,
                principal = it.principal,
                homeCollection = it.homeCollection,
                calendar = it.calendar,
                processingState = state.processingState,
                recentColors = state.calendars.mapNotNull { calendar ->  calendar.color },
                onCreateOrUpdateCalendar = { principal, homeCollection, calendar ->
                    scope.launch {
                        viewModel.onAction(AccountListAction.OnCreateOrUpdateCalendar(principal, homeCollection, calendar))
                    }
                },
                onDismiss = { viewModel.onAction(AccountListAction.OnDismissCreateOrUpdateCalendarBottomSheet) }
            )
        }
    }
}
