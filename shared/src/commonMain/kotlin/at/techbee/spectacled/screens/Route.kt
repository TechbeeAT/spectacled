package at.techbee.spectacled.screens

import at.techbee.spectacled.screens.details.presentation.DetailsSheetOrDialog
import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable data object HomeGraph : Route

    @Serializable data class IcalEntryList(val calendarId: Long) : Route
    @Serializable data class IcalEntryDetails(val icalEntryId: Long, val newIcalEntryCalendarId: Long = 0L, val newIcalEntryInitialDescription: String? = null, val initialSheetOrDialog: DetailsSheetOrDialog? = null) : Route
    @Serializable data object AccountsList : Route
}
