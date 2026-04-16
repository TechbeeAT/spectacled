package at.techbee.spectacled.screens.core.mapper.ics

import androidx.compose.ui.graphics.toArgb
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.KnownIcsPropertyName
import at.techbee.spectacled.screens.note.domain.Note
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock


fun foldIcsLine(line: String, limit: Int = 75): String {
    if (line.length <= limit) return line

    val sb = StringBuilder()
    var index = 0

    while (index < line.length) {
        val end = minOf(index + limit, line.length)
        sb.append(line.substring(index, end))
        if (end < line.length) sb.append("\r\n ")
        index = end
    }
    return sb.toString()
}

fun escapeIcsValue(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace(",", "\\,")
        .replace(";", "\\;")


private fun Int.pad2(): String =
    if (this < 10) "0$this" else toString()

private fun Int.pad4(): String =
    when {
        this < 10 -> "000$this"
        this < 100 -> "00$this"
        this < 1000 -> "0$this"
        else -> toString()
    }

fun formatIcsDate(date: LocalDate): String =
    buildString(8) {
        append(date.year.pad4())
        append(date.month.number.pad2())
        append(date.day.pad2())
    }

fun formatIcsDateTime(icsDateTime: IcsDateTime?): Pair<String, String?>? {

    if (icsDateTime == null) return null

    return if (icsDateTime.isDateOnly) {

        icsDateTime.instant
            .toLocalDateTime(TimeZone.UTC)
            .date
            .let { formatIcsDate(it) to "VALUE=DATE" }

    } else {

        val effectiveZone = icsDateTime.timeZone ?: TimeZone.UTC
        val dt = icsDateTime.instant.toLocalDateTime(effectiveZone)

        val value = buildString(16) {
            append(dt.year.pad4())
            append(dt.month.number.pad2())
            append(dt.day.pad2())
            append('T')
            append(dt.hour.pad2())
            append(dt.minute.pad2())
            append(dt.second.pad2())
        }

        when(icsDateTime.timeZone) {
            null -> value to null
            TimeZone.UTC -> "${value}Z" to null
            else -> value to "TZID=${icsDateTime.timeZone.id}"
        }
    }
}


fun serializeVCalendar(note: Note) = serializeVCalendar(listOf(note))

fun serializeVCalendar(notes: List<Note>): String {
    val lines = mutableListOf<String>()

    lines += "BEGIN:VCALENDAR"
    lines += "VERSION:2.0"
    lines += "PRODID:-//Techbee e.U.//CalDAV Notes 1.0//EN"

    collectTimeZones(notes).forEach { tz ->
        lines += generateVTimeZone(tz)
    }

    notes.forEach { note ->
        lines += serializeVJournal(note)
            .split("\r\n")
    }

    lines += "END:VCALENDAR"

    return lines.joinToString("\r\n", transform = ::foldIcsLine)
}


fun serializeVJournal(note: Note): String {
    val lines = mutableListOf<String>()

    lines += "BEGIN:VJOURNAL"
    lines += "${KnownIcsPropertyName.UID.propertyName}:${escapeIcsValue(note.uid)}"

    note.dtStart?.let { icsDateTime ->
        formatIcsDateTime(icsDateTime)?.let { (value, param) ->
            val paramPart = param?.let { ";$it" } ?: ""
            lines += "${KnownIcsPropertyName.DTSTART.propertyName}$paramPart:$value"
        }
    }
    note.created.let { icsDateTime ->
        formatIcsDateTime(icsDateTime)?.let { (value, _) ->
            lines += "${KnownIcsPropertyName.CREATED.propertyName}:$value"
        }
    }
    note.lastModified?.let {
        formatIcsDateTime(it)?.let { (value, _) ->
            lines += "${KnownIcsPropertyName.LASTMODIFIED.propertyName}:$value"
        }
    }

    note.dtstamp.let {
        formatIcsDateTime(it)?.let { (value, _) ->
            lines += "${KnownIcsPropertyName.DTSTAMP.propertyName}:$value"
        }
    }
    note.summary?.let { lines += "${KnownIcsPropertyName.SUMMARY.propertyName}:${escapeIcsValue(it)}" }
    note.description?.let { lines += "${KnownIcsPropertyName.DESCRIPTION.propertyName}:${escapeIcsValue(it)}" }
    note.color?.let { lines += "${KnownIcsPropertyName.COLOR.propertyName}:${it.toArgb()}" }
    if(note.categories.isNotEmpty())
        note.categories.let { lines += "${KnownIcsPropertyName.CATEGORIES.propertyName}:${it.joinToString(",")}" }
    note.sequence?.let { lines += "${KnownIcsPropertyName.SEQUENCE.propertyName}:${it}" }

    note.extraProperties.forEach {
        lines += it.unfoldedLine
    }

    lines += "END:VJOURNAL"

    return lines.joinToString("\r\n", transform = ::foldIcsLine)
}

private fun collectTimeZones(notes: List<Note>): Set<TimeZone> =
    buildSet {
        notes.forEach { note ->
            note.dtStart?.timeZone?.let { add(it) }
        }
    }.filter { it != TimeZone.UTC }.toSet()

private fun generateVTimeZone(timeZone: TimeZone): List<String> {

    val now = Clock.System.now()
    val currentOffset = timeZone.offsetAt(now)

    val totalSeconds = currentOffset.totalSeconds
    val sign = if (totalSeconds >= 0) "+" else "-"
    val absSeconds = kotlin.math.abs(totalSeconds)

    val hours = absSeconds / 3600
    val minutes = (absSeconds % 3600) / 60

    val offsetString = buildString {
        append(sign)
        append(hours.pad2())
        append(minutes.pad2())
    }

    return listOf(
        "BEGIN:VTIMEZONE",
        "TZID:${timeZone.id}",
        "BEGIN:STANDARD",
        "DTSTART:19700101T000000",
        "TZOFFSETFROM:$offsetString",
        "TZOFFSETTO:$offsetString",
        "END:STANDARD",
        "END:VTIMEZONE"
    )
}