package at.techbee.spectacled.screens.core.domain

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class AlarmTest {

    private val vienna = TimeZone.of("Europe/Vienna")
    private val tokyo = TimeZone.of("Asia/Tokyo")

    private val fifteenMinutesBefore = IcsDuration(isNegative = true, minutes = 15)
    private val oneDayBefore = IcsDuration(isNegative = true, days = 1)

    private val dtStart = IcsDateTime(Instant.parse("2026-10-01T08:00:00Z"), isDateOnly = false, timeZone = TimeZone.UTC)
    private val due = IcsDateTime(Instant.parse("2026-10-05T09:00:00Z"), isDateOnly = false, timeZone = TimeZone.UTC)
    private val task = IcalEntry.newTask().copy(dtStart = dtStart, due = due)

    private fun relative(duration: IcsDuration, relatedTo: AlarmTriggerRelation) =
        Alarm(trigger = AlarmTrigger.Relative(duration, relatedTo), description = "x")

    @Test
    fun triggerTime_relativeToStartAndEnd() {
        assertEquals(Instant.parse("2026-10-01T07:45:00Z"), relative(fifteenMinutesBefore, AlarmTriggerRelation.START).triggerTime(task, vienna))
        assertEquals(Instant.parse("2026-10-05T08:45:00Z"), relative(fifteenMinutesBefore, AlarmTriggerRelation.END).triggerTime(task, vienna))
    }

    @Test
    fun triggerTime_absolute() {
        val at = Instant.parse("2026-10-03T12:00:00Z")
        assertEquals(at, Alarm(trigger = AlarmTrigger.Absolute(at)).triggerTime(IcalEntry.newTask(), vienna))
    }

    @Test
    fun triggerTime_missingReferenceDateIsNull() {
        assertNull(relative(fifteenMinutesBefore, AlarmTriggerRelation.END).triggerTime(task.copy(due = null), vienna))
        assertNull(relative(fifteenMinutesBefore, AlarmTriggerRelation.START).triggerTime(task.copy(dtStart = null), vienna))
        assertNull(Alarm(trigger = null).triggerTime(task, vienna))
    }

    @Test
    fun triggerTime_endOfJournalIsUndefined() {
        val journal = IcalEntry.newJournal().copy(dtStart = dtStart, due = due)
        assertNull(relative(fifteenMinutesBefore, AlarmTriggerRelation.END).triggerTime(journal, vienna))
    }

    @Test
    fun triggerTime_allDayDueIsResolvedInTheDeviceTimeZone() {
        val allDayDue = IcsDateTime(LocalDate(2026, 10, 5).atStartOfDayIn(TimeZone.UTC), isDateOnly = true)
        val alarm = relative(oneDayBefore, AlarmTriggerRelation.END)

        // Local midnight of the 4th, wherever the device is.
        assertEquals(LocalDate(2026, 10, 4).atStartOfDayIn(vienna), alarm.triggerTime(task.copy(due = allDayDue), vienna))
        assertEquals(LocalDate(2026, 10, 4).atStartOfDayIn(tokyo), alarm.triggerTime(task.copy(due = allDayDue), tokyo))
    }

    @Test
    fun triggerTime_floatingDueIsResolvedInTheDeviceTimeZone() {
        // Floating times are stored as UTC wall clock (see parseIcsDateTime).
        val floatingDue = IcsDateTime(LocalDateTime(2026, 10, 5, 9, 0).toInstant(TimeZone.UTC), isDateOnly = false, timeZone = null)
        val alarm = relative(fifteenMinutesBefore, AlarmTriggerRelation.END)

        assertEquals(LocalDateTime(2026, 10, 5, 8, 45).toInstant(vienna), alarm.triggerTime(task.copy(due = floatingDue), vienna))
    }

    @Test
    fun triggerTime_nominalDayIsAddedInTheZoneOfTheReferenceDate() {
        // Due Sunday 2026-10-25 09:00 Vienna, a few hours after DST ended that night (03:00 -> 02:00).
        val zonedDue = IcsDateTime(LocalDateTime(2026, 10, 25, 9, 0).toInstant(vienna), isDateOnly = false, timeZone = vienna)
        val alarm = relative(oneDayBefore, AlarmTriggerRelation.END)

        // Still 09:00 local time on Saturday, even though that's 25 hours earlier - regardless of
        // the device being in another zone.
        assertEquals(LocalDateTime(2026, 10, 24, 9, 0).toInstant(vienna), alarm.triggerTime(task.copy(due = zonedDue), tokyo))
    }

    @Test
    fun isEditable() {
        assertTrue(relative(fifteenMinutesBefore, AlarmTriggerRelation.END).isEditable())
        assertFalse(Alarm(trigger = null).isEditable())
        assertFalse(relative(fifteenMinutesBefore, AlarmTriggerRelation.END).copy(action = Alarm.ACTION_EMAIL).isEditable())
        assertFalse(
            relative(fifteenMinutesBefore, AlarmTriggerRelation.END)
                .copy(extraProperties = listOf(RawIcsProperty("REPEAT", "REPEAT:2")))
                .isEditable()
        )
    }

    @Test
    fun isNotifying() {
        assertTrue(Alarm(action = Alarm.ACTION_DISPLAY).isNotifying())
        assertTrue(Alarm(action = Alarm.ACTION_AUDIO).isNotifying())
        assertFalse(Alarm(action = Alarm.ACTION_EMAIL).isNotifying())
        assertFalse(Alarm(action = null).isNotifying())
    }
}
