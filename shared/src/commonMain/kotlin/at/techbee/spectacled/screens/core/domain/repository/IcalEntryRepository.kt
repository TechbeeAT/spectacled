package at.techbee.spectacled.screens.core.domain.repository

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.IcalEntry
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
}
