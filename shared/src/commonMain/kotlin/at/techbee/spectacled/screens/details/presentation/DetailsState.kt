package at.techbee.spectacled.screens.details.presentation

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Principal
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime

data class DetailsState @OptIn(ExperimentalTime::class) constructor(
    val icalEntry: IcalEntry = IcalEntry(calendarComponent = CalendarComponent.VJOURNAL),
    val originalIcalEntry: IcalEntry = IcalEntry(calendarComponent = CalendarComponent.VJOURNAL),
    val calendar: Calendar? = null,
    val subtasks: List<IcalEntry> = emptyList(),

    val changeBackstack: List<IcalEntry> = emptyList(),
    val undoRestoreMarker: IcsDateTime? = null,   // lastModified stamp of an in-flight undo/redo-restore save, so saveIcalEntry() can recognize and skip it
    val redoStack: List<IcalEntry> = emptyList(), // states taken back by onUndo(), most recent last
    val redoAnchor: IcsDateTime? = null,          // lastModified of the state produced by the latest undo/redo - any edit after it invalidates the redo stack

    val allColors: List<Color> = emptyList(),
    val allCategories: List<String> = emptyList(),
    val latestUsedTimezones: List<TimeZone> = emptyList(),

    val allCalendars: List<Calendar> = emptyList(),
    val allHomeCollections: List<HomeCollection> = emptyList(),
    val allPrincipals: List<Principal> = emptyList(),

    val showSheetOrDialog: DetailsSheetOrDialog? = null,
    val showDrawingCanvasBottomSheet: DetailsAction.OnShowDrawingCanvasBottomSheet = DetailsAction.OnShowDrawingCanvasBottomSheet(false, null, null),
    val deleteAttachmentUid: String? = null,      // uid of the attachment the delete confirmation dialog asks about
    val deleteSubtaskId: Long? = null,            // uid of the subtask the delete confirmation dialog asks about

    val launchPickerAction: AttachmentPickerAction? = null,      // One-shot signal to the screen to launch a platform picker.

    val isLoading: Boolean = true,
    val isInitialized: Boolean = false,
    val downloadingAttachmentUids: Set<String> = emptySet(),

    val snackbarText: String? = null,
    val navigateUp: Boolean = false,
    val navigateToIcalEntryId: Long? = null,

    val claudeUserApiKey: String? = null
) {

    // Recurring entries are read-only: this app has no recurrence support, so editing one would
    // silently reinterpret or drop its RRULE/RDATE/RECURRENCE-ID and corrupt the series.
    fun allowEditing() = calendar?.canWriteContent() == true
            && !icalEntry.syncState.isDeletedState()
            && !icalEntry.isRecurring()

    // Redo is only possible as long as the entry was not edited since the last undo/redo.
    fun allowRedo() = allowEditing()
            && redoStack.isNotEmpty()
            && redoAnchor != null
            && icalEntry.lastModified == redoAnchor

    fun allowRestore() = calendar?.canWriteContent() == true && icalEntry.syncState.isDeletedState()

}

enum class DetailsSheetOrDialog {
    DELETE, MOVE, COPY, COLOR_SELECTOR, CATEGORY_SELECTOR, JOURNAL_STATUS_PICKER, TASK_STATUS_PICKER, ADD_SUBTASKS, EDIT_URL, EDIT_LOCATION, ADD_ATTACHMENT_URL, ADD_ALARM
}

// Action to perform right after the details screen opens
enum class DetailsInitialAction {
    ADD_DRAWING, ADD_ATTACHMENT, ADD_PHOTO, ADD_FROM_GALLERY, ADD_ATTACHMENT_URL
}

// The three platform pickers the screen can launch on behalf of the ViewModel.
enum class AttachmentPickerAction {
    FILE, PHOTO, GALLERY
}

