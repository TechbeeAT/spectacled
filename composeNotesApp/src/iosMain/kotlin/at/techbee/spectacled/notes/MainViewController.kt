package at.techbee.spectacled.notes

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(
    initialCalendarId: Long? = null,
    initialIcalEntryId: Long? = null,
    initialIcalEntryDescription: String? = null
) = ComposeUIViewController { 
    NotesApp(
        initialCalendarId = initialCalendarId,
        initialIcalEntryId = initialIcalEntryId,
        initialIcalEntryDescription = initialIcalEntryDescription
    ) 
}