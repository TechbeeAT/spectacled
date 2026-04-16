package at.techbee.spectacled.screens.note.presentation.notedetails

import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.note.domain.Note
import kotlin.time.ExperimentalTime

data class NoteDetailsState @OptIn(ExperimentalTime::class) constructor(
    val note: Note = Note(),
    val originalNote: Note = Note(),
    val calendar: Calendar? = null,

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

    fun allowEditing() = calendar?.canWriteContent() == true && !note.syncState.isDeletedState()

    fun allowRestore() = calendar?.canWriteContent() == true && note.syncState.isDeletedState()
}

