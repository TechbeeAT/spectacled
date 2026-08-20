package at.techbee.spectacled.screens.core.mapper.dto

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.Classification
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.mapper.ics.escapeIcsValue
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import at.techbee.spectacled.screens.core.mapper.ics.parseIcsDateTime
import at.techbee.spectacled.screens.core.mapper.ics.splitIcsList
import at.techbee.spectacled.sqldelight.IcalEntryDto
import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

const val CATEGORY_SPLIT_DELIMITER = ","

@OptIn(ExperimentalTime::class)
fun IcalEntryDto.toDomain(attachments: List<Attachment> = emptyList()): IcalEntry {

    val mapperJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    val extraProps: List<RawIcsProperty> = try {
        this.extraProperties?.let { mapperJson.decodeFromString<List<RawIcsProperty>>(it) } ?: emptyList()
    } catch (_: Exception) {
        emptyList() // Fallback if data is corrupted
    }

    return IcalEntry(
        id = this.id,
        calendarId = this.calendarId,
        uid = this.uid,
        summary = this.summary,
        dtStart = parseIcsDateTime(this.dtstart, this.dtStartTimeZone),
        due = parseIcsDateTime(this.due, this.dueTimeZone),
        completed = parseIcsDateTime(this.completed, this.completedTimeZone),
        description = this.description,
        color = this.color?.let { Color(it) },
        sequence = this.sequence,
        classification = this.classification?.let { Classification.entries.find { cl -> cl.name == it } },
        status = this.status?.let { Status.entries.find { vtodoStatus -> vtodoStatus.name == it } },
        priority = this.priority,
        percentComplete = this.percentComplete ?: 0L,
        dtstamp = parseIcsDateTime(this.dtstamp) ?: IcsDateTime.now(),
        categories = this.categories?.let { splitIcsList(it) } ?: emptyList(),
        created = parseIcsDateTime(this.created) ?: IcsDateTime.now(),
        lastModified = parseIcsDateTime(this.lastModified) ?: IcsDateTime.now(),
        extraProperties = extraProps,
        attachments = attachments,
        orderNo = this.orderNo,
        syncState = this.syncState?.let { SyncState.entries.find { it.name == this.syncState } } ?: SyncState.LOCAL_MODIFIED,
        etag = this.etag,
        href = this.href?.let { Url(it) },
        calendarComponent = CalendarComponent.entries.find { it.name == this.calendarComponent } ?: CalendarComponent.VJOURNAL,
        parentUid = this.parentUid,
        relType = this.relType,
        url = this.url?.let { Url(it) }
    )
}

@OptIn(ExperimentalTime::class)
fun IcalEntry.toDto(): IcalEntryDto {
    val mapperJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    return IcalEntryDto(
        id = this.id,
        calendarId = this.calendarId,
        uid = this.uid,
        summary = this.summary?.ifEmpty { null },
        description = this.description?.ifEmpty { null },
        dtstamp = formatIcsDateTime(this.dtstamp)?.first,
        color = this.color?.toArgb()?.toLong(),
        sequence = this.sequence,
        dtstart = this.dtStart.let { formatIcsDateTime(it)?.first },
        dtStartTimeZone = this.dtStart?.timeZone?.id,
        due = if(this.calendarComponent == CalendarComponent.VTODO) this.due?.let { formatIcsDateTime(it)?.first } else null,
        dueTimeZone = if(this.calendarComponent == CalendarComponent.VTODO) this.due?.timeZone?.id else null,
        completed = if(this.calendarComponent == CalendarComponent.VTODO) this.completed?.let { formatIcsDateTime(it)?.first } else null,
        completedTimeZone = if(this.calendarComponent == CalendarComponent.VTODO) this.completed?.timeZone?.id else null,
        status = this.status?.name,
        percentComplete = if(this.calendarComponent == CalendarComponent.VTODO) this.percentComplete else null,
        priority = if(this.calendarComponent == CalendarComponent.VTODO) this.priority else null,
        classification = this.classification?.name,
        // Escape each category so a comma inside a value stays distinguishable from the
        // delimiter - same scheme as the ICS wire format, so one escaping implementation.
        categories = this.categories.joinToString(CATEGORY_SPLIT_DELIMITER) { escapeIcsValue(it) }.ifEmpty { null },
        created = formatIcsDateTime(this.created)?.first,
        lastModified = formatIcsDateTime(this.lastModified)?.first,
        extraProperties = if(this.extraProperties.isNotEmpty()) mapperJson.encodeToString(this.extraProperties) else null,
        orderNo = this.orderNo,
        syncState = this.syncState.name,
        etag = this.etag,
        href = this.href?.toString(),
        calendarComponent = this.calendarComponent.name,
        parentUid = this.parentUid,
        relType = this.relType,
        url = this.url?.toString()
    )
}
