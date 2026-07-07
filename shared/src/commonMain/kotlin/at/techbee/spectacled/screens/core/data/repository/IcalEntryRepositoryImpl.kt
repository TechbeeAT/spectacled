package at.techbee.spectacled.screens.core.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import at.techbee.spectacled.screens.core.mapper.dto.CATEGORY_SPLIT_DELIMITER
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.core.mapper.dto.toDto
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

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
                .map { query -> query.awaitAsList().map { it.toDomain() } }
        }
    }

    override fun getIcalEntryByUidFlow(uid: String): Flow<IcalEntry?> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getIcalEntryByUid(uid).asFlow()
                .map { query ->
                    val dto = query.awaitAsOneOrNull() ?: return@map null
                    val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
                    dto.toDomain(attachments)
                }
        }
    }

    override suspend fun getIcalEntryById(id: Long): IcalEntry? {
        val db = getDatabase()
        val dto = db.icalentry_dtoQueries.getIcalEntryById(id).awaitAsOneOrNull() ?: return null
        val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
        return dto.toDomain(attachments)
    }

    override suspend fun getIcalEntryByUid(uid: String): IcalEntry? {
        val db = getDatabase()
        val dto = db.icalentry_dtoQueries.getIcalEntryByUid(uid).awaitAsOneOrNull() ?: return null
        val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
        return dto.toDomain(attachments)
    }

    override suspend fun getIcalEntryByHref(href: Url): IcalEntry? {
        val db = getDatabase()
        val dto = db.icalentry_dtoQueries.getIcalEntryByHref(href.toString()).awaitAsOneOrNull() ?: return null
        val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
        return dto.toDomain(attachments)
    }

    override suspend fun getDirtyIcalEntriesByCalendar(calendarId: Long): List<IcalEntry> {
        val db = getDatabase()
        return db.icalentry_dtoQueries.getDirtyIcalEntriesByCalendar(calendarId).awaitAsList().map { dto ->
            val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
            dto.toDomain(attachments)
        }
    }

    override suspend fun getIcalEntriesByHrefs(hrefs: List<Url>): List<IcalEntry> {
        val db = getDatabase()
        return db.icalentry_dtoQueries.getIcalEntriesByHrefs(hrefs.map { it.toString() }).awaitAsList().map { dto ->
            val attachments = db.attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
            dto.toDomain(attachments)
        }
    }

    override suspend fun getIcalEntriesByCalendar(calendarId: Long): List<IcalEntry> {
        val db = getDatabase()
        return db.icalentry_dtoQueries.getIcalEntriesByCalendar(calendarId).awaitAsList().map { it.toDomain() }
    }

    override suspend fun getDeletedDeltaHrefs(
        calendarId: Long,
        allServerHrefs: List<Url>
    ): List<Url> {
        return getDatabase()
            .icalentry_dtoQueries.getDeletedDeltaHrefs(calendarId, allServerHrefs.map { it.toString() })
            .awaitAsList()
            .mapNotNull { result -> result.href?.let { hrefString -> Url(hrefString) } }
    }

    override fun getAllColors(): Flow<List<Color>> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getAllColors().asFlow()
                .map { query -> query.awaitAsList().map { Color(it) } }
        }
    }

    override fun getAllCategories(): Flow<List<String>> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getAllCategories().asFlow()
                .map { query ->
                    val allCategories = mutableSetOf<String>()
                    query.awaitAsList().let { unsplitCategories ->
                        unsplitCategories.forEach { allCategories.addAll(it.split(CATEGORY_SPLIT_DELIMITER)) }
                    }
                    allCategories.toList()
                }
        }
    }

    override fun getLastUsedTimezones(): Flow<List<String>> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getLastUsedTimezones().asFlow()
                .map { query -> query.awaitAsList() }
        }
    }

    override fun getSubtasksByParentUid(parentUid: String): Flow<List<IcalEntry>> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getSubtasksByParentUid(parentUid).asFlow()
                .map { query -> query.awaitAsList().map { it.toDomain() } }
        }
    }

    override suspend fun insertOrUpdateIcalEntry(icalEntry: IcalEntry): IcalEntry {

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
        val entryId = db.icalentry_dtoQueries.getIcalEntryByUid(icalEntry.uid).executeAsOne().id

        // Handle attachments within the same transaction
        val existingAttachments = db.attachment_dtoQueries.getAttachmentsForEntry(entryId).executeAsList()
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
                    syncState = attachmentDto.syncState
                )
            }
        }
        
        return getIcalEntryByUid(icalEntry.uid)!!
    }

    override suspend fun markAsDeleted(ids: List<Long>) {
        getDatabase().icalentry_dtoQueries.markAsDeleted(ids)
    }

    override suspend fun updateProgress(id: Long, percentComplete: Long, status: Status?, lastModified: IcsDateTime?, syncState: SyncState) {
        getDatabase().icalentry_dtoQueries.updateProgress(
            newPercent = percentComplete,
            newStatus = status?.name,
            lastModified = lastModified?.let { formatIcsDateTime(it)?.first },
            syncState = syncState.name,
            id = id
        )
    }

    override suspend fun updateOrderNo(sortedIcalEntryIds: List<Long>) {
        getDatabase().icalentry_dtoQueries.transaction {
            sortedIcalEntryIds.forEachIndexed { index, icalEntryId ->
                getDatabase().icalentry_dtoQueries.updateOrderNo(orderNo = index.toLong(), id = icalEntryId)
            }
        }
    }

    override suspend fun updateColor(id: Long, color: Color?, lastModified: IcsDateTime?, syncState: SyncState) {
        getDatabase().icalentry_dtoQueries.updateColor(
            newColor = color?.toArgb()?.toLong(),
            lastModified = lastModified?.let { formatIcsDateTime(it)?.first },
            syncState = syncState.name,
            id = id
        )
    }

    override suspend fun updateCategory(id: Long, categories: List<String>, lastModified: IcsDateTime?, syncState: SyncState) {
        getDatabase().icalentry_dtoQueries.updateCategory(
            newCategories = categories.joinToString(CATEGORY_SPLIT_DELIMITER).ifEmpty { null },
            lastModified = lastModified?.let { formatIcsDateTime(it)?.first },
            syncState = syncState.name,
            id = id
        )
    }

    override suspend fun deleteTrashed(cutoffDateTime: IcsDateTime) {    // TODO: Test again!
        getDatabase().icalentry_dtoQueries.deleteTrashed(formatIcsDateTime(cutoffDateTime)?.first)
    }

    override suspend fun updateSyncMetadata(
        etag: String?,
        href: Url?,
        syncState: SyncState?,
        id: Long
    ) {
        getDatabase().icalentry_dtoQueries.updateSyncMetadata(etag, href?.toString(), syncState?.name, id)
    }

    override suspend fun insertOrUpdateAttachment(attachment: Attachment) {
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
            syncState = dto.syncState
        )
    }

    override suspend fun deleteAttachment(id: Long) {
        getDatabase().attachment_dtoQueries.deleteAttachment(id)
    }

    override suspend fun getAttachmentsForEntry(entryId: Long): List<Attachment> {
        return getDatabase().attachment_dtoQueries.getAttachmentsForEntry(entryId).awaitAsList().map { it.toDomain() }
    }
}
