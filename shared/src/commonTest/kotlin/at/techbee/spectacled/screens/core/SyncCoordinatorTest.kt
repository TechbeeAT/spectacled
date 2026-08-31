package at.techbee.spectacled.screens.core

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.webdav.DeleteResourceResult
import at.techbee.spectacled.screens.core.data.webdav.GetResourceResult
import at.techbee.spectacled.screens.core.data.webdav.MultigetResourceHrefETagResult
import at.techbee.spectacled.screens.core.data.webdav.MultigetResourceResult
import at.techbee.spectacled.screens.core.data.webdav.MultigetSyncCollectionResult
import at.techbee.spectacled.screens.core.data.webdav.PutResourceResult
import at.techbee.spectacled.screens.core.data.webdav.WebDavRemoteIcalEntryDataSource
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.CalendarSyncError
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.PendingRemoteFileDeletion
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Drives the SyncCoordinator's conflict/sync state machine through its public
 * syncCalendarWithSyncLock entry point, with a fake WebDavRemoteIcalEntryDataSource scripting the
 * server's responses and fake repositories recording what the coordinator decided to write.
 * No real HttpClient, server, or database is involved.
 */
class SyncCoordinatorTest {

    private val calendarUrl = Url("https://example.com/calendars/test/")
    private val entryHref = Url("https://example.com/calendars/test/entry.ics")

    private fun calendar(id: Long = 1L, syncToken: String? = "token") =
        Calendar.getCalendarForPreview().copy(id = id, url = calendarUrl, syncToken = syncToken)

    private fun entry(
        id: Long = 10L,
        syncState: SyncState,
        etag: String? = "etag-old",
        href: Url? = entryHref,
        attachments: List<Attachment> = emptyList()
    ) = IcalEntry(
        id = id,
        calendarId = 1L,
        uid = "uid-$id",
        syncState = syncState,
        etag = etag,
        href = href,
        attachments = attachments,
        calendarComponent = CalendarComponent.VJOURNAL
    )

    private fun fakeSyncCoordinator(
        fakeWebDavRemoteIcalEntryDataSource: WebDavRemoteIcalEntryDataSource,
        fakeIcalEntryRepository: FakeIcalEntryRepository,
        fakeCalendarRepository: FakeCalendarRepository = FakeCalendarRepository()
    ) = SyncCoordinator(fakeCalendarRepository, fakeIcalEntryRepository, FakeFileManager(), fakeWebDavRemoteIcalEntryDataSource, credentials = null)

    // --- pushLocalChanges: LOCAL_MODIFIED ---

    @Test
    fun localModified_putSuccess_marksSynced() = runTest {
        val icalRepo = FakeIcalEntryRepository(dirty = listOf(entry(syncState = SyncState.LOCAL_MODIFIED)))
        val remote = FakeRemote(putResult = { PutResourceResult.Success(entry(syncState = SyncState.SYNCED, etag = "etag-new")) })
        val calRepo = FakeCalendarRepository()

        fakeSyncCoordinator(remote, icalRepo, calRepo).syncCalendarWithSyncLock(calendar())

        val update = icalRepo.syncMetadataUpdates.single()
        assertEquals(SyncState.SYNCED, update.syncState)
        assertEquals("etag-new", update.etag)
        assertEquals(10L, update.id)
        assertEquals(CalendarSyncStatusType.SYNCED, calRepo.lastStatusType())
    }

    @Test
    fun localModified_putConflict_serverModified_yieldsConflict() = runTest {
        val icalRepo = FakeIcalEntryRepository(dirty = listOf(entry(syncState = SyncState.LOCAL_MODIFIED)))
        val remote = FakeRemote(
            putResult = { PutResourceResult.Conflict },
            getResult = { GetResourceResult.Success(entry(syncState = SyncState.SYNCED, etag = "etag-server")) }
        )

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED, icalRepo.upserts.single().syncState)
    }

    @Test
    fun localModified_putConflict_serverDeleted_yieldsConflict() = runTest {
        val icalRepo = FakeIcalEntryRepository(dirty = listOf(entry(syncState = SyncState.LOCAL_MODIFIED)))
        val remote = FakeRemote(
            putResult = { PutResourceResult.Conflict },
            getResult = { GetResourceResult.NotFound }
        )

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED, icalRepo.upserts.single().syncState)
    }

    @Test
    fun localModified_putNotFound_yieldsServerDeletedConflict() = runTest {
        val icalRepo = FakeIcalEntryRepository(dirty = listOf(entry(syncState = SyncState.LOCAL_MODIFIED)))
        val remote = FakeRemote(putResult = { PutResourceResult.NotFound })

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED, icalRepo.upserts.single().syncState)
    }

    @Test
    fun localModified_putFailed_leavesEntryForRetry() = runTest {
        val icalRepo = FakeIcalEntryRepository(dirty = listOf(entry(syncState = SyncState.LOCAL_MODIFIED)))
        val remote = FakeRemote(putResult = { PutResourceResult.Failed(HttpStatusCode.InternalServerError, "boom") })

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        // Left untouched for a later retry - no entry write, no metadata update.
        assertTrue(icalRepo.upserts.isEmpty())
        assertTrue(icalRepo.syncMetadataUpdates.isEmpty())
    }

    // --- pushLocalChanges: LOCAL_DELETED ---

    @Test
    fun localDeleted_deleteSuccess_movesToTrashbin() = runTest {
        val icalRepo = FakeIcalEntryRepository(dirty = listOf(entry(syncState = SyncState.LOCAL_DELETED)))
        val remote = FakeRemote(deleteResult = { DeleteResourceResult.Success })

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(SyncState.REMOTE_DELETED_LOCAL_TRASHBIN, icalRepo.upserts.single().syncState)
    }

    @Test
    fun localDeleted_deleteConflict_serverModified_yieldsConflict() = runTest {
        val icalRepo = FakeIcalEntryRepository(dirty = listOf(entry(syncState = SyncState.LOCAL_DELETED)))
        val remote = FakeRemote(
            deleteResult = { DeleteResourceResult.Conflict },
            getResult = { GetResourceResult.Success(entry(syncState = SyncState.SYNCED)) }
        )

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED, icalRepo.upserts.single().syncState)
    }

    // --- pushLocalChanges: entries that should not be pushed ---

    @Test
    fun conflictAndSyncedEntries_areLeftForTheUserOrIgnored() = runTest {
        val icalRepo = FakeIcalEntryRepository(
            dirty = listOf(
                entry(id = 1, syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED),
                entry(id = 2, syncState = SyncState.SYNCED)
            )
        )
        // A remote whose every mutating call throws would blow up if these were pushed.
        val remote = FakeRemote()

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertTrue(icalRepo.upserts.isEmpty())
        assertTrue(icalRepo.syncMetadataUpdates.isEmpty())
    }

    @Test
    fun userDecidedServerWins_serverDeleted_movesToTrashbin() = runTest {
        val icalRepo = FakeIcalEntryRepository(dirty = listOf(entry(syncState = SyncState.USER_DECIDED_SERVER_WINS)))
        val remote = FakeRemote(getResult = { GetResourceResult.NotFound })

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(SyncState.REMOTE_DELETED_LOCAL_TRASHBIN, icalRepo.upserts.single().syncState)
    }

    // --- applyServerchanges: upsert (update hrefs) ---

    @Test
    fun serverUpdate_noLocalEntry_insertsAsSynced() = runTest {
        val icalRepo = FakeIcalEntryRepository(byHref = emptyMap())
        val remote = FakeRemote(
            syncCollectionResult = { MultigetSyncCollectionResult.Success("token2", mapOf(entryHref to "etag-1")) },
            fetchSingleResult = { MultigetResourceResult.Success(listOf(entry(syncState = SyncState.SYNCED))) }
        )

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(SyncState.SYNCED, icalRepo.upserts.single().syncState)
    }

    @Test
    fun serverUpdate_localModified_yieldsConflict() = runTest {
        val local = entry(syncState = SyncState.LOCAL_MODIFIED, etag = "etag-old")
        val icalRepo = FakeIcalEntryRepository(byHref = mapOf(entryHref.toString() to local))
        val remote = FakeRemote(
            syncCollectionResult = { MultigetSyncCollectionResult.Success("token2", mapOf(entryHref to "etag-new")) },
            fetchSingleResult = { MultigetResourceResult.Success(listOf(entry(syncState = SyncState.SYNCED, etag = "etag-new"))) }
        )

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED, icalRepo.upserts.single().syncState)
    }

    @Test
    fun serverUpdate_sameEtag_isSkippedWithoutFetching() = runTest {
        val local = entry(syncState = SyncState.SYNCED, etag = "etag-same")
        val icalRepo = FakeIcalEntryRepository(byHref = mapOf(entryHref.toString() to local))
        val remote = FakeRemote(
            syncCollectionResult = { MultigetSyncCollectionResult.Success("token2", mapOf(entryHref to "etag-same")) }
            // fetchSingleResult intentionally left as the throwing default - must not be called.
        )

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(0, remote.fetchCount)
        assertTrue(icalRepo.upserts.isEmpty())
    }

    // --- applyServerchanges: remove (deleted hrefs) ---

    @Test
    fun serverDeleted_syncedEntry_movesToTrashbin() = runTest {
        val icalRepo = FakeIcalEntryRepository(entriesByHrefs = listOf(entry(syncState = SyncState.SYNCED)))
        val remote = FakeRemote(
            syncCollectionResult = { MultigetSyncCollectionResult.Success("token2", mapOf(entryHref to null)) }
        )

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(SyncState.REMOTE_DELETED_LOCAL_TRASHBIN, icalRepo.upserts.single().syncState)
    }

    @Test
    fun serverDeleted_locallyModifiedEntry_yieldsConflict() = runTest {
        val icalRepo = FakeIcalEntryRepository(entriesByHrefs = listOf(entry(syncState = SyncState.LOCAL_MODIFIED)))
        val remote = FakeRemote(
            syncCollectionResult = { MultigetSyncCollectionResult.Success("token2", mapOf(entryHref to null)) }
        )

        fakeSyncCoordinator(remote, icalRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED, icalRepo.upserts.single().syncState)
    }

    // --- calendar sync status mapping ---

    @Test
    fun notAuthorized_setsNotAuthorizedStatusAndDoesNotPush() = runTest {
        val icalRepo = FakeIcalEntryRepository(dirty = listOf(entry(syncState = SyncState.LOCAL_MODIFIED)))
        val remote = FakeRemote(syncCollectionResult = { MultigetSyncCollectionResult.NotAuthorized })
        val calRepo = FakeCalendarRepository()

        fakeSyncCoordinator(remote, icalRepo, calRepo).syncCalendarWithSyncLock(calendar())

        assertEquals(CalendarSyncStatusType.NOT_AUTHORIZED, calRepo.lastStatusType())
        assertTrue(icalRepo.syncMetadataUpdates.isEmpty(), "must not push local changes when not authorized")
    }

    @Test
    fun syncTokenFailed_fallsBackToFullSyncWithoutToken() = runTest {
        val icalRepo = FakeIcalEntryRepository(
            entriesByHrefs = emptyList(),
            deletedDeltaHrefs = emptyList()
        )
        val remote = FakeRemote(
            syncCollectionResult = { MultigetSyncCollectionResult.Failed(HttpStatusCode.Conflict, CalendarSyncError.CALENDAR_NOT_FETCHABLE) },
            multigetHrefsResult = { MultigetResourceHrefETagResult.Success(emptyMap(), "fresh-token") }
        )
        val calRepo = FakeCalendarRepository()

        fakeSyncCoordinator(remote, icalRepo, calRepo).syncCalendarWithSyncLock(calendar())

        // Failed sync-token report triggers the tokenless REPORT fallback, which then succeeds.
        assertEquals(1, remote.multigetHrefsCount)
        assertEquals(CalendarSyncStatusType.SYNCED, calRepo.lastStatusType())
    }

    // --- per-calendar locking ---

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun secondConcurrentSyncOfSameCalendarIsSkipped() = runTest {
        val gate = CompletableDeferred<Unit>()
        val icalRepo = FakeIcalEntryRepository()
        val remote = FakeRemote(
            syncCollectionResult = {
                gate.await()   // hold the first sync inside its critical section
                MultigetSyncCollectionResult.Success("token2", emptyMap())
            }
        )
        val cal = calendar(id = 99L)
        val coordinator = fakeSyncCoordinator(remote, icalRepo)

        val first = launch { coordinator.syncCalendarWithSyncLock(cal) }
        runCurrent()   // let the first sync acquire the per-calendar lock and park on the gate

        coordinator.syncCalendarWithSyncLock(cal)   // second call: tryLock fails -> returns immediately
        assertEquals(1, remote.syncCollectionCount, "second concurrent sync should be skipped")

        gate.complete(Unit)
        first.join()
        assertEquals(1, remote.syncCollectionCount)
    }
}

// --- Fakes ---

private class FakeFileManager : FileManager {
    override fun getAttachmentsDirectory() = "/fake"
    override fun saveAttachment(fileName: String, bytes: ByteArray) = "/fake/$fileName"
    override fun readAttachment(path: String) = ByteArray(0)
    override fun deleteAttachment(path: String) = false
    override fun exists(path: String) = true
}

/**
 * Scriptable server. Each response is a lambda so a test can inject conflicts, failures, or
 * suspensions. Mutating calls (put/get/delete/upload/fetch) default to throwing, so a test
 * path that reaches the server unexpectedly fails loudly instead of silently passing.
 */
private class FakeRemote(
    private val syncCollectionResult: suspend () -> MultigetSyncCollectionResult =
        { MultigetSyncCollectionResult.Success("token", emptyMap()) },
    private val multigetHrefsResult: suspend () -> MultigetResourceHrefETagResult =
        { MultigetResourceHrefETagResult.Success(emptyMap(), "token") },
    // Mutating/fetch defaults throw AssertionError (an Error, so it is NOT swallowed by
    // sync()'s catch(Exception)) - an unexpected server call propagates out and fails the test.
    private val fetchSingleResult: suspend () -> MultigetResourceResult =
        { throw AssertionError("fetchSingleEntry not expected in this test") },
    private val putResult: suspend () -> PutResourceResult =
        { throw AssertionError("putResource not expected in this test") },
    private val getResult: suspend () -> GetResourceResult =
        { throw AssertionError("getResource not expected in this test") },
    private val deleteResult: suspend () -> DeleteResourceResult =
        { throw AssertionError("deleteResource not expected in this test") },
) : WebDavRemoteIcalEntryDataSource {

    var syncCollectionCount = 0
    var multigetHrefsCount = 0
    var fetchCount = 0

    override suspend fun syncCollection(calendar: Calendar, credentials: Credentials?): MultigetSyncCollectionResult {
        syncCollectionCount++
        return syncCollectionResult()
    }

    override suspend fun multigetResourceHrefs(calendar: Calendar, credentials: Credentials?): MultigetResourceHrefETagResult {
        multigetHrefsCount++
        return multigetHrefsResult()
    }

    override suspend fun fetchSingleEntry(calendar: Calendar, href: Url, credentials: Credentials?): MultigetResourceResult {
        fetchCount++
        return fetchSingleResult()
    }

    override suspend fun putResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?) = putResult()
    override suspend fun getResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?) = getResult()
    override suspend fun deleteResource(calendar: Calendar, icalEntry: IcalEntry, credentials: Credentials?) = deleteResult()
    override suspend fun uploadFile(targetUrl: Url, bytes: ByteArray, mimeType: String?, credentials: Credentials?) = HttpStatusCode.Created
    override suspend fun downloadFile(sourceUrl: Url, credentials: Credentials?): ByteArray =
        throw AssertionError("downloadFile not expected in a sync test")
    override suspend fun deleteFile(targetUrl: Url, credentials: Credentials?) = HttpStatusCode.NoContent
}

private data class SyncMetadataUpdate(val etag: String?, val href: Url?, val syncState: SyncState?, val id: Long)

private class FakeIcalEntryRepository(
    private val dirty: List<IcalEntry> = emptyList(),
    private val byHref: Map<String, IcalEntry> = emptyMap(),
    private val byUid: Map<String, IcalEntry> = emptyMap(),
    private val entriesByHrefs: List<IcalEntry> = emptyList(),
    private val deletedDeltaHrefs: List<Url> = emptyList(),
) : IcalEntryRepository {

    val upserts = mutableListOf<IcalEntry>()
    val syncMetadataUpdates = mutableListOf<SyncMetadataUpdate>()
    val attachmentUpserts = mutableListOf<Attachment>()

    override suspend fun getDirtyIcalEntriesByCalendar(calendarId: Long) = dirty
    override suspend fun getIcalEntryByHref(href: Url) = byHref[href.toString()]
    override suspend fun getIcalEntryByUid(calendarId: Long, uid: String) = byUid[uid]
    override suspend fun getIcalEntriesByHrefs(hrefs: List<Url>) = entriesByHrefs
    override suspend fun getDeletedDeltaHrefs(calendarId: Long, allServerHrefs: List<Url>) = deletedDeltaHrefs

    override suspend fun insertOrUpdateIcalEntry(icalEntry: IcalEntry): IcalEntry {
        upserts += icalEntry
        return icalEntry
    }

    override suspend fun updateSyncMetadata(etag: String?, href: Url?, syncState: SyncState?, id: Long) {
        syncMetadataUpdates += SyncMetadataUpdate(etag, href, syncState, id)
    }

    override suspend fun insertOrUpdateAttachment(attachment: Attachment) {
        attachmentUpserts += attachment
    }

    // --- not exercised by the sync path ---
    override fun getIcalEntriesByCalendarFlow(calendarId: Long): Flow<List<IcalEntry>> = TODO()
    override fun getIcalEntryByUidFlow(calendarId: Long, uid: String): Flow<IcalEntry?> = TODO()
    override fun getAllColors(): Flow<List<Color>> = TODO()
    override fun getAllCategories(): Flow<List<String>> = TODO()
    override fun getLastUsedTimezones(): Flow<List<String>> = TODO()
    override fun getSubtasksByParentUid(calendarId: Long, parentUid: String): Flow<List<IcalEntry>> = TODO()
    override suspend fun getIcalEntryById(id: Long): IcalEntry = TODO()
    override suspend fun getIcalEntriesByCalendar(calendarId: Long): List<IcalEntry> = TODO()
    override suspend fun getIcalEntriesWithSubtasks(icalEntryIds: List<Long>): List<IcalEntry> = TODO()
    override suspend fun moveIcalEntriesToCalendar(icalEntryIds: List<Long>, targetCalendarId: Long) = TODO()
    override suspend fun markAsDeleted(ids: List<Long>) = TODO()
    override suspend fun updateProgress(id: Long, percentComplete: Long, status: Status?, lastModified: IcsDateTime?, syncState: SyncState) = TODO()
    override suspend fun updateOrderNo(sortedIcalEntryIds: List<Long>) = TODO()
    override suspend fun updateColor(id: Long, color: Color?, lastModified: IcsDateTime?, syncState: SyncState) = TODO()
    override suspend fun updateCategory(id: Long, categories: List<String>, lastModified: IcsDateTime?, syncState: SyncState) = TODO()
    override suspend fun deleteTrashed(cutoffDateTime: IcsDateTime) = TODO()
    override suspend fun deleteAttachment(id: Long) = TODO()
    override suspend fun getAttachmentsForEntry(entryId: Long): List<Attachment> = TODO()
    override suspend fun enqueueRemoteFileDeletions(calendarId: Long, remoteUrls: List<String>) = TODO()
    override suspend fun deletePendingRemoteFileDeletion(id: Long) = TODO()

    // Drained on every sync - return nothing so the sync path issues no remote deletions.
    override suspend fun getPendingRemoteFileDeletions(calendarId: Long): List<PendingRemoteFileDeletion> = emptyList()
}

private class FakeCalendarRepository : CalendarRepository {

    val statuses = mutableListOf<String?>()

    fun lastStatusType(): CalendarSyncStatusType? =
        statuses.lastOrNull()?.let { CalendarSyncStatus.deserialize(it).type }

    override suspend fun updateCalendarSyncStatus(calendarSyncStatus: String?, syncToken: String?, id: Long) {
        statuses += calendarSyncStatus
    }

    override suspend fun getPrincipalForCalendar(calendarId: Long): Principal? = null

    // --- not exercised by the instance sync path ---
    override fun getAllPrincipalsFlow(): Flow<List<Principal>> = TODO()
    override fun getAllHomeCollectionsFlow(): Flow<List<HomeCollection>> = TODO()
    override fun getAllCalendarsFlow(): Flow<List<Calendar>> = TODO()
    override suspend fun getAllPrincipals(): List<Principal> = TODO()
    override suspend fun getAllHomeCollections(): List<HomeCollection> = TODO()
    override suspend fun getAllCalendars(): List<Calendar> = TODO()
    override suspend fun getCalendarById(id: Long): Calendar = TODO()
    override suspend fun getPrincipalUrlForCalendarId(calendarId: Long): String = TODO()
    override suspend fun getCalendarsForPrincipalUrl(principalUrl: String): List<Calendar> = TODO()
    override suspend fun getCalendarsByIds(calendarIds: List<Long>): List<Calendar> = TODO()
    override suspend fun upsertPrincipal(principal: Principal): Long = TODO()
    override suspend fun upsertHomeCollection(homeCollection: HomeCollection, principalUrl: Url): Long = TODO()
    override suspend fun upsertCalendar(calendar: Calendar, homeCollectionUrl: Url): Long = TODO()
    override suspend fun deletePrincipal(id: Long) = TODO()
    override suspend fun deleteCalendar(id: Long) = TODO()
}
