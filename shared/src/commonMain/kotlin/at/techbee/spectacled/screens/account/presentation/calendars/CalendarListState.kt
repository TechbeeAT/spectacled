package at.techbee.spectacled.screens.account.presentation.calendars

import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal

data class CalendarListState(
    val principals: List<Principal> = emptyList(),
    val homeCollections: List<HomeCollection> = emptyList(),
    val calendars: List<Calendar> = emptyList(),
    val editFoldersOfPrincipal: Principal? = null,
    val snackbarText: String? = null,
    var processingState: ProcessingState = ProcessingState.Idle,

    var showAboutBottomSheet: Boolean = false,
    var showAddPrincipalBottomSheet: Boolean = false,
    var showAddOrUpdateCalendarBottomSheet: CalendarListAction.OnShowCreateOrUpdateCalendarBottomSheet? = null,
    var showDeleteCalendarDialog: CalendarListAction.OnShowDeleteCalendarDialog? = null,
    var showRemovePrincipalDialog: CalendarListAction.OnShowRemovePrincipalDialog? = null,
    var showSyncInfoDialog: CalendarListAction.OnShowSyncInfoDialog? = null,
    var showUpdatePrincipalPasswordBottomSheet: CalendarListAction.OnShowUpdatePrincipalPasswordBottomSheet? = null
)


sealed interface ProcessingState {
    object Idle: ProcessingState
    object Processing: ProcessingState
    data class Success(val message: String): ProcessingState
    data class Error(val message: String, val detail: String? = null): ProcessingState
}