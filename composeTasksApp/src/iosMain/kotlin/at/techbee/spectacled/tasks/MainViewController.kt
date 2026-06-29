package at.techbee.spectacled.tasks

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(
    initialCalendarId: Long? = null,
    initialIcalEntryId: Long? = null,
    initialIcalEntryDescription: String? = null
) = ComposeUIViewController { 
    TasksApp(
        initialCalendarId = initialCalendarId,
        initialIcalEntryId = initialIcalEntryId,
        initialIcalEntryDescription = initialIcalEntryDescription
    ) 
}