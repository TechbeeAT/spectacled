package at.techbee.spectacled.screens.account.presentation

import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal

data class AccountListState(
    val principals: List<Principal> = emptyList(),
    val homeCollections: List<HomeCollection> = emptyList(),
    val calendars: List<Calendar> = emptyList(),
    val editFoldersOfPrincipal: Principal? = null,
    val snackbarText: String? = null,
    var processingState: ProcessingState = ProcessingState.Idle,

    var showAboutBottomSheet: Boolean = false,
    var showAddPrincipalBottomSheet: Boolean = false,
    var showAddOrUpdateCalendarBottomSheet: AccountListAction.OnShowCreateOrUpdateCalendarBottomSheet? = null,
    var showDeleteCalendarDialog: AccountListAction.OnShowDeleteCalendarDialog? = null,
    var showRemovePrincipalDialog: AccountListAction.OnShowRemovePrincipalDialog? = null,
    var showSyncInfoDialog: AccountListAction.OnShowSyncInfoDialog? = null,
    var showUpdatePrincipalPasswordBottomSheet: AccountListAction.OnShowUpdatePrincipalPasswordBottomSheet? = null,
    var showSettingsBottomSheet: Boolean = false
)


sealed interface ProcessingState {
    object Idle: ProcessingState
    object Processing: ProcessingState
    data class Success(val message: String): ProcessingState
    data class Error(val message: String, val detail: String? = null): ProcessingState
}