package at.techbee.spectacled.journals

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(
    initialCalendarId: Long? = null,
    initialIcalEntryId: Long? = null,
    initialIcalEntryDescription: String? = null
) = ComposeUIViewController { 
    JournalsApp(
        initialCalendarId = initialCalendarId,
        initialIcalEntryId = initialIcalEntryId,
        initialIcalEntryDescription = initialIcalEntryDescription
    ) 
}