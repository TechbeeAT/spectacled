package at.techbee.spectacled.screens.core.data.webdav

import at.techbee.spectacled.screens.core.FileManager
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.IcalEntry
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url

/**
 * The set of CalDAV server operations the SyncCoordinator needs, behind an interface so the
 * sync/conflict state machine can be exercised in tests without a real server. The production
 * implementation ([DefaultWebDavRemoteDataSource]) just delegates to the existing top-level
 * *Multiplatform functions; the transport concerns (HttpClient, Credentials, FileManager) are
 * held by the implementation so callers pass only the semantic arguments.
 */
interface WebDavRemoteDataSource {
    suspend fun syncCollection(calendar: Calendar): MultigetSyncCollectionResult
    suspend fun multigetResourceHrefs(calendar: Calendar): MultigetResourceHrefETagResult
    suspend fun fetchSingleEntry(calendar: Calendar, href: Url): MultigetResourceResult
    suspend fun putResource(calendar: Calendar, icalEntry: IcalEntry): PutResourceResult
    suspend fun getResource(calendar: Calendar, icalEntry: IcalEntry): GetResourceResult
    suspend fun deleteResource(calendar: Calendar, icalEntry: IcalEntry): DeleteResourceResult
    suspend fun uploadFile(targetUrl: Url, bytes: ByteArray, mimeType: String?): HttpStatusCode
}

class DefaultWebDavRemoteDataSource(
    private val client: HttpClient,
    private val credentials: Credentials?,
    private val fileManager: FileManager?,
) : WebDavRemoteDataSource {

    override suspend fun syncCollection(calendar: Calendar) =
        syncCollectionMultiplatform(client, calendar, credentials)

    override suspend fun multigetResourceHrefs(calendar: Calendar) =
        multigetResourceHrefsMultiplatform(client, calendar, credentials)

    override suspend fun fetchSingleEntry(calendar: Calendar, href: Url) =
        fetchSingleEntryMultiplatform(client, calendar, href, credentials, fileManager)

    override suspend fun putResource(calendar: Calendar, icalEntry: IcalEntry) =
        putResourceMultiplatform(client, calendar, icalEntry, credentials, fileManager)

    override suspend fun getResource(calendar: Calendar, icalEntry: IcalEntry) =
        getResourceMultiplatform(client, calendar, icalEntry, credentials, fileManager)

    override suspend fun deleteResource(calendar: Calendar, icalEntry: IcalEntry) =
        deleteResourceMultiplatform(client, calendar, icalEntry, credentials)

    override suspend fun uploadFile(targetUrl: Url, bytes: ByteArray, mimeType: String?) =
        uploadFileMultiplatform(client, targetUrl, bytes, mimeType, credentials)
}
