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
import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.AttachmentSyncState
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.UpcomingAlarm
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import at.techbee.spectacled.screens.core.ioDispatcher
import at.techbee.spectacled.screens.core.mapper.dto.CATEGORY_SPLIT_DELIMITER
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.core.mapper.dto.toDto
import at.techbee.spectacled.screens.core.mapper.dto.toIcsBlock
import at.techbee.spectacled.screens.core.mapper.ics.escapeIcsValue
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import at.techbee.spectacled.screens.core.mapper.ics.splitIcsList
import at.techbee.spectacled.sqldelight.IcalEntryDto
import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

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
                    loadIcalEntries(icalEntryDtos)
                }
        }.flowOn(ioDispatcher)
    }

    override fun getIcalEntryByUidFlow(calendarId: Long, uid: String): Flow<IcalEntry?> {
        return dbFlow.flatMapLatest { db ->
            db.icalentry_dtoQueries.getIcalEntryByUid(calendarId, uid).asFlow()
                .map { query ->
                    val dto = query.awaitAsOneOrNull() ?: return@map null
                    db.loadIcalEntry(dto)
                }
        }.flowOn(ioDispatcher)
    }

    override suspend fun getIcalEntryById(id: Long): IcalEntry? = withContext(ioDispatcher) {
            val db = getDatabase()
            val dto = db.icalentry_dtoQueries.getIcalEntryById(id).awaitAsOneOrNull() ?: return@withContext null
            db.loadIcalEntry(dto)
    }

    override suspend fun getIcalEntryByUid(calendarId: Long, uid: String): IcalEntry? = withContext(ioDispatcher) {
        val db = getDatabase()
        val dto = db.icalentry_dtoQueries.getIcalEntryByUid(calendarId, uid).awaitAsOneOrNull() ?: return@withContext null
        db.loadIcalEntry(dto)
    }

    override suspend fun getIcalEntryByHref(href: Url): IcalEntry? = withContext(ioDispatcher) {
        val db = getDatabase()
        val dto = db.icalentry_dtoQueries.getIcalEntryByHref(href.toString()).awaitAsOneOrNull() ?: return@withContext null
        db.loadIcalEntry(dto)
    }

    private suspend fun attachmentsGroupedByEntryId(entryIds: List<Long>): Map<Long, List<Attachment>> {
        if (entryIds.isEmpty()) return emptyMap()

        val db = getDatabase()
        return entryIds.chunked(500)   // chunk to make sure SQL limits for IN (...) aren't reached.
            .flatMap { chunk -> db.attachment_dtoQueries.getAttachmentsForEntries(chunk).awaitAsList() }
            .map { it.toDomain() }
            .groupBy { it.icalEntryId }
    }

    private suspend fun alarmsGroupedByEntryId(entryIds: List<Long>): Map<Long, List<Alarm>> {
        if (entryIds.isEmpty()) return emptyMap()

        val db = getDatabase()
        return entryIds.chunked(500)   // chunk to make sure SQL limits for IN (...) aren't reached.
            .flatMap { chunk -> db.alarm_dtoQueries.getAlarmsForEntries(chunk).awaitAsList() }
            .groupBy(keySelector = { it.icalEntryId }, valueTransform = { it.toDomain() })
    }

    /** Maps a single row to the domain model, with its attachments and alarms. */
    private suspend fun SpectacledDatabase.loadIcalEntry(dto: IcalEntryDto): IcalEntry {
        val attachments = attachment_dtoQueries.getAttachmentsForEntry(dto.id).awaitAsList().map { it.toDomain() }
        val alarms = alarm_dtoQueries.getAlarmsForEntry(dto.id).awaitAsList().map { it.toDomain() }
        return dto.toDomain(attachments, alarms)
    }

    /** Maps rows to the domain model, loading attachments and alarms in bulk. */
    private suspend fun loadIcalEntries(dtos: List<IcalEntryDto>): List<IcalEntry> {
        val entryIds = dtos.map { it.id }
        val attachmentsByEntryId = attachmentsGroupedByEntryId(entryIds)
        val alarmsByEntryId = alarmsGroupedByEntryId(entryIds)
        return dtos.map { dto ->
            dto.toDomain(attachmentsByEntryId[dto.id] ?: emptyList(), alarmsByEntryId[dto.id] ?: emptyList())
        }
    }

    override suspend fun getDirtyIcalEntriesByCalendar(calendarId: Long): List<IcalEntry> = withContext(ioDispatcher) {
        val db = getDatabase()
        val icalEntryDtos = db.icalentry_dtoQueries.getDirtyIcalEntriesByCalendar(calendarId).awaitAsList()
        loadIcalEntries(icalEntryDtos)
    }

    override suspend fun getIcalEntriesByHrefs(hrefs: List<Url>): List<IcalEntry> = withContext(ioDispatcher) {
        val db = getDatabase()
        val icalEntryDtos = db.icalentry_dtoQueries.getIcalEntriesByHrefs(hrefs.map { it.toString() }).awaitAsList()
        loadIcalEntries(icalEntryDtos)
    }

    override suspend fun getIcalEntriesByCalendar(calendarId: Long): List<IcalEntry> = withContext(ioDispatcher) {
        val icalEntryDtos = getDatabase().icalentry_dtoQueries.getIcalEntriesByCalendar(calendarId).awaitAsList()
        loadIcalEntries(icalEntryDtos)
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
                    loadIcalEntries(icalEntryDtos)
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
                url = icalEntryDto.url,
                location = icalEntryDto.location
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
                url = icalEntryDto.url,
                location = icalEntryDto.location
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

        saveAlarms(db, icalEntry.copy(id = entryId))

        getIcalEntryByUid(icalEntry.calendarId, icalEntry.uid)!!
    }

    /**
     * Replaces the stored alarms of [icalEntry] and (re)computes their trigger time from its
     * current DTSTART/DUE. Alarms have no stable row identity (a UID is optional), so the rows are
     * rewritten on every save. The local-only lastNotifiedAt is carried over to the alarm it
     * belongs to (same UID, or the same block if it has none) - but only while its trigger time is
     * unchanged, so an alarm moved to a new time notifies again.
     */
    private suspend fun saveAlarms(db: SpectacledDatabase, icalEntry: IcalEntry) {
        val existingAlarms = db.alarm_dtoQueries.getAlarmsForEntry(icalEntry.id).awaitAsList()

        db.transaction {
            db.alarm_dtoQueries.deleteAlarmsForEntry(icalEntry.id)

            icalEntry.alarms.forEach { alarm ->
                val triggerAt = alarm.triggerTime(icalEntry)
                val icsBlock = alarm.toIcsBlock()
                val previous = existingAlarms.firstOrNull { existing ->
                    if (alarm.uid != null) existing.uid == alarm.uid else existing.icsBlock == icsBlock
                }
                val lastNotifiedAt = previous
                    ?.takeIf { it.triggerAt == triggerAt?.toEpochMilliseconds() }
                    ?.lastNotifiedAt

                val alarmDto = alarm.toDto(icalEntry.id, triggerAt, lastNotifiedAt)
                db.alarm_dtoQueries.insertAlarm(
                    icalEntryId = alarmDto.icalEntryId,
                    uid = alarmDto.uid,
                    action = alarmDto.action,
                    icsBlock = alarmDto.icsBlock,
                    triggerAt = alarmDto.triggerAt,
                    lastNotifiedAt = alarmDto.lastNotifiedAt
                )
            }
        }
    }

    override suspend fun getUpcomingAlarms(from: Instant, until: Instant, limit: Long): List<UpcomingAlarm> = withContext(ioDispatcher) {
        val db = getDatabase()
        db.alarm_dtoQueries.getUpcomingAlarms(from.toEpochMilliseconds(), until.toEpochMilliseconds(), limit)
            .awaitAsList()
            .mapNotNull { alarmDto ->
                // Recurring entries are read-only in this app, their alarms are only preserved.
                val entry = db.icalentry_dtoQueries.getIcalEntryById(alarmDto.icalEntryId).awaitAsOneOrNull()?.toDomain()
                if (entry == null || entry.isRecurring()) return@mapNotNull null
                UpcomingAlarm(
                    alarmId = alarmDto.id,
                    icalEntryId = alarmDto.icalEntryId,
                    calendarId = entry.calendarId,
                    entryUid = entry.uid,
                    entrySummary = entry.summary,
                    alarm = alarmDto.toDomain(),
                    triggerAt = Instant.fromEpochMilliseconds(alarmDto.triggerAt ?: return@mapNotNull null)
                )
            }
    }

    override suspend fun markAlarmNotified(alarmId: Long, notifiedAt: Instant) {
        withContext(ioDispatcher) {
            getDatabase().alarm_dtoQueries.updateLastNotifiedAt(lastNotifiedAt = notifiedAt.toEpochMilliseconds(), id = alarmId)
        }
    }

    override suspend fun markAlarmsNotifiedBefore(before: Instant, notifiedAt: Instant) {
        withContext(ioDispatcher) {
            getDatabase().alarm_dtoQueries.markAlarmsNotifiedBefore(
                lastNotifiedAt = notifiedAt.toEpochMilliseconds(),
                beforeMillis = before.toEpochMilliseconds()
            )
        }
    }

    override suspend fun markAsDeleted(ids: List<Long>) {
        withContext(ioDispatcher) {
            getDatabase().icalentry_dtoQueries.markAsDeleted(ids)
        }
    }

    override suspend fun moveIcalEntriesToCalendar(icalEntryIds: List<Long>, targetCalendarId: Long) = withContext(ioDispatcher) {

        // Re-read fresh so any attachments downloaded just before the move are picked up.
        val entriesToMove = getIcalEntriesWithSubtasks(icalEntryIds)

        // 1. Re-create each entry in the target calendar. Same uid keeps its identity.
        entriesToMove.forEach { entry ->
            recreateInCalendar(entry, targetCalendarId, uid = entry.uid, parentUid = entry.parentUid)
        }

        // 2. Mark the originals deleted. The sync engine DELETEs them from the source collection;
        //    we only mark locally and let it reconcile the ordering with the server.
        markAsDeleted(entriesToMove.map { it.id })
    }

    override suspend fun copyIcalEntriesToCalendar(
        icalEntryIds: List<Long>,
        targetCalendarId: Long,
        sameCalendarSummarySuffix: String
    ): Map<Long, Long> = withContext(ioDispatcher) {

        // Re-read fresh so any attachments downloaded just before the copy are picked up.
        val entriesToCopy = getIcalEntriesWithSubtasks(icalEntryIds)

        // A copy is a new entry, so it needs a new uid - even in another collection, two entries
        // sharing a uid would be treated as the same one. Copied subtasks are re-linked to their
        // copied parent; a link to a parent that wasn't copied only survives inside its own
        // collection, elsewhere it would dangle.
        val newUids = entriesToCopy.associate { it.uid to IcalEntry.getRandomUUID() }
        val now = IcsDateTime.now()
        entriesToCopy.associate { entry ->
            // Only the entries the user picked are marked - copied subtasks hang below their
            // marked parent anyway.
            val summary = if (entry.id in icalEntryIds && entry.calendarId == targetCalendarId)
                (entry.summary ?: "") + sameCalendarSummarySuffix
            else
                entry.summary
            entry.id to recreateInCalendar(
                // Attachment uids are unique too - reusing them would re-assign the original's
                // attachment rows to the copy.
                entry = entry.copy(
                    summary = summary,
                    created = now,
                    dtstamp = now,
                    lastModified = now,
                    sequence = null,
                    attachments = entry.attachments.map { it.copy(uid = IcalEntry.getRandomUUID()) }
                ),
                targetCalendarId = targetCalendarId,
                uid = newUids.getValue(entry.uid),
                parentUid = entry.parentUid?.let { parentUid ->
                    newUids[parentUid] ?: parentUid.takeIf { entry.calendarId == targetCalendarId }
                }
            )
        }
    }

    // Writes [entry] as a new row in [targetCalendarId] under [uid]. Clearing href/etag makes the
    // sync engine PUT it as a new resource into the target collection.
    private suspend fun recreateInCalendar(entry: IcalEntry, targetCalendarId: Long, uid: String, parentUid: String?): Long {
        val db = getDatabase()
        val copy = insertOrUpdateIcalEntry(
            entry.copy(
                id = 0L,
                calendarId = targetCalendarId,
                uid = uid,
                parentUid = parentUid,
                href = null,
                etag = null,
                syncState = SyncState.LOCAL_MODIFIED,
                attachments = entry.attachments.map { it.remappedForMove() }
            )
        )
        // The copy is a new row, so saveAlarms found no previous alarms to carry the local
        // notification state over from - do it here, or an alarm that already fired would
        // count as missed and fire again.
        val originalAlarms = db.alarm_dtoQueries.getAlarmsForEntry(entry.id).awaitAsList()
        db.alarm_dtoQueries.getAlarmsForEntry(copy.id).awaitAsList().forEach { copied ->
            originalAlarms
                .firstOrNull { it.lastNotifiedAt != null && it.triggerAt == copied.triggerAt && (if (copied.uid != null) it.uid == copied.uid else it.icsBlock == copied.icsBlock) }
                ?.let { db.alarm_dtoQueries.updateLastNotifiedAt(lastNotifiedAt = it.lastNotifiedAt, id = copied.id) }
        }
        return copy.id
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
            val child = db.loadIcalEntry(childDto)
            // A deleted subtask that isn't synced yet is still in the db - it must not be
            // re-created as a live entry in a copy or move. Left behind, sync deletes it as usual.
            if (!child.syncState.isDeletedState())
                collectWithSubtasks(child, acc)
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
