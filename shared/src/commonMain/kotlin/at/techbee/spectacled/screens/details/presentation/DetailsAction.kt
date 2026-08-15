package at.techbee.spectacled.screens.details.presentation

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.presentation.components.PathData
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
    data class OnAddUrlAttachment(val url: Url): DetailsAction
    data class OnOpenAttachment(val attachmentUid: String): DetailsAction
    data class OnDeleteAttachment(val attachmentUid: String): DetailsAction
    data class OnUpdateDrawing(val replaceAttachmentUid: String?, val paths: List<PathData>, val width: Float, val height: Float): DetailsAction

    data class OnNewCalendarIdSelected(val calendarId: Long): DetailsAction

    data class OnSyncConflictUpdateUserDecision(val syncState: SyncState): DetailsAction

    data class OnShowSheetOrDialog(val sheetOrDialog: DetailsSheetOrDialog?): DetailsAction
    data class OnShowDrawingCanvasBottomSheet(val show: Boolean, val replaceAttachmentUid: String?, val initialPaths: List<PathData>?): DetailsAction
    data class OnLaunchPicker(val pickerAction: AttachmentPickerAction?): DetailsAction

    data class OnPersistOrderNo(val list: List<Long>): DetailsAction

    data class OnNavigateToIcalEntryId(val id: Long?): DetailsAction

    object OnProcessWithAI: DetailsAction

    object OnDelete: DetailsAction
    data class OnMove(val newCalendarId: Long): DetailsAction
    object OnCreateCopy: DetailsAction
    object OnDispose: DetailsAction
    object OnRestoreEntry: DetailsAction
    object OnShare: DetailsAction

    data class OnUpdateSnackbar(val message: String?): DetailsAction
    data class OnNavigateUp(val navigateUp: Boolean): DetailsAction
}