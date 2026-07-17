package at.techbee.spectacled.screens.core.data.webdav

import at.techbee.spectacled.screens.core.FileManager
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.ioDispatcher
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.withContext

/**
 * All CalDAV server operations about the resources within a calendar - enumerating/syncing
 * entries, reading/writing individual entries, and transferring attachment files - behind an
 * interface so every DAV call goes through an injectable seam. Like
 * [WebDavRemoteCalendarDataSource] it is stateless with respect to credentials (passed per
 * call), so [DefaultWebDavRemoteIcalEntryDataSource] is a single injected singleton holding only
 * the transport and a [FileManager] (used to materialize inline attachments while parsing).
 *
 * The two REPORT operations (syncCollection / multigetResourceHrefs) live here rather than with
 * the calendar data source: they enumerate a calendar's entries, which is an entry concern, not
 * calendar management.
 */
interface WebDavRemoteIcalEntryDataSource {
    suspend fun syncCollection(calendar: Calendar, credentials: Credentials?): MultigetSyncCollectionResult
    suspend fun multigetResourceHrefs(calendar: Calendar, credentials: Credentials?): MultigetResourceHrefETagResult
    suspend fun fetchSingleEntry(calendar: Calendar, href: Url, credentials: Credentials?): MultigetResourceResult
    suspend fun putResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?): PutResourceResult
    suspend fun getResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?): GetResourceResult
    suspend fun deleteResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?): DeleteResourceResult
    suspend fun uploadFile(targetUrl: Url, bytes: ByteArray, mimeType: String?, credentials: Credentials?): HttpStatusCode
    suspend fun downloadFile(sourceUrl: Url, credentials: Credentials?): ByteArray?
}

/**
 * Every call dispatches its own work onto [ioDispatcher] — the network transfer plus, for the
 * fetch/put paths, the iCal parsing/serialization and inline-attachment file I/O that runs inside
 * those top-level functions. Callers can launch on Main without naming a dispatcher, matching the
 * repositories. As with [WebDavRemoteCalendarDataSource], IO is also what makes network
 * continuations resume reliably on Compose Desktop.
 */
class DefaultWebDavRemoteIcalEntryDataSource(
    private val client: HttpClient,
    private val fileManager: FileManager,
) : WebDavRemoteIcalEntryDataSource {

    override suspend fun syncCollection(calendar: Calendar, credentials: Credentials?) =
        withContext(ioDispatcher) { syncCollectionMultiplatform(client, calendar, credentials) }

    override suspend fun multigetResourceHrefs(calendar: Calendar, credentials: Credentials?) =
        withContext(ioDispatcher) { multigetResourceHrefsMultiplatform(client, calendar, credentials) }

    override suspend fun fetchSingleEntry(calendar: Calendar, href: Url, credentials: Credentials?) =
        withContext(ioDispatcher) { fetchSingleEntryMultiplatform(client, calendar, href, credentials, fileManager) }

    override suspend fun putResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?) =
        withContext(ioDispatcher) { putResourceMultiplatform(client, calendar, icalEntry, credentials, fileManager) }

    override suspend fun getResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?) =
        withContext(ioDispatcher) { getResourceMultiplatform(client, calendar, icalEntry, credentials, fileManager) }

    override suspend fun deleteResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?) =
        withContext(ioDispatcher) { deleteResourceMultiplatform(client, calendar, icalEntry, credentials) }

    override suspend fun uploadFile(targetUrl: Url, bytes: ByteArray, mimeType: String?, credentials: Credentials?) =
        withContext(ioDispatcher) { uploadFileMultiplatform(client, targetUrl, bytes, mimeType, credentials) }

    override suspend fun downloadFile(sourceUrl: Url, credentials: Credentials?) =
        withContext(ioDispatcher) { downloadFileMultiplatform(client, sourceUrl, credentials) }
}
