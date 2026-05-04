package at.techbee.spectacled.screens.details.presentation

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.SyncState

sealed interface DetailsAction {
    data class OnUpdateSummary(val summary: String): DetailsAction
    data class OnUpdateDescription(val description: String): DetailsAction
    data class OnUpdateCategories(val addCategory: String?, val removeCategory: String?): DetailsAction
    data class OnPin(val pin: Boolean): DetailsAction
    data class OnUpdateColor(val color: Color?): DetailsAction
    data class OnUpdateDtStart(val icsDateTime: IcsDateTime): DetailsAction

    data class OnSyncConflictUpdateUserDecision(val syncState: SyncState): DetailsAction

    data class OnShowMoreBottomSheet(val show: Boolean): DetailsAction
    data class OnShowDeleteDialog(val show: Boolean): DetailsAction
    data class OnShowColorSelectorBottomSheet(val show: Boolean): DetailsAction
    data class OnShowCategorySelectorBottomSheet(val show: Boolean): DetailsAction
    data class OnShowTimePickerBottomSheet(val show: Boolean): DetailsAction
    data class OnShowDatePickerBottomSheet(val show: Boolean): DetailsAction
    data class OnShowTimezonePickerBottomSheet(val show: Boolean): DetailsAction

    object OnDelete: DetailsAction
    object OnCreateCopy: DetailsAction
    object OnDispose: DetailsAction
    object OnRestoreEntry: DetailsAction
    object OnShare: DetailsAction

    data class OnUpdateSnackbar(val message: String?): DetailsAction
    data class OnNavigateUp(val navigateUp: Boolean): DetailsAction
}