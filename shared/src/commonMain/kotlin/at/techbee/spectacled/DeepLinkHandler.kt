package at.techbee.spectacled

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DeepLinkHandler {
    var deepLinkData by mutableStateOf(DeepLinkData())

    fun onDeepLinkReceived(calendarId: Long?, entryId: Long?, description: String?) {
        //println("DeepLinkHandler: Received link calendarId=$calendarId, entryId=$entryId, desc=$description")
        deepLinkData = DeepLinkData(
            initialCalendarId = calendarId,
            initialIcalEntryId = entryId,
            initialIcalEntryDescription = description,
            consumed = false
        )
    }

    fun consume() {
        deepLinkData = deepLinkData.copy(consumed = true)
    }
}

data class DeepLinkData(
    val initialCalendarId: Long? = null,
    val initialIcalEntryId: Long? = null,
    val initialIcalEntryDescription: String? = null,
    val consumed: Boolean = true
) {
    companion object {
        const val DEEPLINK_ADD_HOST = "add"
        const val DEEPLINK_DESCRIPTION_PARAM = "description"

    }

    fun isEmpty() = initialCalendarId == null && initialIcalEntryId == null && initialIcalEntryDescription == null
}
