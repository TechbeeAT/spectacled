package at.techbee.spectacled.screens.core.mapper.ics

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.domain.AlarmTrigger
import at.techbee.spectacled.screens.core.domain.AlarmTriggerRelation
import kotlinx.datetime.TimeZone

private const val VALARM = "VALARM"
private const val PROP_UID = "UID"
private const val PROP_ACTION = "ACTION"
private const val PROP_TRIGGER = "TRIGGER"
private const val PROP_DESCRIPTION = "DESCRIPTION"
private const val PARAM_RELATED = "RELATED"
private const val PARAM_VALUE = "VALUE"

private fun String.propertyName() = substringBefore(':').substringBefore(';')

private fun String.isBeginLine() = startsWith("BEGIN:", ignoreCase = true)
private fun String.isEndLine() = startsWith("END:", ignoreCase = true)

/**
 * The (unfolded) content lines of one component, split into its own properties and its nested
 * subcomponents. Nested lines must never be read as properties of the parent: a VALARM's
 * DESCRIPTION is not the VTODO's description.
 */
internal class ComponentLines(
    /** The component's own property lines. */
    val ownLines: List<String>,
    /** Content lines of each nested VALARM, without its BEGIN/END lines. */
    val alarmBlocks: List<List<String>>,
    /** Every line (including BEGIN/END) of any other nested component, to be kept verbatim. */
    val otherNestedLines: List<String>
)

internal fun splitNestedComponents(lines: List<String>): ComponentLines {
    val ownLines = mutableListOf<String>()
    val alarmBlocks = mutableListOf<List<String>>()
    val otherNestedLines = mutableListOf<String>()

    var depth = 0
    var currentAlarm: MutableList<String>? = null

    lines.forEach { line ->
        when {
            depth == 0 && line.isBeginLine() -> {
                depth = 1
                if (line.substringAfter(':').trim().equals(VALARM, ignoreCase = true))
                    currentAlarm = mutableListOf()
                else
                    otherNestedLines += line
            }
            depth == 0 -> ownLines += line
            else -> {
                if (line.isBeginLine()) depth++
                if (line.isEndLine()) depth--

                val alarm = currentAlarm
                when {
                    depth == 0 && alarm != null -> { alarmBlocks += alarm; currentAlarm = null }
                    alarm != null -> alarm += line
                    else -> otherNestedLines += line
                }
            }
        }
    }
    // An unterminated VALARM is still kept rather than silently dropped.
    currentAlarm?.let { alarmBlocks += it }

    return ComponentLines(ownLines, alarmBlocks, otherNestedLines)
}

/**
 * Parses the content lines of a VALARM (without BEGIN/END). Never fails: whatever isn't
 * understood is kept in [Alarm.extraProperties], so the alarm is written back as received.
 */
fun parseAlarm(lines: List<String>): Alarm {
    val split = splitNestedComponents(lines)

    var uid: String? = null
    var action: String? = null
    var trigger: AlarmTrigger? = null
    var description: String? = null
    val extraProperties = mutableListOf<RawIcsProperty>()

    split.ownLines.forEach { line ->
        val name = line.propertyName()
        when {
            name.equals(PROP_UID, ignoreCase = true) && uid == null -> uid = parseProperty(line).value
            name.equals(PROP_ACTION, ignoreCase = true) && action == null -> action = parseProperty(line).value.uppercase()
            name.equals(PROP_DESCRIPTION, ignoreCase = true) && description == null -> description = parseProperty(line).value
            name.equals(PROP_TRIGGER, ignoreCase = true) && trigger == null -> {
                trigger = parseTrigger(line)
                if (trigger == null) extraProperties += RawIcsProperty(name, line)   // keep what we can't read
            }
            else -> extraProperties += RawIcsProperty(name, line)
        }
    }
    // Nested components (e.g. RFC 9074 proximity VLOCATION) go last, after all properties.
    split.otherNestedLines.forEach { extraProperties += RawIcsProperty(it.propertyName(), it) }

    return Alarm(
        uid = uid,
        action = action,
        trigger = trigger,
        description = description,
        extraProperties = extraProperties
    )
}

private fun parseTrigger(line: String): AlarmTrigger? {
    val property = parseProperty(line)
    val params = property.params.mapKeys { it.key.uppercase() }

    return if (params[PARAM_VALUE].equals("DATE-TIME", ignoreCase = true)) {
        // RFC 5545 §3.8.6.3: an absolute trigger MUST be in UTC. Anything else is kept raw.
        parseIcsDateTime(property.value.trim())
            ?.takeIf { !it.isDateOnly && it.timeZone == TimeZone.UTC }
            ?.let { AlarmTrigger.Absolute(it.instant) }
    } else {
        val relatedTo = when (params[PARAM_RELATED]?.uppercase()) {
            null, "START" -> AlarmTriggerRelation.START
            "END" -> AlarmTriggerRelation.END
            else -> return null
        }
        IcsDuration.parse(property.value)?.let { AlarmTrigger.Relative(it, relatedTo) }
    }
}

/** Serializes [alarm] to unfolded content lines, including BEGIN:VALARM and END:VALARM. */
fun serializeAlarm(alarm: Alarm): List<String> {
    val lines = mutableListOf("BEGIN:$VALARM")
    alarm.uid?.let { lines += "$PROP_UID:${escapeIcsValue(it)}" }
    alarm.action?.let { lines += "$PROP_ACTION:$it" }
    when (val trigger = alarm.trigger) {
        is AlarmTrigger.Relative -> {
            val related = if (trigger.relatedTo == AlarmTriggerRelation.END) ";$PARAM_RELATED=END" else ""
            lines += "$PROP_TRIGGER$related:${trigger.duration.format()}"
        }
        is AlarmTrigger.Absolute -> {
            val value = formatIcsDateTime(IcsDateTime(trigger.dateTime, isDateOnly = false, timeZone = TimeZone.UTC))!!.first
            lines += "$PROP_TRIGGER;$PARAM_VALUE=DATE-TIME:$value"
        }
        null -> Unit    // an unparseable TRIGGER is part of extraProperties
    }
    alarm.description?.let { lines += "$PROP_DESCRIPTION:${escapeIcsValue(it)}" }
    alarm.extraProperties.forEach { lines += it.unfoldedLine }
    lines += "END:$VALARM"
    return lines
}
