package at.techbee.spectacled.screens.core

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.account.data.insertOrUpdateNote
import at.techbee.spectacled.screens.core.data.CredentialStore
import at.techbee.spectacled.screens.core.data.HttpClientFactory
import at.techbee.spectacled.screens.core.data.getPlatformEngine
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.webdav.DeleteResourceResult
import at.techbee.spectacled.screens.core.data.webdav.GetResourceResult
import at.techbee.spectacled.screens.core.data.webdav.MultigetResourceHrefETagResult
import at.techbee.spectacled.screens.core.data.webdav.MultigetResourceResult
import at.techbee.spectacled.screens.core.data.webdav.MultigetSyncCollectionResult
import at.techbee.spectacled.screens.core.data.webdav.PutResourceResult
import at.techbee.spectacled.screens.core.data.webdav.deleteResourceMultiplatform
import at.techbee.spectacled.screens.core.data.webdav.fetchSingleEntryMultiplatform
import at.techbee.spectacled.screens.core.data.webdav.getResourceMultiplatform
import at.techbee.spectacled.screens.core.data.webdav.multigetResourceHrefsMultiplatform
import at.techbee.spectacled.screens.core.data.webdav.putResourceMultiplatform
import at.techbee.spectacled.screens.core.data.webdav.syncCollectionMultiplatform
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import at.techbee.spectacled.screens.note.domain.Note
import at.techbee.spectacled.screens.note.domain.SyncState
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.Url
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime


class SyncCoordinator(
    val database: SpectacledDatabase,
    val client: HttpClient
) {

    companion object {

        @OptIn(ExperimentalTime::class)
        suspend fun syncAllPrincipals(
            database: SpectacledDatabase,
            credentialStore: CredentialStore
        ) {
            database.principal_dtoQueries.getAllPrincipals().awaitAsList().map { it.toDomain() }.forEach { principal ->
                val credentials = credentialStore.load(principal.principalUrl)
                val client = HttpClientFactory.create(getPlatformEngine(), credentials?.username, credentials?.password)
                database.calendar_dtoQueries
                    .getCalendarsForPrincipalUrl(principal.principalUrl.toString())
                    .awaitAsList()
                    .map { it.toDomain() }
                    .forEach { calendar ->
                        SyncCoordinator(database, client).syncCalendar(calendar)
                    }
            }

            // empty trashbin for items older than 30 days
            val cutoffDate = formatIcsDateTime(IcsDateTime(Clock.System.now().minus(30.days), false))?.first
            database.vjournal_dtoQueries.deleteTrashed(cutoffDate)
        }

        suspend fun syncSpecificCalendars(
            calendarIds: List<Long>,
            database: SpectacledDatabase,
            credentialStore: CredentialStore
        ) {
            database.calendar_dtoQueries.getCalendarsByIds(calendarIds).awaitAsList().map { it.toDomain() }.forEach { calendar ->

                val principal = database.principal_dtoQueries.getPrincipalForCalendar(calendar.id).awaitAsOne().toDomain()
                val credentials = credentialStore.load(principal.principalUrl)
                val client = HttpClientFactory.create(getPlatformEngine(), credentials?.username, credentials?.password)
                SyncCoordinator(database, client).syncCalendar(calendar)
            }
        }

        suspend fun pushLocalChanges(
            calendarId: Long,
            database: SpectacledDatabase,
            credentialStore: CredentialStore
        ) {
            val calendar = database.calendar_dtoQueries.getCalendarById(calendarId).awaitAsOne().toDomain()
            val principal = database.principal_dtoQueries.getPrincipalForCalendar(calendar.id).awaitAsOne().toDomain()
            val credentials = credentialStore.load(principal.principalUrl)
            val client = HttpClientFactory.create(getPlatformEngine(), credentials?.username, credentials?.password)

            SyncCoordinator(database, client).pushLocalChanges(calendar)
        }
    }

    private suspend fun syncCalendar(calendar: Calendar) = sync(calendar, null)

    suspend fun pushDirtyNote(dirtyNote: Note, calendar: Calendar) = sync(calendar, dirtyNote)

    private suspend fun sync(
        calendar: Calendar,
        dirtyNote: Note? = null   // if null all calendar is synchronized, otherwise only the dirtyNote is pushed
    ) {

        try {
            if (dirtyNote != null) {
                pushSingleLocalChange(dirtyNote, calendar)
                return
            }

            database.calendar_dtoQueries.updateCalendarSyncStatus(
                CalendarSyncStatus(CalendarSyncStatusType.IN_PROGRESS).serialize(),
                calendar.syncToken,
                calendar.id
            )

            /*
            if (enforceFullSync) {
                syncWithoutSyncToken(calendar)
                return
            }

             */

            val syncCollectionResponse = syncCollectionMultiplatform(client, calendar)
            println(syncCollectionResponse)
            when (syncCollectionResponse) {

                is MultigetSyncCollectionResult.Failed -> {
                    database.calendar_dtoQueries.updateCalendarSyncStatus(
                        CalendarSyncStatus(
                            CalendarSyncStatusType.FAILED,
                            syncCollectionResponse.message,
                            syncCollectionResponse.details
                        ).serialize(),
                        calendar.syncToken,
                        calendar.id
                    )
                    syncWithoutSyncToken(calendar)
                    return
                }

                MultigetSyncCollectionResult.NotAuthorized -> {
                    database.calendar_dtoQueries.updateCalendarSyncStatus(
                        CalendarSyncStatus(CalendarSyncStatusType.NOT_AUTHORIZED, "Not authorized").serialize(),
                        calendar.syncToken,
                        calendar.id
                    )
                    return
                }

                MultigetSyncCollectionResult.NotFound -> {
                    database.calendar_dtoQueries.updateCalendarSyncStatus(
                        CalendarSyncStatus(CalendarSyncStatusType.NOT_FOUND, "Not found").serialize(),
                        calendar.syncToken,
                        calendar.id
                    )
                    return
                }

                is MultigetSyncCollectionResult.Success -> {
                    applyServerchanges(
                        calendar,
                        syncCollectionResponse.hrefs.filter { it.value == null }.keys.toList(),
                        syncCollectionResponse.hrefs.mapNotNull { (url, eTag) -> eTag?.let { url to it } }.toMap()
                    )
                    pushLocalChanges(calendar)
                    database.calendar_dtoQueries.updateCalendarSyncStatus(
                        CalendarSyncStatus(CalendarSyncStatusType.SYNCED).serialize(),
                        syncCollectionResponse.syncToken,
                        calendar.id
                    )
                }
            }

        } catch (e: HttpRequestTimeoutException) { // Thrown when a request exceeds the configured timeout period.
            database.calendar_dtoQueries.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Connection timed out. Please check your internet connection and try again.",
                    e.stackTraceToString()
                ).serialize(), null, calendar.id
            )
        } catch (e: SocketTimeoutException) { // Standard Java/Kotlin IO exceptions often encountered during connectivity issues (e.g., airplane mode).
            database.calendar_dtoQueries.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Connection timed out. Please check your internet connection and try again.",
                    e.stackTraceToString()
                ).serialize(), null, calendar.id
            )
        } catch (e: ConnectTimeoutException) { // Standard Java/Kotlin IO exceptions often encountered during connectivity issues (e.g., airplane mode).
            database.calendar_dtoQueries.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Connection timed out. Please check your internet connection and try again.",
                    e.stackTraceToString()
                ).serialize(), null, calendar.id
            )
        } catch (e: ClientRequestException) {  // Thrown for 4xx status codes (Client errors like 404 Not Found or 401 Unauthorized).
            database.calendar_dtoQueries.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Request error. Please check your server, username and password and try again.",
                    e.stackTraceToString()
                ).serialize(), calendar.syncToken, calendar.id
            )
        } catch (e: ServerResponseException) {  // Thrown for 5xx status codes (Server errors).
            database.calendar_dtoQueries.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "An unexpected server error occurred. Please try again.",
                    e.stackTraceToString()
                ).serialize(), null, calendar.id
            )
        } catch (e: ResponseException) {   //The base class for all exceptions related to non-success HTTP responses.
            database.calendar_dtoQueries.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Connection error. Please check your internet connection and try again.",
                    e.stackTraceToString()
                ).serialize(), calendar.syncToken, calendar.id
            )
        } catch (e: NullPointerException) {   // Seems to be thrown when not authenticated
            database.calendar_dtoQueries.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.NOT_AUTHORIZED,
                    "Connection error. Please check your internet connection, username and password and try again.",
                    e.stackTraceToString()
                ).serialize(), null, calendar.id
            )
        } catch (e: Exception) {   //The base class for all exceptions related to non-success HTTP responses.
            database.calendar_dtoQueries.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Connection error. Please check your internet connection and try again.",
                    e.stackTraceToString()
                ).serialize(), null, calendar.id
            )
        }
    }

    private suspend fun syncWithoutSyncToken(calendar: Calendar) {
        when (val multigetResourceHrefsMultiplatformResult = multigetResourceHrefsMultiplatform(client, calendar)) {

            is MultigetResourceHrefETagResult.Failed -> {
                database.calendar_dtoQueries.updateCalendarSyncStatus(
                    CalendarSyncStatus(
                        CalendarSyncStatusType.FAILED,
                        multigetResourceHrefsMultiplatformResult.message,
                        multigetResourceHrefsMultiplatformResult.details
                    ).serialize(),
                    calendar.syncToken,
                    calendar.id
                )
                return
            }

            MultigetResourceHrefETagResult.NotAuthorized -> {
                database.calendar_dtoQueries.updateCalendarSyncStatus(
                    CalendarSyncStatus(CalendarSyncStatusType.NOT_AUTHORIZED, "Not authorized").serialize(),
                    calendar.syncToken,
                    calendar.id
                )
                return
            }

            MultigetResourceHrefETagResult.NotFound -> {
                database.calendar_dtoQueries.updateCalendarSyncStatus(
                    CalendarSyncStatus(CalendarSyncStatusType.NOT_FOUND, "Not found").serialize(),
                    calendar.syncToken,
                    calendar.id
                )
                return
            }

            is MultigetResourceHrefETagResult.Success -> {
                applyServerchanges(calendar, multigetResourceHrefsMultiplatformResult.hrefs)
                pushLocalChanges(calendar)
                database.calendar_dtoQueries.updateCalendarSyncStatus(
                    CalendarSyncStatus(CalendarSyncStatusType.SYNCED).serialize(),
                    multigetResourceHrefsMultiplatformResult.syncToken,
                    calendar.id
                )
            }
        }
    }

    private suspend fun applyServerchanges(calendar: Calendar, allServerHrefs: Map<Url, String?>) {

        val deletedHrefs =
            database.vjournal_dtoQueries.getDeletedDeltaHrefs(calendar.id, allServerHrefs.map { it.key.toString() }).awaitAsList()
                .mapNotNull { it.href }
        removeLocalByHrefs(deletedHrefs)

        allServerHrefs.forEach { serverHref ->
            upsertLocalByHrefs(calendar, serverHref.key, serverHref.value)
        }
    }

    private suspend fun applyServerchanges(calendar: Calendar, deleteHrefs: List<Url>, updateHrefs: Map<Url, String>) {

        removeLocalByHrefs(deleteHrefs.map { it.toString() })

        updateHrefs.forEach { serverHref ->
            upsertLocalByHrefs(calendar, serverHref.key, serverHref.value)
        }
    }

    private suspend fun upsertLocalByHrefs(calendar: Calendar, href: Url, eTag: String?) {

        val localNote = database.vjournal_dtoQueries.getJournalByHref(href.toString()).awaitAsOneOrNull()?.toDomain()

        if (localNote?.href != null && localNote.etag == eTag)
            return    // no eTag change, we skip

        val serverNote = when (val fetchSingleResult = fetchSingleEntryMultiplatform(client, calendar, href)) {
            is MultigetResourceResult.Failed -> return   // skip failed entries
            MultigetResourceResult.NotAuthorized -> return   // skip failed entries
            MultigetResourceResult.NotFound -> return   // skip failed entries
            is MultigetResourceResult.Success -> fetchSingleResult.notes.firstOrNull() ?: return
        }

        if (localNote?.href == null) {     // Local note doesn't exist, we insert
            database.insertOrUpdateNote(serverNote.copy(calendarId = calendar.id, syncState = SyncState.SYNCED))
        } else {    //Local note exists, but eTag is different. It is unchanged locally, but was changed on the server.

            when (localNote.syncState) {
                SyncState.SYNCED, SyncState.USER_DECIDED_SERVER_WINS ->
                    database.insertOrUpdateNote(serverNote.copy(id = localNote.id, calendarId = calendar.id, syncState = SyncState.SYNCED))

                SyncState.LOCAL_MODIFIED ->
                    database.insertOrUpdateNote(localNote.copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED))

                SyncState.LOCAL_DELETED, SyncState.REMOTE_DELETED_LOCAL_TRASHBIN ->
                    database.insertOrUpdateNote(localNote.copy(syncState = SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED))

                SyncState.USER_DECIDED_CLIENT_WINS ->
                    database.insertOrUpdateNote(
                        localNote.copy(
                            syncState = SyncState.LOCAL_MODIFIED,
                            etag = serverNote.etag
                        )
                    )    // etag updated, push local changes after

                SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED -> {}  // do nothing, user needs to decide
                SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED -> {}  // do nothing, user needs to decide
                SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED -> {}  // do nothing, user needs to decide
            }
        }
    }

    private suspend fun removeLocalByHrefs(hrefs: List<String>) {
        database.transaction {

            val deletedNotes = database.vjournal_dtoQueries.getJournalsByHrefs(hrefs).awaitAsList().map { it.toDomain() }

            deletedNotes.forEach { deletedNote ->

                when (deletedNote.syncState) {
                    SyncState.LOCAL_DELETED, SyncState.SYNCED, SyncState.REMOTE_DELETED_LOCAL_TRASHBIN ->
                        database.insertOrUpdateNote(deletedNote.copy(syncState = SyncState.REMOTE_DELETED_LOCAL_TRASHBIN))

                    SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED, SyncState.USER_DECIDED_SERVER_WINS ->
                        // conflict1, but now it's also deleted on the server, we can delete it now
                        // conflict2, user decided to keep remote changes (delete), we can delete it now
                        database.insertOrUpdateNote(deletedNote.copy(syncState = SyncState.REMOTE_DELETED_LOCAL_TRASHBIN))

                    SyncState.LOCAL_MODIFIED, SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED, SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED ->
                        database.insertOrUpdateNote(deletedNote.copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED))

                    SyncState.USER_DECIDED_CLIENT_WINS ->
                        database.insertOrUpdateNote(
                            deletedNote.copy(
                                syncState = SyncState.LOCAL_MODIFIED,
                                href = null,
                                etag = null
                            )
                        )  // treat like a new note
                }
            }
        }
    }


    private suspend fun pushLocalChanges(
        calendar: Calendar
    ) {

        val dirtyNotes = database.vjournal_dtoQueries.getDirtyJournalsByCalendar(calendar.id).awaitAsList().map { it.toDomain() }

        dirtyNotes.forEach { dirtyNote ->
            pushSingleLocalChange(dirtyNote, calendar)
        }
    }


    private suspend fun pushSingleLocalChange(dirtyNote: Note, calendar: Calendar) {

        when (dirtyNote.syncState) {
            // synchronized notes shouldn't even be returned by the query, do nothing
            SyncState.SYNCED, SyncState.REMOTE_DELETED_LOCAL_TRASHBIN -> Unit // do nothing

            SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED, SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED, SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED -> Unit // do nothing, conflicts need to be resolved by user

            SyncState.LOCAL_MODIFIED -> {
                val insertOrUpdateNoteResult = putResourceMultiplatform(client, calendar, dirtyNote)
                when (insertOrUpdateNoteResult) {
                    // Conflict was detected, we get the latest resource
                    PutResourceResult.Conflict -> {
                        val conflictingServerNoteResult = getResourceMultiplatform(client, calendar, dirtyNote)
                        when (conflictingServerNoteResult) {

                            is GetResourceResult.Failed -> Unit   // failed will be kept for another retry TODO: Review if this is sufficient in future

                            // Resource wasn't found, deleted on server
                            GetResourceResult.NotFound -> database.insertOrUpdateNote(dirtyNote.copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED))

                            // A newer version exists
                            is GetResourceResult.Success -> database.insertOrUpdateNote(dirtyNote.copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED))
                        }
                    }

                    // Failed for some reason, retry
                    is PutResourceResult.Failed -> Unit   // leave for retry // TODO: Review in future, maybe store info why it failed

                    // The entry was deleted in the meantime, we also delete it locally
                    PutResourceResult.NotFound -> database.insertOrUpdateNote(dirtyNote.copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED))

                    // The locally modified entry was successfully pushed to the server, we just update the local entry as synced and store the new eTag
                    is PutResourceResult.Success -> database.insertOrUpdateNote(
                        dirtyNote.copy(
                            etag = insertOrUpdateNoteResult.note.etag,
                            href = insertOrUpdateNoteResult.note.href,
                            syncState = SyncState.SYNCED
                        )
                    )
                }
            }


            // note was locally modified, we put and see if there's a conflict
            SyncState.USER_DECIDED_CLIENT_WINS -> {
                val insertOrUpdateNoteResult = putResourceMultiplatform(client, calendar, dirtyNote)
                when (insertOrUpdateNoteResult) {
                    // Conflict was detected, we get the latest resource
                    PutResourceResult.Conflict -> {
                        val conflictingServerNoteResult = getResourceMultiplatform(client, calendar, dirtyNote)
                        when (conflictingServerNoteResult) {

                            // failed will be kept for another retry TODO: Review if this is sufficient in future
                            is GetResourceResult.Failed -> Unit   // Retry

                            // Resource wasn't found, deleted on server
                            GetResourceResult.NotFound -> {
                                val clientNote = dirtyNote.copy(syncState = SyncState.LOCAL_MODIFIED, etag = null, href = null)
                                database.insertOrUpdateNote(clientNote)
                                pushSingleLocalChange(clientNote, calendar)
                            }

                            // The new entry was fetched, we overwrite the local changes, server wins
                            is GetResourceResult.Success -> {
                                val clientNote =
                                    dirtyNote.copy(syncState = SyncState.LOCAL_MODIFIED, etag = conflictingServerNoteResult.note.etag)
                                database.insertOrUpdateNote(clientNote)
                                pushSingleLocalChange(clientNote, calendar)
                            }
                        }
                    }

                    // Failed for some reason, retry
                    is PutResourceResult.Failed -> Unit   // leave for retry // TODO: Review in future, maybe store info why it failed

                    // The entry was deleted in the meantime, we also delete it locally
                    PutResourceResult.NotFound -> {
                        val clientNote = dirtyNote.copy(syncState = SyncState.LOCAL_MODIFIED, etag = null, href = null)
                        database.insertOrUpdateNote(clientNote)
                        pushSingleLocalChange(clientNote, calendar)
                    }

                    // The locally modified entry was successfully pushed to the server, we just update the local entry as synced and store the new eTag
                    is PutResourceResult.Success ->
                        database.insertOrUpdateNote(
                            dirtyNote.copy(
                                etag = insertOrUpdateNoteResult.note.etag,
                                href = insertOrUpdateNoteResult.note.href,
                                syncState = SyncState.SYNCED
                            )
                        )
                }
            }

            // note was locally modified, we put and see if there's a conflict
            SyncState.USER_DECIDED_SERVER_WINS -> {   //TODO!!
                val conflictingServerNoteResult = getResourceMultiplatform(client, calendar, dirtyNote)
                when (conflictingServerNoteResult) {

                    // failed will be kept for another retry TODO: Review if this is sufficient in future
                    is GetResourceResult.Failed -> Unit   // Retry

                    // Resource wasn't found, deleted on server, we delete as user decided to keep server version
                    GetResourceResult.NotFound -> database.insertOrUpdateNote(dirtyNote.copy(syncState = SyncState.REMOTE_DELETED_LOCAL_TRASHBIN))

                    // The new entry was fetched, we overwrite the local changes, server wins
                    is GetResourceResult.Success -> {
                        database.insertOrUpdateNote(
                            conflictingServerNoteResult.note.copy(
                                id = dirtyNote.id,
                                calendarId = dirtyNote.calendarId,
                                syncState = SyncState.SYNCED
                            )
                        )
                    }
                }
            }

            SyncState.LOCAL_DELETED -> {
                val deleteResourceResult = deleteResourceMultiplatform(client, calendar, dirtyNote)
                when (deleteResourceResult) {

                    // The entry was already deleted or successfully deleted on the server. We delete it locally.
                    DeleteResourceResult.AlreadyDeleted, DeleteResourceResult.Success ->
                        database.insertOrUpdateNote(dirtyNote.copy(syncState = SyncState.REMOTE_DELETED_LOCAL_TRASHBIN))

                    // There was a conflict, the resourcew as changed on the server, we discard the local delete and update the entry instead
                    // TODO: Review in future
                    DeleteResourceResult.Conflict -> {
                        val conflictingServerNoteResult = getResourceMultiplatform(client, calendar, dirtyNote)
                        when (conflictingServerNoteResult) {

                            // failed will be kept for another retry TODO: Review if this is sufficient in future
                            is GetResourceResult.Failed -> Unit   // Retry

                            // Resource wasn't found, we delete the local copy, this should have been Success though
                            GetResourceResult.NotFound -> database.insertOrUpdateNote(dirtyNote.copy(syncState = SyncState.REMOTE_DELETED_LOCAL_TRASHBIN))

                            // The new entry was fetched
                            is GetResourceResult.Success -> {
                                // server returns updated note
                                database.insertOrUpdateNote(dirtyNote.copy(syncState = SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED))
                            }
                        }
                    }

                    is DeleteResourceResult.Failed -> Unit   // Retry another time. TODO: Consider storing information why the delete failed
                }
            }
        }
    }
}
