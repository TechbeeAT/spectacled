package at.techbee.spectacled.screens

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable data object HomeGraph : Route

    @Serializable data class IcalEntryList(val calendarId: Long) : Route
    @Serializable data class IcalEntryDetails(val icalEntryId: Long) : Route
    @Serializable data class AddICalEntry(val calendarId: Long, val copyFromId: Long? = null) : Route
    @Serializable data object AccountsList : Route
}