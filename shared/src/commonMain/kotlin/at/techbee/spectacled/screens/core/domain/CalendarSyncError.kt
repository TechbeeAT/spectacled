package at.techbee.spectacled.screens.core.domain

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.sync_error_calendar_not_fetchable
import spectacled.shared.generated.resources.sync_error_calendar_not_parsable
import spectacled.shared.generated.resources.sync_error_connection_error
import spectacled.shared.generated.resources.sync_error_connection_timed_out
import spectacled.shared.generated.resources.sync_error_request_failed
import spectacled.shared.generated.resources.sync_error_server_error
import spectacled.shared.generated.resources.sync_status_failed

/**
 * Why a sync failed, as a stable code rather than prose.
 *
 * CalendarSyncStatus is serialized into the database, so storing a translated message there would
 * freeze it in whatever language was active at sync time. Persisting a code and holding only a
 * [StringResource] reference lets the UI resolve the text on every render, so it follows the
 * current language. Referencing a resource is just object construction - nothing reads the
 * resource bundle until the UI calls stringResource(), which keeps sync itself free of the Compose
 * resource runtime.
 *
 * Names are part of the on-disk format - rename only with a migration. A code written by a newer
 * app version decodes to null (see CalendarSyncStatus.mapperJson), and the UI then falls back to
 * [CalendarSyncStatusType.stringRes].
 */
@Serializable
enum class CalendarSyncError(val stringRes: StringResource) {
    /** The request exceeded its timeout, or the connection timed out. */
    CONNECTION_TIMED_OUT(Res.string.sync_error_connection_timed_out),

    /** The server could not be reached, or the response was not a usable HTTP response. */
    CONNECTION_ERROR(Res.string.sync_error_connection_error),

    /** The server answered with a 5xx status. */
    SERVER_ERROR(Res.string.sync_error_server_error),

    /** The server answered with a 4xx status other than 401/404. */
    REQUEST_ERROR(Res.string.sync_error_request_failed),

    /** The calendar could not be fetched - the server answered with an unexpected status. */
    CALENDAR_NOT_FETCHABLE(Res.string.sync_error_calendar_not_fetchable),

    /** The calendar was fetched but its body could not be parsed. */
    CALENDAR_NOT_PARSABLE(Res.string.sync_error_calendar_not_parsable),

    /** Anything else - an unexpected exception during sync. */
    UNKNOWN(Res.string.sync_status_failed)
}
