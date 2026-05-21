package at.techbee.spectacled.screens.account.presentation

import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal

sealed interface AccountListAction {
    object OnAddLocalCalendar: AccountListAction

    data class OnShowAboutBottomSheet(val show: Boolean? = null): AccountListAction
    data class OnShowAddPrincipalBottomSheet(val show: Boolean? = null): AccountListAction

    data class OnShowCreateOrUpdateCalendarBottomSheet(val principal: Principal, val homeCollection: HomeCollection, val calendar: Calendar): AccountListAction
    object OnDismissCreateOrUpdateCalendarBottomSheet: AccountListAction

    data class OnShowRemovePrincipalDialog(val principal: Principal?): AccountListAction
    data class OnShowDeleteCalendarDialog(val principal: Principal?, val calendar: Calendar?): AccountListAction

    data class OnShowUpdatePrincipalPasswordBottomSheet(val principal: Principal): AccountListAction
    object OnDismissUpdatePrincipalPasswordBottomSheet: AccountListAction

    data class OnShowSyncInfoDialog(val principal: Principal, val calendar: Calendar): AccountListAction
    object OnDismissSyncInfoDialog: AccountListAction

    data class OnSyncCalendars(val calendars: List<Calendar>): AccountListAction
    data class OnRerunAccountDiscovery(val principals: List<Principal>): AccountListAction

    data class OnRemovePrincipal(val principal: Principal): AccountListAction
    data class OnDeleteCalendar(val principal: Principal, val calendar: Calendar): AccountListAction
    data class OnCreateOrUpdateCalendar(val principal: Principal, val homeCollection: HomeCollection, val calendar: Calendar): AccountListAction

    data class OnCalendarClicked(val calendarId: Long): AccountListAction
    data class OnAddPrincipal(val credentials: Credentials, val useDav5Jvm: Boolean = false): AccountListAction
    data class OnUpdatePrincipalPassword(val principal: Principal, val newPassword: String): AccountListAction

    data class OnEditAccountFolders(val principal: Principal?): AccountListAction

    data class OnUpdateSnackbar(val message: String?): AccountListAction
}