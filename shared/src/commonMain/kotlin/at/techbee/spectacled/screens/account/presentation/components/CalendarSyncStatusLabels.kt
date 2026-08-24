package at.techbee.spectacled.screens.account.presentation.components

import at.techbee.spectacled.screens.core.domain.CalendarSyncError
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.sync_error_calendar_not_fetchable
import spectacled.shared.generated.resources.sync_error_calendar_not_parsable
import spectacled.shared.generated.resources.sync_error_connection_error
import spectacled.shared.generated.resources.sync_error_connection_timed_out
import spectacled.shared.generated.resources.sync_error_request_failed
import spectacled.shared.generated.resources.sync_error_server_error
import spectacled.shared.generated.resources.sync_error_unknown
import spectacled.shared.generated.resources.sync_status_not_authorized
import spectacled.shared.generated.resources.sync_status_not_found

/**
 * Turns a persisted sync status into display text.
 *
 * SyncCoordinator stores only a [CalendarSyncStatusType] and an optional [CalendarSyncError], never
 * a translated message, so the text is resolved here on every render and follows the current
 * language even for a status written long ago in another one.
 */
fun CalendarSyncError.labelRes(): StringResource = when (this) {
    CalendarSyncError.CONNECTION_TIMED_OUT -> Res.string.sync_error_connection_timed_out
    CalendarSyncError.CONNECTION_ERROR -> Res.string.sync_error_connection_error
    CalendarSyncError.SERVER_ERROR -> Res.string.sync_error_server_error
    CalendarSyncError.REQUEST_ERROR -> Res.string.sync_error_request_failed
    CalendarSyncError.CALENDAR_NOT_FETCHABLE -> Res.string.sync_error_calendar_not_fetchable
    CalendarSyncError.CALENDAR_NOT_PARSABLE -> Res.string.sync_error_calendar_not_parsable
    CalendarSyncError.UNKNOWN -> Res.string.sync_error_unknown
}

/** Text for a status type on its own, where the type alone is what the user needs to know. */
fun CalendarSyncStatusType.labelRes(): StringResource? = when (this) {
    CalendarSyncStatusType.NOT_AUTHORIZED -> Res.string.sync_status_not_authorized
    CalendarSyncStatusType.NOT_FOUND -> Res.string.sync_status_not_found
    else -> null
}

/**
 * The message to show for a status, or null when there is nothing useful to say. Falls back to the
 * type when [CalendarSyncStatus.error] is absent - which is the case for statuses written before
 * error codes existed, and for codes written by a newer app version.
 */
fun CalendarSyncStatus.messageLabelRes(): StringResource? =
    error?.labelRes() ?: type.labelRes() ?: if (type == CalendarSyncStatusType.FAILED) Res.string.sync_error_unknown else null
