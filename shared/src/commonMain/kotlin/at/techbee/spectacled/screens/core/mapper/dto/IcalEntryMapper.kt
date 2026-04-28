package at.techbee.spectacled.screens.core.mapper.dto

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import at.techbee.spectacled.screens.core.mapper.ics.parseIcsDateTime
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import at.techbee.spectacled.screens.icalentry.domain.SyncState
import at.techbee.spectacled.sqldelight.IcalEntryDto
import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
fun IcalEntryDto.toDomain(): IcalEntry {

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
        dtStart =  parseIcsDateTime(this.dtstart, this.dtStartTimeZone),
        description = this.description,
        color = this.color?.let { Color(it) },
        sequence = this.sequence,
        dtstamp = parseIcsDateTime(this.dtstamp)?: IcsDateTime(System.now(), false),
        categories = this.categories?.split(',') ?: emptyList(),
        created = parseIcsDateTime(this.created)?: IcsDateTime(System.now(), false),
        lastModified = parseIcsDateTime(this.lastModified)?: IcsDateTime(System.now(), false),
        extraProperties = extraProps,
        orderNo = this.orderNo,
        syncState = this.syncState?.let { SyncState.entries.find { it.name == this.syncState } } ?: SyncState.LOCAL_MODIFIED,
        etag = this.etag,
        href = this.href?.let { Url(it) }
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
        dtstart = this.dtStart?.let { formatIcsDateTime(it)?.first },
        dtStartTimeZone = this.dtStart?.timeZone?.id,
        categories = this.categories.joinToString(",").ifEmpty { null },
        created = formatIcsDateTime(this.created)?.first,
        lastModified = formatIcsDateTime(this.lastModified)?.first,
        extraProperties = if(this.extraProperties.isNotEmpty()) mapperJson.encodeToString(this.extraProperties) else null,
        orderNo = this.orderNo,
        syncState = this.syncState.name,
        etag = this.etag,
        href = this.href?.toString()
    )
}
