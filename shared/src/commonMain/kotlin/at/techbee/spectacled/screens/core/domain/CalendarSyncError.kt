package at.techbee.spectacled.screens.core.domain

import kotlinx.serialization.Serializable

/**
 * Why a sync failed, as a stable code rather than prose.
 *
 * CalendarSyncStatus is serialized into the database, so storing a translated message there would
 * freeze it in whatever language was active at sync time. Persisting a code instead lets the UI
 * resolve the text on every render, so it follows the current language, and keeps the Compose
 * resource runtime out of the data layer.
 *
 * Names are part of the on-disk format - rename only with a migration. A code written by a newer
 * app version decodes to null (see CalendarSyncStatus.mapperJson), and the UI then falls back to
 * text derived from CalendarSyncStatusType.
 */
@Serializable
enum class CalendarSyncError {
    /** The request exceeded its timeout, or the connection timed out. */
    CONNECTION_TIMED_OUT,

    /** The server could not be reached, or the response was not a usable HTTP response. */
    CONNECTION_ERROR,

    /** The server answered with a 5xx status. */
    SERVER_ERROR,

    /** The server answered with a 4xx status other than 401/404. */
    REQUEST_ERROR,

    /** The calendar could not be fetched - the server answered with an unexpected status. */
    CALENDAR_NOT_FETCHABLE,

    /** The calendar was fetched but its body could not be parsed. */
    CALENDAR_NOT_PARSABLE,

    /** Anything else - an unexpected exception during sync. */
    UNKNOWN
}
