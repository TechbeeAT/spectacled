package at.techbee.spectacled.screens.core.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.AttachmentSyncState
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import at.techbee.spectacled.screens.core.ioDispatcher
import at.techbee.spectacled.screens.core.mapper.dto.CATEGORY_SPLIT_DELIMITER
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.core.mapper.dto.toDto
import at.techbee.spectacled.screens.core.mapper.ics.escapeIcsValue
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import at.techbee.spectacled.screens.core.mapper.ics.splitIcsList
import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// Keep well below SQLite's default SQLITE_MAX_VARIABLE_NUMBER (999 on older builds) so an
// `IN (?, ?, …)` expansion of entry ids never overflows the per-statement parameter limit.
private const val ATTACHMENT_QUERY_CHUNK_SIZE = 500

@OptIn(ExperimentalCoroutinesApi::class)
class IcalEntryRepositoryImpl(
    private val databaseDriverFactory: DatabaseDriverFactory
) : IcalEntryRepository {

    private suspend fun getDatabase() = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)

    private val dbFlow = flow {
        emit(getDatabase())
    }

    override fun getIcalEntriesByCalendarFlow(calendarId: Long): Flow<List<IcalEntry>> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getIcalEntriesByCalendar(calendarId).asFlow()
                .map { query ->
                    val icalEntryDtos = query.awaitAsList()
                    val attachmentsByEntryId = attachmentsGroupedByEntryId(icalEntryDtos.map { it.id })
                    icalEntryDtos.map { icalEntryDto ->
                        icalEntryDto.toDomain(attachmentsByEntryId[icalEntryDto.id] ?: emptyList())
                    }
                }
        }.flowOn(ioDispatcher)
    }

    override fun getIcalEntryByUidFlow(calendarId: Long, uid: String): Flow<IcalEntry?> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getIcalEntryByUid(calendarId, uid).asFlow()
                .map { query ->
                    val dto = query.awaitAsOneOrNull() ?: return@map null
                    val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
                    dto.toDomain(attachments)
                }
        }.flowOn(ioDispatcher)
    }

    override suspend fun getIcalEntryById(id: Long): IcalEntry? = withContext(ioDispatcher) {
            val db = getDatabase()
            val dto = db.icalentry_dtoQueries.getIcalEntryById(id).awaitAsOneOrNull() ?: return@withContext null
            val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
            dto.toDomain(attachments)
    }

    override suspend fun getIcalEntryByUid(calendarId: Long, uid: String): IcalEntry? = withContext(ioDispatcher) {
        val db = getDatabase()
        val dto = db.icalentry_dtoQueries.getIcalEntryByUid(calendarId, uid).awaitAsOneOrNull() ?: return@withContext null
        val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
        dto.toDomain(attachments)
    }

    override suspend fun getIcalEntryByHref(href: Url): IcalEntry? = withContext(ioDispatcher) {
        val db = getDatabase()
        val dto = db.icalentry_dtoQueries.getIcalEntryByHref(href.toString()).awaitAsOneOrNull() ?: return@withContext null
        val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
        dto.toDomain(attachments)
    }

    private suspend fun attachmentsGroupedByEntryId(entryIds: List<Long>): Map<Long, List<Attachment>> {
        if (entryIds.isEmpty()) return emptyMap()

        val db = getDatabase()
        // `getAttachmentsForEntries` uses `IN ?`, which binds one host parameter per id.
        // SQLite caps the number of parameters per statement (SQLITE_MAX_VARIABLE_NUMBER,
        // as low as 999 on older builds), so chunk to stay safely under the limit for
        // calendars with many entries.
        return entryIds.chunked(ATTACHMENT_QUERY_CHUNK_SIZE)
            .flatMap { chunk -> db.attachment_dtoQueries.getAttachmentsForEntries(chunk).awaitAsList() }
            .map { it.toDomain() }
            .groupBy { it.icalEntryId }
    }

    override suspend fun getDirtyIcalEntriesByCalendar(calendarId: Long): List<IcalEntry> = withContext(ioDispatcher) {
        val db = getDatabase()
        val icalEntryDtos = db.icalentry_dtoQueries.getDirtyIcalEntriesByCalendar(calendarId).awaitAsList()
        val attachmentsByEntryId = attachmentsGroupedByEntryId(icalEntryDtos.map { it.id })
        icalEntryDtos.map { icalEntryDto -> icalEntryDto.toDomain(attachmentsByEntryId[icalEntryDto.id] ?: emptyList()) }
    }

    override suspend fun getIcalEntriesByHrefs(hrefs: List<Url>): List<IcalEntry> = withContext(ioDispatcher) {
        val db = getDatabase()
        val icalEntryDtos = db.icalentry_dtoQueries.getIcalEntriesByHrefs(hrefs.map { it.toString() }).awaitAsList()
        val attachmentsByEntryId = attachmentsGroupedByEntryId(icalEntryDtos.map { it.id })
        icalEntryDtos.map { icalEntryDto -> icalEntryDto.toDomain(attachmentsByEntryId[icalEntryDto.id] ?: emptyList()) }
    }

    override suspend fun getIcalEntriesByCalendar(calendarId: Long): List<IcalEntry> = withContext(ioDispatcher) {
        val icalEntryDtos = getDatabase().icalentry_dtoQueries.getIcalEntriesByCalendar(calendarId).awaitAsList()
        val attachmentsByEntryId = attachmentsGroupedByEntryId(icalEntryDtos.map { it.id })
        icalEntryDtos.map { it.toDomain(attachmentsByEntryId[it.id] ?: emptyList()) }
    }

    override suspend fun getDeletedDeltaHrefs(
        calendarId: Long,
        allServerHrefs: List<Url>
    ): List<Url> = withContext(ioDispatcher) {
        getDatabase()
            .icalentry_dtoQueries.getDeletedDeltaHrefs(calendarId, allServerHrefs.map { it.toString() })
            .awaitAsList()
            .mapNotNull { result -> result.href?.let { hrefString -> Url(hrefString) } }
    }

    override fun getAllColors(): Flow<List<Color>> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getAllColors().asFlow()
                .map { query -> query.awaitAsList().map { Color(it) } }
        }.flowOn(ioDispatcher)
    }

    override fun getAllCategories(): Flow<List<String>> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getAllCategories().asFlow()
                .map { query ->
                    val allCategories = mutableSetOf<String>()
                    query.awaitAsList().let { unsplitCategories ->
                        unsplitCategories.forEach { allCategories.addAll(splitIcsList(it)) }
                    }
                    allCategories.toList()
                }
        }.flowOn(ioDispatcher)
    }

    override fun getLastUsedTimezones(): Flow<List<String>> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getLastUsedTimezones().asFlow()
                .map { query -> query.awaitAsList() }
        }.flowOn(ioDispatcher)
    }

    override fun getSubtasksByParentUid(calendarId: Long, parentUid: String): Flow<List<IcalEntry>> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getSubtasksByParentUid(calendarId, parentUid).asFlow()
                .map { query ->
                    val icalEntryDtos = query.awaitAsList()
                    val attachmentsByEntryId = attachmentsGroupedByEntryId(icalEntryDtos.map { it.id })
                    icalEntryDtos.map { icalEntryDto ->
                        icalEntryDto.toDomain(attachmentsByEntryId[icalEntryDto.id] ?: emptyList())
                    }
                }
        }.flowOn(ioDispatcher)
    }

    override suspend fun insertOrUpdateIcalEntry(icalEntry: IcalEntry): IcalEntry = withContext(ioDispatcher) {

        val icalEntryDto = icalEntry.toDto()
        val db = getDatabase()

        db.transaction {

            // first update, if the UID doesn't exist, this is ignored
            db.icalentry_dtoQueries.updateIcalEntry(
                calendarId = icalEntryDto.calendarId,
                uid = icalEntryDto.uid,
                summary = icalEntryDto.summary,
                description = icalEntryDto.description,
                dtstart = icalEntryDto.dtstart,
                dtStartTimeZone = icalEntryDto.dtStartTimeZone,
                due = icalEntryDto.due,
                dueTimeZone = icalEntryDto.dueTimeZone,
                completed = icalEntryDto.completed,
                completedTimeZone = icalEntryDto.completedTimeZone,
                dtstamp = icalEntryDto.dtstamp,
                color = icalEntryDto.color,
                sequence = icalEntryDto.sequence,
                status = icalEntryDto.status,
                percentComplete = icalEntryDto.percentComplete,
                priority = icalEntryDto.priority,
                classification = icalEntryDto.classification,
                categories = icalEntryDto.categories,
                created = icalEntryDto.created,
                lastModified = icalEntryDto.lastModified,
                extraProperties = icalEntryDto.extraProperties,
                syncState = icalEntryDto.syncState,
                etag = icalEntryDto.etag,
                href = icalEntryDto.href,
                calendarComponent = icalEntryDto.calendarComponent,
                parentUid = icalEntryDto.parentUid,
                relType = icalEntryDto.relType,
                url = icalEntryDto.url
            )
            // insert, but if the UID exists, it will be ignored
            db.icalentry_dtoQueries.insertIcalEntry(
                calendarId = icalEntryDto.calendarId,
                uid = icalEntryDto.uid,
                summary = icalEntryDto.summary,
                description = icalEntryDto.description,
                dtstart = icalEntryDto.dtstart,
                dtStartTimeZone = icalEntryDto.dtStartTimeZone,
                due = icalEntryDto.due,
                dueTimeZone = icalEntryDto.dueTimeZone,
                completed = icalEntryDto.completed,
                completedTimeZone = icalEntryDto.completedTimeZone,
                dtstamp = icalEntryDto.dtstamp,
                color = icalEntryDto.color,
                sequence = icalEntryDto.sequence,
                status = icalEntryDto.status,
                percentComplete = icalEntryDto.percentComplete,
                priority = icalEntryDto.priority,
                classification = icalEntryDto.classification,
                categories = icalEntryDto.categories,
                created = icalEntryDto.created,
                lastModified = icalEntryDto.lastModified,
                extraProperties = icalEntryDto.extraProperties,
                syncState = icalEntryDto.syncState,
                etag = icalEntryDto.etag,
                href = icalEntryDto.href,
                calendarComponent = icalEntryDto.calendarComponent,
                parentUid = icalEntryDto.parentUid,
                relType = icalEntryDto.relType,
                url = icalEntryDto.url
            )
        }

        // Get the actual ID of the entry (it's either the existing one or the one just inserted)
        val entryId = db.icalentry_dtoQueries.getIcalEntryByUid(icalEntry.calendarId, icalEntry.uid).awaitAsOne().id

        // Handle attachments within the same transaction
        val existingAttachments = db.attachment_dtoQueries.getAttachmentsForEntry(entryId).awaitAsList()
        val currentAttachmentUids = icalEntry.attachments.map { it.uid }.toSet()

        db.transaction {

            existingAttachments.forEach { existing ->
                if (existing.uid !in currentAttachmentUids) {
                    db.attachment_dtoQueries.deleteAttachment(existing.id)
                }
            }

            icalEntry.attachments.forEach { attachment ->
                val attachmentDto = attachment.copy(icalEntryId = entryId).toDto()
                db.attachment_dtoQueries.insertAttachment(
                    icalEntryId = attachmentDto.icalEntryId,
                    uid = attachmentDto.uid,
                    localPath = attachmentDto.localPath,
                    remoteUrl = attachmentDto.remoteUrl,
                    fileName = attachmentDto.fileName,
                    mimeType = attachmentDto.mimeType,
                    size = attachmentDto.size,
                    isInline = attachmentDto.isInline,
                    syncState = attachmentDto.syncState,
                    syncErrorMessage = attachmentDto.syncErrorMessage
                )
            }
        }
        
        getIcalEntryByUid(icalEntry.calendarId, icalEntry.uid)!!
    }

    override suspend fun markAsDeleted(ids: List<Long>) {
        withContext(ioDispatcher) {
            getDatabase().icalentry_dtoQueries.markAsDeleted(ids)
        }
    }

    override suspend fun moveIcalEntriesToCalendar(icalEntryIds: List<Long>, targetCalendarId: Long) = withContext(ioDispatcher) {

        // Re-read fresh so any attachments downloaded just before the move are picked up.
        val entriesToMove = getIcalEntriesWithSubtasks(icalEntryIds)

        // 1. Re-create each entry in the target calendar. Same uid keeps its identity; clearing
        //    href/etag makes the sync engine PUT it as a new resource into the target collection.
        entriesToMove.forEach { entry ->
            insertOrUpdateIcalEntry(
                entry.copy(
                    id = 0L,
                    calendarId = targetCalendarId,
                    href = null,
                    etag = null,
                    syncState = SyncState.LOCAL_MODIFIED,
                    attachments = entry.attachments.map { it.remappedForMove() }
                )
            )
        }

        // 2. Mark the originals deleted. The sync engine DELETEs them from the source collection;
        //    we only mark locally and let it reconcile the ordering with the server.
        markAsDeleted(entriesToMove.map { it.id })
    }

    override suspend fun getIcalEntriesWithSubtasks(icalEntryIds: List<Long>): List<IcalEntry> = withContext(ioDispatcher) {

        // Children reference the parent by uid, and we keep uids on move, so relocating the whole
        // tree preserves the RELATED-TO parent/child links inside the target collection.
        val acc = LinkedHashMap<Long, IcalEntry>()
        icalEntryIds.forEach { id ->
            getIcalEntryById(id)?.let { collectWithSubtasks(it, acc) }
        }
        acc.values.toList()
    }

    // Depth-first collection of an entry and all of its (transitive) subtasks. The visited-set keyed
    // on id guards against cycles in malformed data and against a shared child being visited twice.
    private suspend fun collectWithSubtasks(entry: IcalEntry, acc: LinkedHashMap<Long, IcalEntry>) {
        if (acc.containsKey(entry.id)) return
        acc[entry.id] = entry

        val db = getDatabase()
        db.icalentry_dtoQueries.getSubtasksByParentUid(entry.calendarId, entry.uid).awaitAsList().forEach { childDto ->
            val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(childDto.id).awaitAsList().map { it.toDomain() }
            collectWithSubtasks(childDto.toDomain(attachments), acc)
        }
    }


    // Prepares an attachment to travel with a moved entry. A local copy is re-uploaded into the
    // target collection - callers pre-download server-only attachments so they take this path. If
    // one still has no local copy (e.g. the pre-download failed), we keep its existing reference so
    // the file stays accessible; it then physically remains in the source collection.
    private fun Attachment.remappedForMove(): Attachment =
        if (localPath != null)
            copy(id = 0L, icalEntryId = 0L, remoteUrl = null, syncState = AttachmentSyncState.LOCAL_MODIFIED)
        else
            copy(id = 0L, icalEntryId = 0L, syncState = AttachmentSyncState.SYNCED)

    override suspend fun updateProgress(id: Long, percentComplete: Long, status: Status?, lastModified: IcsDateTime?, syncState: SyncState) {
        withContext(ioDispatcher) {
            getDatabase().icalentry_dtoQueries.updateProgress(
                newPercent = percentComplete,
                newStatus = status?.name,
                lastModified = lastModified?.let { formatIcsDateTime(it)?.first },
                syncState = syncState.name,
                id = id
            )
        }
    }

    override suspend fun updateOrderNo(sortedIcalEntryIds: List<Long>) {
        withContext(ioDispatcher) {
            val db = getDatabase()
            db.icalentry_dtoQueries.transaction {
                sortedIcalEntryIds.forEachIndexed { index, icalEntryId ->
                    db.icalentry_dtoQueries.updateOrderNo(orderNo = index.toLong(), id = icalEntryId)
                }
            }
        }
    }

    override suspend fun updateColor(id: Long, color: Color?, lastModified: IcsDateTime?, syncState: SyncState) {
        withContext(ioDispatcher) {
            getDatabase().icalentry_dtoQueries.updateColor(
                newColor = color?.toArgb()?.toLong(),
                lastModified = lastModified?.let { formatIcsDateTime(it)?.first },
                syncState = syncState.name,
                id = id
            )
        }
    }

    override suspend fun updateCategory(id: Long, categories: List<String>, lastModified: IcsDateTime?, syncState: SyncState) {
        withContext(ioDispatcher) {
            getDatabase().icalentry_dtoQueries.updateCategory(
                newCategories = categories.joinToString(CATEGORY_SPLIT_DELIMITER) { escapeIcsValue(it) }.ifEmpty { null },
                lastModified = lastModified?.let { formatIcsDateTime(it)?.first },
                syncState = syncState.name,
                id = id
            )
        }
    }

    override suspend fun deleteTrashed(cutoffDateTime: IcsDateTime) {    // TODO: Test again!
        withContext(ioDispatcher) {
            getDatabase().icalentry_dtoQueries.deleteTrashed(formatIcsDateTime(cutoffDateTime)?.first)
        }
    }

    override suspend fun updateSyncMetadata(
        etag: String?,
        href: Url?,
        syncState: SyncState?,
        id: Long
    ) {
        withContext(ioDispatcher) {
            getDatabase().icalentry_dtoQueries.updateSyncMetadata(etag, href?.toString(), syncState?.name, id)
        }
    }

    override suspend fun insertOrUpdateAttachment(attachment: Attachment) {
        withContext(ioDispatcher) {
            val db = getDatabase()
            val dto = attachment.toDto()
            db.attachment_dtoQueries.insertAttachment(
                icalEntryId = dto.icalEntryId,
                uid = dto.uid,
                localPath = dto.localPath,
                remoteUrl = dto.remoteUrl,
                fileName = dto.fileName,
                mimeType = dto.mimeType,
                size = dto.size,
                isInline = dto.isInline,
                syncState = dto.syncState,
                syncErrorMessage = dto.syncErrorMessage
            )
        }
    }

    override suspend fun deleteAttachment(id: Long) {
        withContext(ioDispatcher) {
            getDatabase().attachment_dtoQueries.deleteAttachment(id)
        }
    }

    override suspend fun getAttachmentsForEntry(entryId: Long): List<Attachment> = withContext(ioDispatcher) {
        getDatabase().attachment_dtoQueries.getAttachmentsForEntry(entryId).awaitAsList().map { it.toDomain() }
    }
}
