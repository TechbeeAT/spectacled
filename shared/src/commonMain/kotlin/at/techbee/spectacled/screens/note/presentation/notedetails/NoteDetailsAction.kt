package at.techbee.spectacled.screens.note.presentation.notedetails

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.note.domain.SyncState

sealed interface NoteDetailsAction {
    data class OnUpdateSummary(val summary: String): NoteDetailsAction
    data class OnUpdateDescription(val description: String): NoteDetailsAction
    data class OnUpdateCategories(val categories: List<String>): NoteDetailsAction
    data class OnUpdateColor(val color: Color?): NoteDetailsAction
    data class OnUpdateDtStart(val icsDateTime: IcsDateTime): NoteDetailsAction

    data class OnSyncConflictUpdateUserDecision(val syncState: SyncState): NoteDetailsAction

    data class OnShowMoreBottomSheet(val show: Boolean): NoteDetailsAction
    data class OnShowDeleteNoteDialog(val show: Boolean): NoteDetailsAction
    data class OnShowColorSelectorBottomSheet(val show: Boolean): NoteDetailsAction
    data class OnShowCategorySelectorBottomSheet(val show: Boolean): NoteDetailsAction
    data class OnShowTimePickerBottomSheet(val show: Boolean): NoteDetailsAction
    data class OnShowDatePickerBottomSheet(val show: Boolean): NoteDetailsAction
    data class OnShowTimezonePickerBottomSheet(val show: Boolean): NoteDetailsAction

    //object OnSaveNote: NoteDetailsAction
    object OnDeleteNote: NoteDetailsAction
    object OnCreateCopy: NoteDetailsAction
    object OnDispose: NoteDetailsAction
    object OnRestoreEntry: NoteDetailsAction
    //data class OnCopyNote(val uid: String): NoteDetailsAction
    //object OnShareNote: NoteDetailsAction
    data class OnUpdateSnackbar(val message: String?): NoteDetailsAction
    data class OnNavigateUp(val navigateUp: Boolean): NoteDetailsAction
}