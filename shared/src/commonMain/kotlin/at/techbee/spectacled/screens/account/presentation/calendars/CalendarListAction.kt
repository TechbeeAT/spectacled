package at.techbee.spectacled.screens.account.presentation.calendars

import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal

sealed interface CalendarListAction {
    object OnAddLocalCalendar: CalendarListAction

    data class OnShowAboutBottomSheet(val show: Boolean? = null): CalendarListAction
    data class OnShowAddPrincipalBottomSheet(val show: Boolean? = null): CalendarListAction

    data class OnShowCreateOrUpdateCalendarBottomSheet(val principal: Principal, val homeCollection: HomeCollection, val calendar: Calendar): CalendarListAction
    object OnDismissCreateOrUpdateCalendarBottomSheet: CalendarListAction

    data class OnShowRemovePrincipalDialog(val principal: Principal?): CalendarListAction
    data class OnShowDeleteCalendarDialog(val principal: Principal?, val calendar: Calendar?): CalendarListAction

    data class OnShowUpdatePrincipalPasswordBottomSheet(val principal: Principal): CalendarListAction
    object OnDismissUpdatePrincipalPasswordBottomSheet: CalendarListAction

    data class OnShowSyncInfoDialog(val principal: Principal, val calendar: Calendar): CalendarListAction
    object OnDismissSyncInfoDialog: CalendarListAction

    data class OnSyncCalendars(val calendars: List<Calendar>): CalendarListAction
    data class OnRerunAccountDiscovery(val principals: List<Principal>): CalendarListAction

    data class OnRemovePrincipal(val principal: Principal): CalendarListAction
    data class OnDeleteCalendar(val principal: Principal, val calendar: Calendar): CalendarListAction
    data class OnCreateOrUpdateCalendar(val principal: Principal, val homeCollection: HomeCollection, val calendar: Calendar): CalendarListAction

    data class OnCalendarClicked(val calendarId: Long): CalendarListAction
    data class OnAddPrincipal(val credentials: Credentials, val useDav5Jvm: Boolean = false): CalendarListAction
    data class OnUpdatePrincipalPassword(val principal: Principal, val newPassword: String): CalendarListAction

    object OnToggleEditMode: CalendarListAction

    data class OnUpdateSnackbar(val message: String?): CalendarListAction
}