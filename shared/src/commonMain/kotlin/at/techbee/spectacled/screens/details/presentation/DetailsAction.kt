package at.techbee.spectacled.screens.details.presentation

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import io.ktor.http.Url

sealed interface DetailsAction {
    data class OnUpdateSummary(val summary: String): DetailsAction
    data class OnUpdateDescription(val description: String): DetailsAction
    data class OnUpdateCategories(val addCategory: String?, val removeCategory: String?): DetailsAction
    data class OnPin(val pin: Boolean): DetailsAction
    data class OnUpdateColor(val color: Color?): DetailsAction
    data class OnUpdateStatus(val status: Status?): DetailsAction
    data class OnUpdateDtStart(val icsDateTime: IcsDateTime?): DetailsAction
    data class OnUpdateDue(val icsDateTime: IcsDateTime?): DetailsAction
    data class OnUpdateProgress(val percent: Long): DetailsAction
    data class OnUpdateSubtaskProgress(val percent: Long, val subtaskIcalEntryId: Long): DetailsAction
    data class OnAddSubtask(val summary: String): DetailsAction
    data class OnUpdateUrl(val url: Url?): DetailsAction
    data class OnAddAttachment(val fileName: String, val bytes: ByteArray, val mimeType: String?): DetailsAction
    data class OnOpenAttachment(val attachmentId: Long): DetailsAction
    data class OnDeleteAttachment(val attachmentId: Long): DetailsAction

    data class OnNewCalendarIdSelected(val calendarId: Long): DetailsAction

    data class OnSyncConflictUpdateUserDecision(val syncState: SyncState): DetailsAction

    data class OnShowMoreBottomSheet(val show: Boolean): DetailsAction
    data class OnShowDeleteDialog(val show: Boolean): DetailsAction
    data class OnShowColorSelectorBottomSheet(val show: Boolean): DetailsAction
    data class OnShowCategorySelectorBottomSheet(val show: Boolean): DetailsAction
    data class OnShowJournalStatusPickerBottomSheet(val show: Boolean): DetailsAction
    data class OnShowTaskStatusProgressPickerBottomSheet(val show: Boolean): DetailsAction
    data class OnShowAddSubtaskBottomSheet(val show: Boolean): DetailsAction
    data class OnShowEditUrlBottomSheet(val show: Boolean): DetailsAction
    data class OnShowDrawingCanvasBottomSheet(val show: Boolean): DetailsAction

    data class OnPersistOrderNo(val list: List<Long>): DetailsAction

    data class OnNavigateToIcalEntryId(val id: Long?): DetailsAction

    object OnProcessWithAI: DetailsAction

    object OnDelete: DetailsAction
    object OnCreateCopy: DetailsAction
    object OnDispose: DetailsAction
    object OnRestoreEntry: DetailsAction
    object OnShare: DetailsAction

    data class OnUpdateSnackbar(val message: String?): DetailsAction
    data class OnNavigateUp(val navigateUp: Boolean): DetailsAction
}