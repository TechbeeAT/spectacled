package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.data.CredentialStore
import at.techbee.spectacled.screens.core.data.Credentials
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
import at.techbee.spectacled.screens.core.data.webdav.uploadFileMultiplatform
import at.techbee.spectacled.screens.core.domain.AttachmentSyncState
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime


class SyncCoordinator(
    val calendarRepository: CalendarRepository,
    val icalEntryRepository: IcalEntryRepository,
    val fileManager: FileManager,
    val client: HttpClient,
    val credentials: Credentials?
) {

    companion object {

        // Guards against a background sync and a manual push/refresh racing on the same
        // calendar's syncToken and rows. One Mutex per calendarId; the outer lock only guards
        // creating that per-calendar Mutex, it isn't held during the sync itself.
        private val calendarSyncMutexesLock = Mutex()
        private val calendarSyncMutexes = mutableMapOf<Long, Mutex>()

        private suspend fun mutexFor(calendarId: Long): Mutex =
            calendarSyncMutexesLock.withLock {
                calendarSyncMutexes.getOrPut(calendarId) { Mutex() }
            }

        @OptIn(ExperimentalTime::class)
        suspend fun syncAllPrincipals(
            calendarRepository: CalendarRepository,
            icalEntryRepository: IcalEntryRepository,
            fileManager: FileManager,
            credentialStore: CredentialStore,
            client: HttpClient
        ) {
            calendarRepository.getAllPrincipals().forEach { principal ->
                val credentials = credentialStore.load(principal.principalUrl)
                calendarRepository
                    .getCalendarsForPrincipalUrl(principal.principalUrl.toString())
                    .let { calendars ->
                        coroutineScope {
                            calendars.forEach { calendar ->
                                if (calendar.calendarSyncStatus?.type == CalendarSyncStatusType.DISABLED)
                                    return@forEach  // skip disabled calendars

                                launch {
                                    SyncCoordinator(
                                        calendarRepository,
                                        icalEntryRepository,
                                        fileManager,
                                        client,
                                        credentials
                                    ).syncCalendarWithSyncLock(calendar)
                                }
                            }
                        }
                    }
            }

            // empty trashbin for items older than 30 days
            val cutoffDate = IcsDateTime(Clock.System.now().minus(30.days), false)
            icalEntryRepository.deleteTrashed(cutoffDate)
        }

        suspend fun syncSpecificCalendars(
            calendarIds: List<Long>,
            calendarRepository: CalendarRepository,
            icalEntryRepository: IcalEntryRepository,
            fileManager: FileManager,
            credentialStore: CredentialStore,
            client: HttpClient
        ) {
            val calendars = calendarRepository.getCalendarsByIds(calendarIds)

            coroutineScope {
                calendars.forEach { calendar ->
                    launch {
                        val principal = calendarRepository.getPrincipalForCalendar(calendar.id)
                            ?: return@launch  // TODO: Maybe better enter sync-problem in DB
                        val credentials = credentialStore.load(principal.principalUrl)
                        SyncCoordinator(calendarRepository, icalEntryRepository, fileManager, client, credentials).syncCalendarWithSyncLock(calendar)
                    }
                }
            }
        }
    }


    suspend fun syncCalendarWithSyncLock(calendar: Calendar) {
        val mutex = mutexFor(calendar.id)
        if (!mutex.tryLock()) {
            Napier.d("Sync already in progress for calendar ${calendar.id}, skipping")
            return
        }
        try {
            sync(calendar)
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun sync(calendar: Calendar) {

        try {
            calendarRepository.updateCalendarSyncStatus(
                CalendarSyncStatus(CalendarSyncStatusType.IN_PROGRESS).serialize(),
                calendar.syncToken,
                calendar.id
            )

            val syncCollectionResponse = syncCollectionMultiplatform(client, calendar, credentials)
            when (syncCollectionResponse) {

                is MultigetSyncCollectionResult.Failed -> {
                    calendarRepository.updateCalendarSyncStatus(
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
                    calendarRepository.updateCalendarSyncStatus(
                        CalendarSyncStatus(CalendarSyncStatusType.NOT_AUTHORIZED, "Not authorized").serialize(),
                        calendar.syncToken,
                        calendar.id
                    )
                    return
                }

                MultigetSyncCollectionResult.NotFound -> {
                    calendarRepository.updateCalendarSyncStatus(
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
                    calendarRepository.updateCalendarSyncStatus(
                        CalendarSyncStatus(CalendarSyncStatusType.SYNCED).serialize(),
                        syncCollectionResponse.syncToken,
                        calendar.id
                    )
                }
            }

        } catch (e: HttpRequestTimeoutException) { // Thrown when a request exceeds the configured timeout period.
            calendarRepository.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Connection timed out. Please check your internet connection and try again.",
                    e.stackTraceToString()
                ).serialize(), calendar.syncToken, calendar.id
            )
        } catch (e: SocketTimeoutException) { // Standard Java/Kotlin IO exceptions often encountered during connectivity issues (e.g., airplane mode).
            calendarRepository.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Connection timed out. Please check your internet connection and try again.",
                    e.stackTraceToString()
                ).serialize(), calendar.syncToken, calendar.id
            )
        } catch (e: ConnectTimeoutException) { // Standard Java/Kotlin IO exceptions often encountered during connectivity issues (e.g., airplane mode).
            calendarRepository.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Connection timed out. Please check your internet connection and try again.",
                    e.stackTraceToString()
                ).serialize(), calendar.syncToken, calendar.id
            )
        } catch (e: ClientRequestException) {  // Thrown for 4xx status codes (Client errors like 404 Not Found or 401 Unauthorized).
            calendarRepository.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Request error. Please check your server, username and password and try again.",
                    e.stackTraceToString()
                ).serialize(), calendar.syncToken, calendar.id
            )
        } catch (e: ServerResponseException) {  // Thrown for 5xx status codes (Server errors).
            calendarRepository.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "An unexpected server error occurred. Please try again.",
                    e.stackTraceToString()
                ).serialize(), calendar.syncToken, calendar.id
            )
        } catch (e: ResponseException) {   //The base class for all exceptions related to non-success HTTP responses.
            calendarRepository.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Connection error. Please check your internet connection and try again.",
                    e.stackTraceToString()
                ).serialize(), calendar.syncToken, calendar.id
            )
        } catch (e: Exception) {   //The base class for all exceptions related to non-success HTTP responses.
            calendarRepository.updateCalendarSyncStatus(
                CalendarSyncStatus(
                    CalendarSyncStatusType.FAILED,
                    "Connection error: ${e.message}",
                    e.stackTraceToString()
                ).serialize(), calendar.syncToken, calendar.id
            )
        }
    }

    private suspend fun syncWithoutSyncToken(calendar: Calendar) {
        when (val multigetResourceHrefsMultiplatformResult = multigetResourceHrefsMultiplatform(client, calendar, credentials)) {

            is MultigetResourceHrefETagResult.Failed -> {
                calendarRepository.updateCalendarSyncStatus(
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
                calendarRepository.updateCalendarSyncStatus(
                    CalendarSyncStatus(CalendarSyncStatusType.NOT_AUTHORIZED, "Not authorized").serialize(),
                    calendar.syncToken,
                    calendar.id
                )
                return
            }

            MultigetResourceHrefETagResult.NotFound -> {
                calendarRepository.updateCalendarSyncStatus(
                    CalendarSyncStatus(CalendarSyncStatusType.NOT_FOUND, "Not found").serialize(),
                    calendar.syncToken,
                    calendar.id
                )
                return
            }

            is MultigetResourceHrefETagResult.Success -> {
                applyServerchanges(calendar, multigetResourceHrefsMultiplatformResult.hrefs)
                pushLocalChanges(calendar)
                calendarRepository.updateCalendarSyncStatus(
                    CalendarSyncStatus(CalendarSyncStatusType.SYNCED).serialize(),
                    multigetResourceHrefsMultiplatformResult.syncToken,
                    calendar.id
                )
            }
        }
    }

    private suspend fun applyServerchanges(calendar: Calendar, allServerHrefs: Map<Url, String?>) {

        val deletedHrefs = icalEntryRepository.getDeletedDeltaHrefs(calendar.id, allServerHrefs.map { it.key })
        removeLocalByHrefs(deletedHrefs)

        allServerHrefs.forEach { serverHref ->
            upsertLocalByHrefs(calendar, serverHref.key, serverHref.value)
        }
    }

    private suspend fun applyServerchanges(calendar: Calendar, deleteHrefs: List<Url>, updateHrefs: Map<Url, String>) {

        removeLocalByHrefs(deleteHrefs)

        updateHrefs.forEach { serverHref ->
            upsertLocalByHrefs(calendar, serverHref.key, serverHref.value)
        }
    }

    private suspend fun upsertLocalByHrefs(calendar: Calendar, href: Url, eTag: String?) {

        var localIcalEntry = icalEntryRepository.getIcalEntryByHref(href)

        if (localIcalEntry?.href != null && localIcalEntry.etag == eTag)
            return    // no eTag change, we skip

        val serverIcalEntry = when (val fetchSingleResult = fetchSingleEntryMultiplatform(client, calendar, href, credentials, fileManager)) {
            is MultigetResourceResult.Failed -> return   // skip failed entries
            MultigetResourceResult.NotAuthorized -> return   // skip failed entries
            MultigetResourceResult.NotFound -> return   // skip failed entries
            is MultigetResourceResult.Success -> fetchSingleResult.icalEntries.firstOrNull() ?: return
        }

        if (localIcalEntry == null) {
            localIcalEntry = icalEntryRepository.getIcalEntryByUid(calendar.id, serverIcalEntry.uid)
        }

        if (localIcalEntry == null) {     // Local IcalEntry doesn't exist, we insert
            icalEntryRepository.insertOrUpdateIcalEntry(serverIcalEntry.copy(calendarId = calendar.id, syncState = SyncState.SYNCED))
        } else {    //Local IcalEntry exists, but eTag is different. It is unchanged locally, but was changed on the server.

            when (localIcalEntry.syncState) {
                SyncState.SYNCED, SyncState.USER_DECIDED_SERVER_WINS ->
                    icalEntryRepository.insertOrUpdateIcalEntry(
                        serverIcalEntry.copy(
                            id = localIcalEntry.id,
                            calendarId = calendar.id,
                            syncState = SyncState.SYNCED
                        )
                    )

                SyncState.LOCAL_MODIFIED ->
                    icalEntryRepository.insertOrUpdateIcalEntry(localIcalEntry.copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED))

                SyncState.LOCAL_DELETED, SyncState.REMOTE_DELETED_LOCAL_TRASHBIN ->
                    icalEntryRepository.insertOrUpdateIcalEntry(localIcalEntry.copy(syncState = SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED))

                SyncState.USER_DECIDED_CLIENT_WINS ->
                    icalEntryRepository.insertOrUpdateIcalEntry(
                        localIcalEntry.copy(
                            syncState = SyncState.LOCAL_MODIFIED,
                            etag = serverIcalEntry.etag,
                            href = serverIcalEntry.href
                        )
                    )    // etag updated, push local changes after

                SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED -> {}  // do nothing, user needs to decide
                SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED -> {}  // do nothing, user needs to decide
                SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED -> {}  // do nothing, user needs to decide
            }
        }
    }

    private suspend fun removeLocalByHrefs(hrefs: List<Url>) {

        val deletedIcalEntry = icalEntryRepository.getIcalEntriesByHrefs(hrefs)

        deletedIcalEntry.forEach { deletedIcalEntry ->

            when (deletedIcalEntry.syncState) {
                SyncState.LOCAL_DELETED, SyncState.SYNCED, SyncState.REMOTE_DELETED_LOCAL_TRASHBIN ->
                    icalEntryRepository.insertOrUpdateIcalEntry(
                        deletedIcalEntry.copy(
                            syncState = SyncState.REMOTE_DELETED_LOCAL_TRASHBIN,
                            lastModified = IcsDateTime.now()
                        )
                    )

                SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED, SyncState.USER_DECIDED_SERVER_WINS ->
                    // conflict1, but now it's also deleted on the server, we can delete it now
                    // conflict2, user decided to keep remote changes (delete), we can delete it now
                    icalEntryRepository.insertOrUpdateIcalEntry(
                        deletedIcalEntry.copy(
                            syncState = SyncState.REMOTE_DELETED_LOCAL_TRASHBIN,
                            lastModified = IcsDateTime.now()
                        )
                    )

                SyncState.LOCAL_MODIFIED, SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED, SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED ->
                    icalEntryRepository.insertOrUpdateIcalEntry(
                        deletedIcalEntry.copy(
                            syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED,
                            lastModified = IcsDateTime.now()
                        )
                    )

                SyncState.USER_DECIDED_CLIENT_WINS ->
                    icalEntryRepository.insertOrUpdateIcalEntry(
                        deletedIcalEntry.copy(
                            syncState = SyncState.LOCAL_MODIFIED,
                            href = null,
                            etag = null,
                            lastModified = IcsDateTime.now()
                        )
                    )  // treat like a new entry
            }
        }
    }

    private suspend fun pushLocalChanges(calendar: Calendar) {
        val dirtyIcalEntries = icalEntryRepository.getDirtyIcalEntriesByCalendar(calendar.id)
        dirtyIcalEntries.forEach { pushSingleLocalChange(it, calendar) }
    }

    private suspend fun pushSingleLocalChange(dirtyIcalEntry: IcalEntry, calendar: Calendar) {

        when (dirtyIcalEntry.syncState) {
            // synchronized entries shouldn't even be returned by the query, do nothing
            SyncState.SYNCED, SyncState.REMOTE_DELETED_LOCAL_TRASHBIN -> Unit // do nothing

            SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED, SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED, SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED -> Unit // do nothing, conflicts need to be resolved by user

            SyncState.LOCAL_MODIFIED -> {
                val entryToPush = pushAttachments(dirtyIcalEntry, calendar)    // TODO: store error?
                val insertOrUpdateIcalEntryResult = putResourceMultiplatform(client, calendar, entryToPush, credentials, fileManager)
                when (insertOrUpdateIcalEntryResult) {
                    // Conflict was detected, we get the latest resource
                    PutResourceResult.Conflict -> {
                        val conflictingServerIcalEntryResult = getResourceMultiplatform(client, calendar, entryToPush, credentials, fileManager)
                        when (conflictingServerIcalEntryResult) {

                            is GetResourceResult.Failed -> Unit   // failed will be kept for another retry TODO: Review if this is sufficient in future

                            // Resource wasn't found, deleted on server
                            GetResourceResult.NotFound -> icalEntryRepository.insertOrUpdateIcalEntry(entryToPush.copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED))

                            // A newer version exists
                            is GetResourceResult.Success -> icalEntryRepository.insertOrUpdateIcalEntry(entryToPush.copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED))
                        }
                    }

                    // Failed for some reason, retry
                    is PutResourceResult.Failed -> Unit   // leave for retry // TODO: Review in future, maybe store info why it failed

                    // The entry was deleted in the meantime, we also delete it locally
                    PutResourceResult.NotFound -> icalEntryRepository.insertOrUpdateIcalEntry(entryToPush.copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED))

                    // The locally modified entry was successfully pushed to the server, we just update the local entry as synced and store the new eTag
                    is PutResourceResult.Success -> icalEntryRepository.updateSyncMetadata(
                        etag = insertOrUpdateIcalEntryResult.icalEntry.etag,
                        href = insertOrUpdateIcalEntryResult.icalEntry.href,
                        syncState = SyncState.SYNCED,
                        id = dirtyIcalEntry.id
                    )
                }
            }

            // entry was locally modified, we put and see if there's a conflict
            SyncState.USER_DECIDED_CLIENT_WINS -> {
                val entryToPush = pushAttachments(dirtyIcalEntry, calendar)
                val insertOrUpdateIcalEntryResult = putResourceMultiplatform(client, calendar, entryToPush, credentials, fileManager)
                when (insertOrUpdateIcalEntryResult) {
                    // Conflict was detected, we get the latest resource
                    PutResourceResult.Conflict -> {
                        val conflictingServerIcalEntryResult = getResourceMultiplatform(client, calendar, entryToPush, credentials, fileManager)
                        when (conflictingServerIcalEntryResult) {

                            // failed will be kept for another retry TODO: Review if this is sufficient in future
                            is GetResourceResult.Failed -> Unit   // Retry

                            // Resource wasn't found, deleted on server
                            GetResourceResult.NotFound -> {
                                val clientIcalEntry = entryToPush.copy(syncState = SyncState.LOCAL_MODIFIED, etag = null, href = null)
                                icalEntryRepository.insertOrUpdateIcalEntry(clientIcalEntry)
                                pushSingleLocalChange(clientIcalEntry, calendar)
                            }

                            // The new entry was fetched, we overwrite the local changes, server wins
                            is GetResourceResult.Success -> {
                                val clientIcalEntry =
                                    entryToPush.copy(
                                        syncState = SyncState.LOCAL_MODIFIED,
                                        etag = conflictingServerIcalEntryResult.icalEntry.etag
                                    )
                                icalEntryRepository.insertOrUpdateIcalEntry(clientIcalEntry)
                                pushSingleLocalChange(clientIcalEntry, calendar)
                            }
                        }
                    }

                    // Failed for some reason, retry
                    is PutResourceResult.Failed -> Unit   // leave for retry // TODO: Review in future, maybe store info why it failed

                    // The entry was deleted in the meantime, recreate it to push it again
                    PutResourceResult.NotFound -> {
                        val clientIcalEntry = entryToPush.copy(syncState = SyncState.LOCAL_MODIFIED, etag = null, href = null)
                        icalEntryRepository.insertOrUpdateIcalEntry(clientIcalEntry)
                        pushSingleLocalChange(clientIcalEntry, calendar)
                    }

                    // The locally modified entry was successfully pushed to the server, we just update the local entry as synced and store the new eTag
                    is PutResourceResult.Success ->
                        icalEntryRepository.updateSyncMetadata(
                            etag = insertOrUpdateIcalEntryResult.icalEntry.etag,
                            href = insertOrUpdateIcalEntryResult.icalEntry.href,
                            syncState = SyncState.SYNCED,
                            id = dirtyIcalEntry.id
                        )
                }
            }

            // entry was locally modified, we put and see if there's a conflict
            SyncState.USER_DECIDED_SERVER_WINS -> {   //TODO!!
                val conflictingServerIcalEntryResult = getResourceMultiplatform(client, calendar, dirtyIcalEntry, credentials, fileManager)
                when (conflictingServerIcalEntryResult) {

                    // failed will be kept for another retry TODO: Review if this is sufficient in future
                    is GetResourceResult.Failed -> Unit   // Retry

                    // Resource wasn't found, deleted on server, we delete as user decided to keep server version
                    GetResourceResult.NotFound -> icalEntryRepository.insertOrUpdateIcalEntry(dirtyIcalEntry.copy(syncState = SyncState.REMOTE_DELETED_LOCAL_TRASHBIN))

                    // The new entry was fetched, we overwrite the local changes, server wins
                    is GetResourceResult.Success -> {
                        icalEntryRepository.insertOrUpdateIcalEntry(
                            conflictingServerIcalEntryResult.icalEntry.copy(
                                id = dirtyIcalEntry.id,
                                calendarId = dirtyIcalEntry.calendarId,
                                syncState = SyncState.SYNCED
                            )
                        )
                    }
                }
            }

            SyncState.LOCAL_DELETED -> {
                val deleteResourceResult = deleteResourceMultiplatform(client, calendar, dirtyIcalEntry, credentials)
                when (deleteResourceResult) {

                    // The entry was already deleted or successfully deleted on the server. We delete it locally.
                    DeleteResourceResult.AlreadyDeleted, DeleteResourceResult.Success ->
                        icalEntryRepository.insertOrUpdateIcalEntry(dirtyIcalEntry.copy(syncState = SyncState.REMOTE_DELETED_LOCAL_TRASHBIN))

                    // There was a conflict, the resourcew as changed on the server, we discard the local delete and update the entry instead
                    // TODO: Review in future
                    DeleteResourceResult.Conflict -> {
                        val conflictingServerIcalEntryResult = getResourceMultiplatform(client, calendar, dirtyIcalEntry, credentials, fileManager)
                        when (conflictingServerIcalEntryResult) {

                            // failed will be kept for another retry TODO: Review if this is sufficient in future
                            is GetResourceResult.Failed -> Unit   // Retry

                            // Resource wasn't found, we delete the local copy, this should have been Success though
                            GetResourceResult.NotFound -> icalEntryRepository.insertOrUpdateIcalEntry(dirtyIcalEntry.copy(syncState = SyncState.REMOTE_DELETED_LOCAL_TRASHBIN))

                            // The new entry was fetched
                            is GetResourceResult.Success -> {
                                // server returns updated entry
                                icalEntryRepository.insertOrUpdateIcalEntry(dirtyIcalEntry.copy(syncState = SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED))
                            }
                        }
                    }

                    is DeleteResourceResult.Failed -> Unit   // Retry another time. TODO: Consider storing information why the delete failed
                }
            }
        }
    }

    private suspend fun pushAttachments(icalEntry: IcalEntry, calendar: Calendar): IcalEntry {
        val updatedAttachments = icalEntry.attachments.map { attachment ->
            if (!attachment.isInline && attachment.syncState == AttachmentSyncState.LOCAL_MODIFIED && attachment.localPath != null) {
                val fileName = "${attachment.uid}_${attachment.fileName ?: "file"}"
                val uploadBaseUrl = calendar.attachmentCollectionUrl ?: calendar.url
                val safeTargetUrl = Url(uploadBaseUrl.toString().trimEnd('/') + "/" + fileName)

                val bytes = fileManager.readAttachment(attachment.localPath)
                if (uploadFileMultiplatform(client, safeTargetUrl, bytes, attachment.mimeType, credentials).isSuccess()) {        // TODO: Instead of handling only success here, inform user in case of a problem
                    val syncedAttachment = attachment.copy(remoteUrl = safeTargetUrl.toString(), syncState = AttachmentSyncState.SYNCED)
                    icalEntryRepository.insertOrUpdateAttachment(syncedAttachment)
                    syncedAttachment
                } else {
                    attachment
                }
            } else {
                attachment
            }
        }
        return icalEntry.copy(attachments = updatedAttachments)
    }
}
