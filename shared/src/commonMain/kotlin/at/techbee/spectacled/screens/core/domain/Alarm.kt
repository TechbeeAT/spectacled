package at.techbee.spectacled.screens.core.domain

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Which date of the parent component a relative [AlarmTrigger] refers to (RFC 5545 RELATED). */
enum class AlarmTriggerRelation { START, END }

sealed interface AlarmTrigger {
    /** TRIGGER;RELATED=START|END:<duration>, the default value type of TRIGGER. */
    data class Relative(
        val duration: IcsDuration,
        val relatedTo: AlarmTriggerRelation = AlarmTriggerRelation.START
    ) : AlarmTrigger

    /** TRIGGER;VALUE=DATE-TIME:<utc date-time>. RFC 5545 requires the value to be UTC. */
    data class Absolute(val dateTime: Instant) : AlarmTrigger
}

/**
 * A VALARM inside a VTODO (RFC 5545 §3.6.6, RFC 9074).
 *
 * Only UID, ACTION, TRIGGER and DESCRIPTION are modelled. Every other line of the block (SUMMARY,
 * REPEAT, DURATION, ATTACH, ATTENDEE, ACKNOWLEDGED, X- properties, nested components ...) is kept
 * verbatim in [extraProperties] and written back unchanged, so alarms created by other clients
 * survive a round trip even if this app can't edit them (see [isEditable]).
 */
@OptIn(ExperimentalUuidApi::class)
data class Alarm(
    /** RFC 9074 UID. Null if the received alarm had none - we don't invent one for foreign alarms. */
    val uid: String? = Uuid.random().toString(),
    val action: String? = ACTION_DISPLAY,
    /** Null if the TRIGGER is missing or unparseable; the raw line is then kept in [extraProperties]. */
    val trigger: AlarmTrigger? = null,
    val description: String? = null,
    val extraProperties: List<RawIcsProperty> = emptyList()
) {

    companion object {
        const val ACTION_DISPLAY = "DISPLAY"
        const val ACTION_AUDIO = "AUDIO"
        const val ACTION_EMAIL = "EMAIL"

        /** Actions this app turns into a local notification. EMAIL/PROCEDURE are left to the server. */
        val NOTIFYING_ACTIONS = setOf(ACTION_DISPLAY, ACTION_AUDIO)

        private val NON_EDITABLE_PROPERTY_NAMES = setOf("REPEAT", "DURATION")
    }

    /**
     * Whether the app can offer this alarm for editing: a single DISPLAY alarm with a trigger we
     * understand. Anything else is shown read-only and only preserved.
     */
    fun isEditable(): Boolean =
        action.equals(ACTION_DISPLAY, ignoreCase = true)
                && trigger != null
                && extraProperties.none { it.name.uppercase() in NON_EDITABLE_PROPERTY_NAMES }

    fun isNotifying(): Boolean = action?.uppercase() in NOTIFYING_ACTIONS

    /**
     * The instant this alarm fires for [entry], or null if it can't fire (no trigger, or the
     * related DTSTART/DUE is missing - RFC 5545 doesn't define a trigger time then).
     *
     * All-day dates and floating times have no zone of their own, they are resolved in
     * [deviceTimeZone]: an alarm "1 day before" an all-day task due on the 5th fires on the 4th
     * at local midnight wherever the user is.
     */
    fun triggerTime(entry: IcalEntry, deviceTimeZone: TimeZone = TimeZone.currentSystemDefault()): Instant? =
        when (val trigger = trigger) {
            null -> null
            is AlarmTrigger.Absolute -> trigger.dateTime
            is AlarmTrigger.Relative -> {
                val reference = when (trigger.relatedTo) {
                    AlarmTriggerRelation.START -> entry.dtStart
                    AlarmTriggerRelation.END -> if (entry.isTask()) entry.due else null
                } ?: return null
                val zone = reference.timeZone?.takeUnless { reference.isDateOnly } ?: deviceTimeZone
                trigger.duration.addTo(reference.resolvedInstant(deviceTimeZone), zone)
            }
        }
}

/** A stored alarm that is due to be shown as a notification, see IcalEntryRepository.getUpcomingAlarms. */
data class UpcomingAlarm(
    /** Row id of the stored alarm, used to mark it as notified. */
    val alarmId: Long,
    val icalEntryId: Long,
    val alarm: Alarm,
    val triggerAt: Instant
)

/** The instant of this value, resolving DATE and floating DATE-TIME values in [deviceTimeZone]. */
private fun IcsDateTime.resolvedInstant(deviceTimeZone: TimeZone): Instant =
    if (isDateOnly || timeZone == null)
        instant.toLocalDateTime(TimeZone.UTC).toInstant(deviceTimeZone)    // both are stored as UTC wall-clock
    else
        instant
