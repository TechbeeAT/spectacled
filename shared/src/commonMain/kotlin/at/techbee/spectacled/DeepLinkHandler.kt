package at.techbee.spectacled

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DeepLinkHandler {
    var deepLinkData by mutableStateOf(DeepLinkData())

    fun onDeepLinkReceived(calendarId: Long?, entryId: Long?, description: String?) {
        deepLinkData = DeepLinkData(
            initialCalendarId = calendarId,
            initialIcalEntryId = entryId,
            initialIcalEntryDescription = description
        )
    }

    fun consume() {
        // Reset to an empty payload once the deep link has been handled. DeepLinkHandler is a
        // process-lifetime singleton, so leaving the calendar/entry/description set would keep
        // isEmpty() returning false for the rest of the process — permanently disabling the
        // "open last used calendar on startup" branch in SpectacledApp on every later (warm)
        // launch, which is what left the app on an empty calendar with a dead back button.
        deepLinkData = DeepLinkData()
    }
}

data class DeepLinkData(
    val initialCalendarId: Long? = null,
    val initialIcalEntryId: Long? = null,
    val initialIcalEntryDescription: String? = null
) {
    companion object {
        const val DEEPLINK_ADD_HOST = "add"
        const val DEEPLINK_DESCRIPTION_PARAM = "description"

    }

    fun isEmpty() = initialCalendarId == null && initialIcalEntryId == null && initialIcalEntryDescription == null
}
