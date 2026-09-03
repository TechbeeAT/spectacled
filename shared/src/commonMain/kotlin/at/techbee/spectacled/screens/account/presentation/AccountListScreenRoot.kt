package at.techbee.spectacled.screens.account.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
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
import at.techbee.spectacled.screens.account.presentation.components.UpdatePrincipalPasswordBottomSheet
import at.techbee.spectacled.screens.account.presentation.components.settings.SettingsBottomSheet
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.components.CustomBottomSnackbarHost
import at.techbee.spectacled.screens.core.presentation.imeAwarePadding
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
    removeSafeAreaPaddingValues: Boolean = false,
    modifier: Modifier = Modifier.fillMaxSize()
) {

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    LaunchedEffect(state.snackbarText) {
        state.snackbarText?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(AccountListAction.OnUpdateSnackbar(null))
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
            PrincipalListTopBar(
                removeHorizontalWindowInsets = removeSafeAreaPaddingValues,
                onAction = { action -> viewModel.onAction(action) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onAction(AccountListAction.OnShowAddPrincipalBottomSheet(true)) },
                // Lift the FAB by the bottom safe-area inset so it clears the iOS home indicator
                // (the content still fills to the edge, only the FAB is padded).
                modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
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
        // Drop the bottom safe-area inset so the content fills to the screen edge and there
        // is no empty strip (the "white gap") over the iOS home indicator. In landscape we
        // additionally drop the horizontal insets so the content fills the sides; the top bar
        // keeps its own vertical insets in either case.
        contentWindowInsets = if (removeSafeAreaPaddingValues)
            ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top)
        else
            ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        modifier = modifier

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imeAwarePadding()
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

            CustomBottomSnackbarHost(snackbarHostState = snackbarHostState)
        }


        // Every sheet below is gated on the ViewModel state, never on its SheetState: the sheet
        // states start out expanded (see rememberExpandedSheetState), so gating on their
        // visibility would pop all of them open on the very first frame.
        if (state.showAddPrincipalBottomSheet) {
            AddPrincipalBottomSheet(
                sheetState = rememberExpandedSheetState(),
                processingState = state.processingState,
                isFirstAccount = state.principals.isEmpty(),
                onAction = { viewModel.onAction(it) },
                onDismiss = { viewModel.onAction(AccountListAction.OnShowAddPrincipalBottomSheet(false)) }
            )
        }

        state.showUpdatePrincipalPasswordBottomSheet?.principal?.let { principal ->
            UpdatePrincipalPasswordBottomSheet(
                sheetState = rememberExpandedSheetState(),
                processingState = state.processingState,
                principal = principal,
                onAction = { action -> viewModel.onAction(action) },
                onDismiss = { viewModel.onAction(AccountListAction.OnDismissUpdatePrincipalPasswordBottomSheet) }
            )
        }


        if (state.showAboutBottomSheet) {
            BottomSheetWithMenu(
                sheetState = rememberExpandedSheetState(),
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

        if (state.showSettingsBottomSheet) {
            SettingsBottomSheet(
                sheetState = rememberExpandedSheetState(),
                userAppPreferencesStore = viewModel.userAppPreferencesStore,
                onDismiss = { viewModel.onAction(AccountListAction.OnShowSettingsBottomSheet(false)) }
            )
        }

        state.showAddOrUpdateCalendarBottomSheet?.let {
            CreateOrUpdateCalendarBottomSheet(
                sheetState = rememberExpandedSheetState(),
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


/**
 * A sheet state that starts out expanded, so the sheet is fully shown as soon as it enters
 * composition, and that skips the partially expanded detent.
 *
 * Created per sheet inside the block that shows it: a sheet state hoisted across open/close cycles
 * would keep the [SheetValue.Hidden] left behind by a swipe-dismissal and reopen the sheet invisibly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberExpandedSheetState() = rememberBottomSheetState(
    initialValue = SheetValue.Expanded,
    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
)
