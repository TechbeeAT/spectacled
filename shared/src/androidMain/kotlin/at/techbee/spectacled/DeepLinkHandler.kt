package at.techbee.spectacled

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

actual object DeepLinkHandler {
    actual var initialCalendarId by mutableStateOf<Long?>(null)
    actual var initialIcalEntryId by mutableStateOf<Long?>(null)
    actual var initialIcalEntryDescription by mutableStateOf<String?>(null)

    actual fun onDeepLinkReceived(calendarId: Long?, entryId: Long?, description: String?) {
        initialCalendarId = calendarId
        initialIcalEntryId = entryId
        initialIcalEntryDescription = description
    }

    actual fun setupDesktopHandler() {
        // No-op on Android
    }

    actual fun parseArgs(args: Array<String>) {
        // No-op on Android
    }
}
