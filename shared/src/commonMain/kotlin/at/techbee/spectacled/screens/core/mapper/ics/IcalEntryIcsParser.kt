package at.techbee.spectacled.screens.core.mapper.ics

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.IcsProperty
import at.techbee.spectacled.screens.core.data.ics.KnownIcsParamName
import at.techbee.spectacled.screens.core.data.ics.KnownIcsPropertyName
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.Classification
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.AttachmentSyncState
import at.techbee.spectacled.screens.core.FileManager
import io.ktor.http.Url
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Instant


fun unfoldLines(input: String): List<String> =
    input
        .replace("\r\n", "\n")
        .split("\n")
        .fold(mutableListOf()) { acc, line ->
            if (line.startsWith(" ") || line.startsWith("\t")) {
                acc[acc.lastIndex] += line.drop(1)
            } else {
                acc.add(line)
            }
            acc
        }


fun parseProperty(line: String): IcsProperty {
    val (left, rawValue) = line.split(":", limit = 2)
    val parts = left.split(";")

    val name = parts.first()
    val params = parts
        .drop(1).associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        }

    val value = rawValue
        .replace("\\n", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")

    return IcsProperty(name, params, value)
}

fun parseIcsDateTime(
    value: String?,
    tzid: String? = null
): IcsDateTime? {

    if (value == null) return null

    return when {

        // DATE (YYYYMMDD)
        value.length == 8 -> {
            val date = LocalDate.parse(
                "${value.substring(0,4)}-" +
                        "${value.substring(4,6)}-" +
                        value.substring(6,8)
            )

            IcsDateTime(
                instant = date.atStartOfDayIn(TimeZone.UTC),
                isDateOnly = true,
                timeZone = null   // DATE values have no TZID semantics
            )
        }

        // UTC DATE-TIME (YYYYMMDDTHHMMSSZ)
        value.endsWith("Z") -> {
            val instant = Instant.parse(
                "${value.substring(0,4)}-" +
                        "${value.substring(4,6)}-" +
                        "${value.substring(6,8)}T" +
                        "${value.substring(9,11)}:" +
                        "${value.substring(11,13)}:" +
                        "${value.substring(13,15)}Z"
            )

            IcsDateTime(
                instant = instant,
                isDateOnly = false,
                timeZone = TimeZone.UTC
            )
        }

        // TZID provided (floating local time in specific zone)
        tzid != null -> {
            val localDateTime = LocalDateTime.parse(
                "${value.substring(0,4)}-" +
                        "${value.substring(4,6)}-" +
                        "${value.substring(6,8)}T" +
                        "${value.substring(9,11)}:" +
                        "${value.substring(11,13)}:" +
                        value.substring(13,15)
            )

            val zone = runCatching {
                TimeZone.of(tzid)
            }.getOrNull() ?: TimeZone.UTC

            IcsDateTime(
                instant = localDateTime.toInstant(zone),
                isDateOnly = false,
                timeZone = zone
            )
        }

        // Floating DATE-TIME (no Z, no TZID)
        value.length == 15 && value[8] == 'T' -> {
            val localDateTime = LocalDateTime.parse(
                "${value.substring(0,4)}-" +
                        "${value.substring(4,6)}-" +
                        "${value.substring(6,8)}T" +
                        "${value.substring(9,11)}:" +
                        "${value.substring(11,13)}:" +
                        value.substring(13,15)
            )

            // Treat floating as UTC (safe default)
            IcsDateTime(
                instant = localDateTime.toInstant(TimeZone.UTC),
                isDateOnly = false,
                timeZone = null
            )
        }

        else -> null
    }
}




fun parseIcalEntryBlock(
    lines: List<String>,
    calendarComponent: CalendarComponent,
    fileManager: FileManager?
): IcalEntry? {

    val knownProps = mutableMapOf<String, MutableList<IcsProperty>>()
    val extraProps = mutableListOf<RawIcsProperty>()

    lines.forEach { line ->
        val name = line.substringBefore(':')
            .substringBefore(';')

        if (name in KnownIcsPropertyName.entries.map { it.propertyName }) {
            val list = knownProps.getOrPut(name) { mutableListOf() }
            list.add(parseProperty(line))
        } else {
            extraProps += RawIcsProperty(
                name = name,
                unfoldedLine = line
            )
        }
    }

    val uid = knownProps[KnownIcsPropertyName.UID.propertyName]?.firstOrNull()?.value ?: return null

    val dtStart = knownProps[KnownIcsPropertyName.DTSTART.propertyName]?.firstOrNull()?.let { property ->
            parseIcsDateTime(property.value, property.params[KnownIcsParamName.TZID.paramName])
        }
    val due = knownProps[KnownIcsPropertyName.DUE.propertyName]?.firstOrNull()?.let { property ->
        parseIcsDateTime(property.value, property.params[KnownIcsParamName.TZID.paramName])
    }
    val completed = knownProps[KnownIcsPropertyName.COMPLETED.propertyName]?.firstOrNull()?.let { property ->
        parseIcsDateTime(property.value, property.params[KnownIcsParamName.TZID.paramName])
    }
    val dtstamp = knownProps[KnownIcsPropertyName.DTSTAMP.propertyName]?.firstOrNull()?.value?.let { parseIcsDateTime(it, null) }
    val created = knownProps[KnownIcsPropertyName.CREATED.propertyName]?.firstOrNull()?.value?.let { parseIcsDateTime(it, null) }
    val lastModified = knownProps[KnownIcsPropertyName.LAST_MODIFIED.propertyName]?.firstOrNull()?.value?.let { parseIcsDateTime(it, null) }
    val categories = knownProps[KnownIcsPropertyName.CATEGORIES.propertyName]?.firstOrNull()?.value?.split(',') ?: emptyList()

    val status = knownProps[KnownIcsPropertyName.STATUS.propertyName]?.firstOrNull()?.value?.let { Status.entries.find { status -> status.rfcName == it } }
    val classification = knownProps[KnownIcsPropertyName.CLASSIFICATION.propertyName]?.firstOrNull()?.value?.let { Classification.entries.find { classification -> classification.name == it } }
    val priority = knownProps[KnownIcsPropertyName.PRIORITY.propertyName]?.firstOrNull()?.value?.toLongOrNull()
    val percentComplete = knownProps[KnownIcsPropertyName.PERCENT_COMPLETE.propertyName]?.firstOrNull()?.value?.toLongOrNull() ?: 0

    val parentUid = knownProps[KnownIcsPropertyName.RELATED_TO.propertyName]?.firstOrNull()?.value
    val relType = knownProps[KnownIcsPropertyName.RELATED_TO.propertyName]?.firstOrNull()?.params?.get(KnownIcsParamName.RELTYPE.paramName) ?: if(parentUid != null) "PARENT" else null
    val url = knownProps[KnownIcsPropertyName.URL.propertyName]?.firstOrNull()?.value?.let { Url(it) }

    val attachments = mutableListOf<Attachment>()
    knownProps[KnownIcsPropertyName.ATTACH.propertyName]?.forEach { prop ->
        val valueType = prop.params[KnownIcsParamName.VALUE.paramName]
        val encoding = prop.params[KnownIcsParamName.ENCODING.paramName]
        val mimeType = prop.params[KnownIcsParamName.FMTTYPE.paramName]
        val fileName = prop.params[KnownIcsParamName.FILENAME.paramName]

        if (valueType == "BINARY" && encoding == "BASE64") {
            fileManager?.let { fm ->
                try {
                    @OptIn(ExperimentalEncodingApi::class)
                    val bytes = Base64.decode(prop.value.replace("\r\n", "").replace("\n", "").trim())
                    val actualFileName = fileName ?: "attachment_${uid.take(8)}_${attachments.size}"
                    val localPath = fm.saveAttachment(actualFileName, bytes)
                    attachments.add(
                        Attachment(
                            localPath = localPath,
                            fileName = actualFileName,
                            mimeType = mimeType,
                            size = bytes.size.toLong(),
                            syncState = AttachmentSyncState.LOCAL_MODIFIED // It's new locally from an inline block
                        )
                    )
                } catch (_: Exception) {
                    // Log error or skip
                }
            }
        } else {
            // Assume it's a URI if not binary
            attachments.add(
                Attachment(
                    remoteUrl = prop.value,
                    fileName = fileName ?: prop.value.substringAfterLast('/'),
                    mimeType = mimeType,
                    syncState = AttachmentSyncState.PENDING_DOWNLOAD
                )
            )
        }
    }

    return IcalEntry(
        uid = uid,
        summary = knownProps[KnownIcsPropertyName.SUMMARY.propertyName]?.firstOrNull()?.value,
        description = knownProps[KnownIcsPropertyName.DESCRIPTION.propertyName]?.firstOrNull()?.value,
        color = knownProps[KnownIcsPropertyName.COLOR.propertyName]?.firstOrNull()?.value?.toLongOrNull()?.let(::Color),
        sequence = knownProps[KnownIcsPropertyName.SEQUENCE.propertyName]?.firstOrNull()?.value?.toLongOrNull(),
        dtStart = dtStart,
        due = due,
        completed = completed,
        status = status,
        classification = classification,
        priority = priority,
        percentComplete = percentComplete,
        categories = categories,
        dtstamp = dtstamp ?: created ?: IcsDateTime(Clock.System.now(), false),
        created = created ?: IcsDateTime(Clock.System.now(), false),
        lastModified = lastModified,
        extraProperties = extraProps,
        attachments = attachments,
        syncState = SyncState.SYNCED,
        calendarComponent = calendarComponent,
        parentUid = parentUid,
        relType = relType,
        url = url
    )
}

fun extractComponents(
    lines: List<String>,
    calendarComponent: CalendarComponent
): List<List<String>> {
    val components = mutableListOf<List<String>>()
    var current: MutableList<String>? = null

    lines.forEach { line ->
        when (line) {
            "BEGIN:${calendarComponent.name}" -> {
                current = mutableListOf()
            }
            "END:${calendarComponent.name}" -> {
                current?.let { components += it }
                current = null
            }
            else -> {
                current?.add(line)
            }
        }
    }
    return components
}

fun parseIcalEntries(
    ics: String,
    calendarComponent: CalendarComponent?,
    fileManager: FileManager? = null
): List<IcalEntry> {
    if(calendarComponent == null)
        return emptyList()

    val lines = unfoldLines(ics)

    val calendarComponentBlocks = extractComponents(lines, calendarComponent)

    return calendarComponentBlocks.mapNotNull { parseIcalEntryBlock(it, calendarComponent, fileManager) }
}
