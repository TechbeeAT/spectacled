package at.techbee.spectacled.screens.core.data.webdav

import at.techbee.spectacled.screens.core.FileManager
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.IcalEntry
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url

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
    suspend fun deleteFile(targetUrl: Url, credentials: Credentials?): HttpStatusCode
}

class DefaultWebDavRemoteIcalEntryDataSource(
    private val client: HttpClient,
    private val fileManager: FileManager,
) : WebDavRemoteIcalEntryDataSource {

    override suspend fun syncCollection(calendar: Calendar, credentials: Credentials?) =
        syncCollectionMultiplatform(client, calendar, credentials)

    override suspend fun multigetResourceHrefs(calendar: Calendar, credentials: Credentials?) =
        multigetResourceHrefsMultiplatform(client, calendar, credentials)

    override suspend fun fetchSingleEntry(calendar: Calendar, href: Url, credentials: Credentials?) =
        fetchSingleEntryMultiplatform(client, calendar, href, credentials, fileManager)

    override suspend fun putResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?) =
        putResourceMultiplatform(client, calendar, icalEntry, credentials, fileManager)

    override suspend fun getResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?) =
        getResourceMultiplatform(client, calendar, icalEntry, credentials, fileManager)

    override suspend fun deleteResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?) =
        deleteResourceMultiplatform(client, calendar, icalEntry, credentials)

    override suspend fun uploadFile(targetUrl: Url, bytes: ByteArray, mimeType: String?, credentials: Credentials?) =
        uploadFileMultiplatform(client, targetUrl, bytes, mimeType, credentials)

    override suspend fun downloadFile(sourceUrl: Url, credentials: Credentials?) =
        downloadFileMultiplatform(client, sourceUrl, credentials)

    override suspend fun deleteFile(targetUrl: Url, credentials: Credentials?) =
        deleteFileMultiplatform(client, targetUrl, credentials)
}
