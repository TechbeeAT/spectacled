package at.techbee.spectacled.screens.details.presentation

import androidx.compose.ui.graphics.Color
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

    val allColors: List<Color> = emptyList(),
    val allCategories: List<String> = emptyList(),
    val latestUsedTimezones: List<TimeZone> = emptyList(),

    val allCalendars: List<Calendar> = emptyList(),
    val allHomeCollections: List<HomeCollection> = emptyList(),
    val allPrincipals: List<Principal> = emptyList(),

    /*
    val showDeleteDialog: Boolean = false,
    val showMoveDialog: Boolean = false,
    val showMoreBottomSheet: Boolean = false,
    val showColorSelectorBottomSheet: Boolean = false,
    val showCategorySelectorBottomSheet: Boolean = false,
    val showJournalStatusPickerBottomSheet: Boolean = false,
    val showTaskStatusProgressPickerBottomSheet: Boolean = false,
    val showAddSubtaskBottomSheet: Boolean = false,
    val showEditUrlBottomSheet: Boolean = false,

     */

    val showSheetOrDialog: DetailsSheetOrDialog? = null,
    val showDrawingCanvasBottomSheet: DetailsAction.OnShowDrawingCanvasBottomSheet = DetailsAction.OnShowDrawingCanvasBottomSheet(false, null, null),

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

    fun allowRestore() = calendar?.canWriteContent() == true && icalEntry.syncState.isDeletedState()

    fun isAttachmentSupportEnabled() = calendar?.isAttachmentSyncSupported() == true
}

enum class DetailsSheetOrDialog {
    DELETE, MOVE, MORE, COLOR_SELECTOR, CATEGORY_SELECTOR, JOURNAL_STATUS_PICKER, TASK_STATUS_PICKER, ADD_SUBTASKS, EDIT_URL, ADD_DRAWING
}

/*
enum class DetailsInitialAction {
    DELETE, MOVE, MORE, COLOR_SELECTOR, CATEGORY_SELECTOR, JOURNAL_STATUS_PICKER, TASK_STATUS_PICKER, ADD_SUBTASKS, EDIT_URL, ADD_DRAWING, ADD_ATTACHMENT, ADD_FROM_GALLERY, ADD_PHOTO
}

 */

