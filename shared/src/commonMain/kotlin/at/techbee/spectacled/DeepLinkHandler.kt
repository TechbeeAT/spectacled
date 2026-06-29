package at.techbee.spectacled

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DeepLinkHandler {
    var initialCalendarId by mutableStateOf<Long?>(null)
    var initialIcalEntryId by mutableStateOf<Long?>(null)
    var initialIcalEntryDescription by mutableStateOf<String?>(null)

    fun onDeepLinkReceived(calendarId: Long?, entryId: Long?, description: String?) {
        initialCalendarId = calendarId
        initialIcalEntryId = entryId
        initialIcalEntryDescription = description
    }
}
