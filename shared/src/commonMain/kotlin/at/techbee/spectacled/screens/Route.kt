package at.techbee.spectacled.screens

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable data object HomeGraph : Route

    @Serializable data class NoteList(val calendarId: Long) : Route
    @Serializable data class NoteDetails(val noteId: Long) : Route
    @Serializable data class AddNote(val calendarId: Long, val copyFromId: Long? = null) : Route
    @Serializable data object AccountsList : Route
}