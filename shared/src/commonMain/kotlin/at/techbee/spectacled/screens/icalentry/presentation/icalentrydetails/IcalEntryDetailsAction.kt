package at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.icalentry.domain.SyncState

sealed interface IcalEntryDetailsAction {
    data class OnUpdateSummary(val summary: String): IcalEntryDetailsAction
    data class OnUpdateDescription(val description: String): IcalEntryDetailsAction
    data class OnUpdateCategories(val categories: List<String>): IcalEntryDetailsAction
    data class OnUpdateColor(val color: Color?): IcalEntryDetailsAction
    data class OnUpdateDtStart(val icsDateTime: IcsDateTime): IcalEntryDetailsAction

    data class OnSyncConflictUpdateUserDecision(val syncState: SyncState): IcalEntryDetailsAction

    data class OnShowMoreBottomSheet(val show: Boolean): IcalEntryDetailsAction
    data class OnShowDeleteIcalEntryDialog(val show: Boolean): IcalEntryDetailsAction
    data class OnShowColorSelectorBottomSheet(val show: Boolean): IcalEntryDetailsAction
    data class OnShowCategorySelectorBottomSheet(val show: Boolean): IcalEntryDetailsAction
    data class OnShowTimePickerBottomSheet(val show: Boolean): IcalEntryDetailsAction
    data class OnShowDatePickerBottomSheet(val show: Boolean): IcalEntryDetailsAction
    data class OnShowTimezonePickerBottomSheet(val show: Boolean): IcalEntryDetailsAction

    object OnDeleteIcalEntry: IcalEntryDetailsAction
    object OnCreateCopy: IcalEntryDetailsAction
    object OnDispose: IcalEntryDetailsAction
    object OnRestoreEntry: IcalEntryDetailsAction

    data class OnUpdateSnackbar(val message: String?): IcalEntryDetailsAction
    data class OnNavigateUp(val navigateUp: Boolean): IcalEntryDetailsAction
}