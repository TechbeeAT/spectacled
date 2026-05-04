package at.techbee.spectacled.screens.details.presentation

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.IcalEntry
import kotlin.time.ExperimentalTime

data class DetailsState @OptIn(ExperimentalTime::class) constructor(
    val icalEntry: IcalEntry = IcalEntry(),
    val originalIcalEntry: IcalEntry = IcalEntry(),
    val calendar: Calendar? = null,

    val allColors: List<Color> = emptyList(),
    val allCategories: List<String> = emptyList(),

    val showDeleteDialog: Boolean = false,
    val showMoreBottomSheet: Boolean = false,
    val showColorSelectorBottomSheet: Boolean = false,
    val showCategorySelectorBottomSheet: Boolean = false,
    val showTimePickerBottomSheet: Boolean = false,
    val showDatePickerBottomSheet: Boolean = false,
    val showTimezonePickerBottomSheet: Boolean = false,

    val isLoading: Boolean = true,

    val snackbarText: String? = null,
    val navigateUp: Boolean = false
) {

    fun allowEditing() = calendar?.canWriteContent() == true && !icalEntry.syncState.isDeletedState()

    fun allowRestore() = calendar?.canWriteContent() == true && icalEntry.syncState.isDeletedState()
}