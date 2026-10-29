package at.techbee.spectacled.screens.core.domain.repository

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.UpcomingAlarm
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

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

    /**
     * Copies the given entries (and, recursively, their subtasks) to [targetCalendarId], which may
     * also be their current collection. Unlike a move, each copy gets a new uid (copied subtasks
     * are re-linked to their copied parent) and the originals are left untouched.
     * Returns the ids of the copies. Their attachments still point to the originals' local files.
     */
    suspend fun copyIcalEntriesToCalendar(icalEntryIds: List<Long>, targetCalendarId: Long): List<Long>

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

    // Alarms - stored and synced as part of the entry (IcalEntry.alarms), these only cover the
    // local notification state.

    /**
     * Alarms that are due to be notified in [from] until [until] (exclusive), earliest first and
     * at most [limit]. Skips alarms already notified, actions that don't notify (EMAIL, ...), and
     * entries that are deleted, done, cancelled or recurring.
     */
    suspend fun getUpcomingAlarms(from: Instant, until: Instant, limit: Long): List<UpcomingAlarm>

    /** Records that a notification was shown for the stored alarm [alarmId]. Local only, never synced. */
    suspend fun markAlarmNotified(alarmId: Long, notifiedAt: Instant)

    /** Marks every alarm due before [before] that was never notified, in one statement. */
    suspend fun markAlarmsNotifiedBefore(before: Instant, notifiedAt: Instant)
}
