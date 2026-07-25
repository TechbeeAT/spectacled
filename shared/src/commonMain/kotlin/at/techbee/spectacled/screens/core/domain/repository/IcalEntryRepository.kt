package at.techbee.spectacled.screens.core.domain.repository

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.PendingRemoteFileDeletion
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow

interface IcalEntryRepository {

    fun getIcalEntriesByCalendarFlow(calendarId: Long): Flow<List<IcalEntry>>
    fun getIcalEntryByUidFlow(calendarId: Long, uid: String): Flow<IcalEntry?>
    fun getAllColors(): Flow<List<Color>>
    fun getAllCategories(): Flow<List<String>>
    fun getLastUsedTimezones(): Flow<List<String>>
    fun getSubtasksByParentUid(calendarId: Long, parentUid: String): Flow<List<IcalEntry>>

    suspend fun getIcalEntryById(id: Long): IcalEntry?
    suspend fun getIcalEntryByUid(calendarId: Long, uid: String): IcalEntry?
    suspend fun getIcalEntryByHref(href: Url): IcalEntry?
    suspend fun getDirtyIcalEntriesByCalendar(calendarId: Long): List<IcalEntry>
    suspend fun getIcalEntriesByHrefs(hrefs: List<Url>): List<IcalEntry>
    suspend fun getDeletedDeltaHrefs(calendarId: Long, allServerHrefs: List<Url>): List<Url>
    suspend fun getIcalEntriesByCalendar(calendarId: Long): List<IcalEntry>
    
    suspend fun insertOrUpdateIcalEntry(icalEntry: IcalEntry): IcalEntry
    suspend fun markAsDeleted(ids: List<Long>)

    /**
     * Returns the given entries together with all of their (transitive) subtasks, de-duplicated,
     * each with its attachments loaded. Used by the move flow both to pre-process attachments and
     * to relocate the whole subtree.
     */
    suspend fun getIcalEntriesWithSubtasks(icalEntryIds: List<Long>): List<IcalEntry>

    /**
     * Moves the given entries (and, recursively, their subtasks) to [targetCalendarId].
     *
     * CalDAV has no reliable cross-collection move, so each entry is re-created in the target
     * collection (keeping its uid, hence its identity and parent/child links) and the original is
     * marked deleted. Both sides are only marked locally - the sync engine reconciles the server.
     */
    suspend fun moveIcalEntriesToCalendar(icalEntryIds: List<Long>, targetCalendarId: Long)

    suspend fun updateProgress(id: Long, percentComplete: Long, status: Status?, lastModified: IcsDateTime?, syncState: SyncState)
    suspend fun updateOrderNo(sortedIcalEntryIds: List<Long>)
    suspend fun updateColor(id: Long, color: Color?, lastModified: IcsDateTime?, syncState: SyncState)
    suspend fun updateCategory(id: Long, categories: List<String>, lastModified: IcsDateTime?, syncState: SyncState)
    suspend fun deleteTrashed(cutoffDateTime: IcsDateTime)
    suspend fun updateSyncMetadata(etag: String?, href: Url?, syncState: SyncState?, id: Long)

    // Attachments
    suspend fun insertOrUpdateAttachment(attachment: Attachment)
    suspend fun deleteAttachment(id: Long)
    suspend fun getAttachmentsForEntry(entryId: Long): List<Attachment>

    // Remote attachment-blob cleanup queue
    /** Queues attachment blob [remoteUrls] (in [calendarId]'s collection) for later deletion by sync. */
    suspend fun enqueueRemoteFileDeletions(calendarId: Long, remoteUrls: List<String>)
    suspend fun getPendingRemoteFileDeletions(calendarId: Long): List<PendingRemoteFileDeletion>
    suspend fun deletePendingRemoteFileDeletion(id: Long)
}
