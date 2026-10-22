package at.techbee.spectacled.screens.core.mapper.ics

import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.domain.AlarmTrigger
import at.techbee.spectacled.screens.core.domain.AlarmTriggerRelation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class AlarmIcsMapperTest {

    private fun ics(vararg componentLines: String) = (listOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//Test//EN"
    ) + componentLines + "END:VCALENDAR").joinToString("\r\n")

    // A task as Thunderbird / Nextcloud Tasks write it: one alarm relative to DUE, one absolute,
    // and an AUDIO alarm with an ATTACH. The task itself has no DESCRIPTION.
    private val taskWithAlarms = ics(
        "BEGIN:VTODO",
        "UID:task-1",
        "DTSTAMP:20261001T080000Z",
        "SUMMARY:Pay rent",
        "DUE:20261005T090000Z",
        "BEGIN:VALARM",
        "UID:alarm-1",
        "ACTION:DISPLAY",
        "TRIGGER;RELATED=END:-PT15M",
        "DESCRIPTION:Rent is due",
        "END:VALARM",
        "BEGIN:VALARM",
        "ACTION:DISPLAY",
        "TRIGGER;VALUE=DATE-TIME:20261004T180000Z",
        "DESCRIPTION:Reminder",
        "X-MOZ-LASTACK:20261004T180100Z",
        "END:VALARM",
        "BEGIN:VALARM",
        "ACTION:AUDIO",
        "TRIGGER:-P1D",
        "ATTACH;FMTTYPE=audio/basic:ftp://example.com/pub/sounds/bell-01.aud",
        "REPEAT:4",
        "DURATION:PT15M",
        "END:VALARM",
        "END:VTODO"
    )

    @Test
    fun parse_multipleAlarms() {
        val entry = parseIcalEntries(taskWithAlarms).single()

        assertEquals(3, entry.alarms.size)

        val (relative, absolute, audio) = entry.alarms
        assertEquals("alarm-1", relative.uid)
        assertEquals(Alarm.ACTION_DISPLAY, relative.action)
        assertEquals(AlarmTrigger.Relative(IcsDuration(isNegative = true, minutes = 15), AlarmTriggerRelation.END), relative.trigger)
        assertEquals("Rent is due", relative.description)
        assertTrue(relative.isEditable())

        assertNull(absolute.uid, "a missing UID must not be invented")
        assertEquals(AlarmTrigger.Absolute(Instant.parse("2026-10-04T18:00:00Z")), absolute.trigger)
        assertEquals(listOf("X-MOZ-LASTACK:20261004T180100Z"), absolute.extraProperties.map { it.unfoldedLine })

        assertEquals(Alarm.ACTION_AUDIO, audio.action)
        assertEquals(AlarmTrigger.Relative(IcsDuration(days = 1, isNegative = true), AlarmTriggerRelation.START), audio.trigger)
        assertFalse(audio.isEditable())
        assertTrue(audio.isNotifying())
    }

    @Test
    fun parse_alarmPropertiesDoNotLeakIntoTheEntry() {
        val entry = parseIcalEntries(taskWithAlarms).single()

        assertEquals("task-1", entry.uid)
        assertEquals("Pay rent", entry.summary)
        assertNull(entry.description, "the alarm's DESCRIPTION is not the task's description")
        assertTrue(entry.attachments.isEmpty(), "the AUDIO alarm's ATTACH is not an attachment of the task")
        assertTrue(
            entry.extraProperties.none { it.name in setOf("BEGIN", "END", "ACTION", "TRIGGER", "REPEAT", "DURATION") },
            "no alarm line may end up in the entry's own extra properties: ${entry.extraProperties}"
        )
    }

    @Test
    fun roundTrip_keepsAlarmsUnchanged() {
        val entry = parseIcalEntries(taskWithAlarms).single()
        val reparsed = parseIcalEntries(serializeVCalendar(entry)).single()

        assertEquals(entry.alarms, reparsed.alarms)
        assertNull(reparsed.description)
    }

    @Test
    fun roundTrip_serializedAlarmLines() {
        val alarm = parseIcalEntries(taskWithAlarms).single().alarms.last()

        assertEquals(
            listOf(
                "BEGIN:VALARM",
                "ACTION:AUDIO",
                "TRIGGER:-P1D",
                "ATTACH;FMTTYPE=audio/basic:ftp://example.com/pub/sounds/bell-01.aud",
                "REPEAT:4",
                "DURATION:PT15M",
                "END:VALARM"
            ),
            serializeAlarm(alarm)
        )
    }

    @Test
    fun serialize_newAlarm() {
        val alarm = Alarm(
            uid = "new-alarm",
            trigger = AlarmTrigger.Relative(IcsDuration(isNegative = true, hours = 1), AlarmTriggerRelation.END),
            description = "Call Anna, Bob; and Carl"
        )

        assertEquals(
            listOf(
                "BEGIN:VALARM",
                "UID:new-alarm",
                "ACTION:DISPLAY",
                "TRIGGER;RELATED=END:-PT1H",
                "DESCRIPTION:Call Anna\\, Bob\\; and Carl",
                "END:VALARM"
            ),
            serializeAlarm(alarm)
        )
        assertEquals(alarm, parseAlarm(serializeAlarm(alarm).drop(1).dropLast(1)))
    }

    @Test
    fun serialize_absoluteTriggerIsUtc() {
        val alarm = Alarm(uid = null, trigger = AlarmTrigger.Absolute(Instant.parse("2026-10-04T18:00:00Z")), description = "x")
        assertTrue("TRIGGER;VALUE=DATE-TIME:20261004T180000Z" in serializeAlarm(alarm))
    }

    @Test
    fun parse_unreadableTriggerIsKeptVerbatim() {
        val alarm = parseAlarm(
            listOf(
                "ACTION:DISPLAY",
                "TRIGGER;VALUE=DATE-TIME:20261004T180000",     // not UTC - not allowed by RFC 5545
                "DESCRIPTION:Reminder"
            )
        )

        assertNull(alarm.trigger)
        assertFalse(alarm.isEditable())
        assertEquals(
            listOf("BEGIN:VALARM", "ACTION:DISPLAY", "DESCRIPTION:Reminder", "TRIGGER;VALUE=DATE-TIME:20261004T180000", "END:VALARM"),
            serializeAlarm(alarm)
        )
    }

    @Test
    fun parse_nestedComponentInsideAlarmIsKept() {
        // RFC 9074 proximity alarm with a nested VLOCATION carrying its own UID.
        val alarm = parseAlarm(
            listOf(
                "UID:alarm-uid",
                "ACTION:DISPLAY",
                "TRIGGER:PT0S",
                "DESCRIPTION:Arrived",
                "PROXIMITY:ARRIVE",
                "BEGIN:VLOCATION",
                "UID:location-uid",
                "URL:geo:40.443,-79.945;u=10",
                "END:VLOCATION"
            )
        )

        assertEquals("alarm-uid", alarm.uid)
        assertEquals(
            listOf("PROXIMITY:ARRIVE", "BEGIN:VLOCATION", "UID:location-uid", "URL:geo:40.443,-79.945;u=10", "END:VLOCATION"),
            alarm.extraProperties.map { it.unfoldedLine }
        )
    }

    @Test
    fun parse_otherNestedComponentsOfTheEntryAreKeptVerbatim() {
        val entry = parseIcalEntries(
            ics(
                "BEGIN:VTODO",
                "UID:task-2",
                "SUMMARY:Task",
                "BEGIN:X-CUSTOM",
                "DESCRIPTION:not the task's",
                "END:X-CUSTOM",
                "END:VTODO"
            )
        ).single()

        assertEquals("Task", entry.summary)
        assertNull(entry.description)
        assertEquals(
            listOf("BEGIN:X-CUSTOM", "DESCRIPTION:not the task's", "END:X-CUSTOM"),
            entry.extraProperties.map { it.unfoldedLine }
        )
    }

    @Test
    fun parse_entryWithoutAlarms() {
        val entry = parseIcalEntries(ics("BEGIN:VTODO", "UID:task-3", "SUMMARY:Task", "END:VTODO")).single()
        assertTrue(entry.alarms.isEmpty())
        assertFalse(serializeVCalendar(entry).contains("VALARM"))
    }
}
