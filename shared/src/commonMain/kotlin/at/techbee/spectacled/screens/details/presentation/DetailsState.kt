package at.techbee.spectacled.screens.details.presentation

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.IcalEntry
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

    val showDeleteDialog: Boolean = false,
    val showMoreBottomSheet: Boolean = false,
    val showColorSelectorBottomSheet: Boolean = false,
    val showCategorySelectorBottomSheet: Boolean = false,
    val showJournalStatusPickerBottomSheet: Boolean = false,
    val showTaskStatusProgressPickerBottomSheet: Boolean = false,
    val showAddSubtaskBottomSheet: Boolean = false,

    val isLoading: Boolean = true,

    val snackbarText: String? = null,
    val navigateUp: Boolean = false,
    val navigateToIcalEntryId: Long? = null
) {

    fun allowEditing() = calendar?.canWriteContent() == true && !icalEntry.syncState.isDeletedState()

    fun allowRestore() = calendar?.canWriteContent() == true && icalEntry.syncState.isDeletedState()
}