package at.techbee.spectacled.screens.core.domain

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class CalendarSyncStatus(
    val type: CalendarSyncStatusType,
    /**
     * Why the sync failed, for the error types. Null when there is nothing to add beyond [type].
     * A code rather than a message so the text can be localized at render time - see
     * [CalendarSyncError].
     */
    val error: CalendarSyncError? = null,
    /** Untranslated technical detail (HTTP status, stack trace) shown behind "Show more". */
    val details: String? = null,
    val icsDateTime: IcsDateTime = IcsDateTime.now()
) {

    companion object {

        private val mapperJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            // A status written by a newer app version may carry an error code this build doesn't
            // know, and rows written before error codes existed carry a "message" string instead.
            // Both decode to error = null, and the UI falls back to text derived from `type`.
            coerceInputValues = true
        }

        fun deserialize(string: String) = mapperJson.decodeFromString<CalendarSyncStatus>(string)
    }

    fun serialize() = mapperJson.encodeToString(this)
}

@Serializable
enum class CalendarSyncStatusType {
    IN_PROGRESS,
    FAILED,
    NOT_AUTHORIZED,
    NOT_FOUND,
    SYNCED,
    DISABLED;

    fun isErrorType() = this == FAILED || this == NOT_AUTHORIZED || this == NOT_FOUND
}
