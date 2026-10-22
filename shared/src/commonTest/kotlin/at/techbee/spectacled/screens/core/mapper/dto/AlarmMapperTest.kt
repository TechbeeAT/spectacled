package at.techbee.spectacled.screens.core.mapper.dto

import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.domain.AlarmTrigger
import at.techbee.spectacled.screens.core.domain.AlarmTriggerRelation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AlarmMapperTest {

    @Test
    fun alarmSurvivesDtoRoundTrip() {
        val alarm = Alarm(
            uid = "alarm-uid",
            trigger = AlarmTrigger.Relative(IcsDuration(isNegative = true, minutes = 15), AlarmTriggerRelation.END),
            description = "Multi\nline, with; special chars",
            extraProperties = listOf(RawIcsProperty("X-APPLE-DEFAULT-ALARM", "X-APPLE-DEFAULT-ALARM:TRUE"))
        )
        val triggerAt = Instant.parse("2026-10-05T08:45:00Z")

        val dto = alarm.toDto(icalEntryId = 7L, triggerAt = triggerAt, lastNotifiedAt = 123L)

        assertEquals(7L, dto.icalEntryId)
        assertEquals("alarm-uid", dto.uid)
        assertEquals(Alarm.ACTION_DISPLAY, dto.action)
        assertEquals(triggerAt.toEpochMilliseconds(), dto.triggerAt)
        assertEquals(123L, dto.lastNotifiedAt)
        assertEquals(alarm, dto.toDomain())
    }

    @Test
    fun alarmWithoutTriggerTime() {
        val dto = Alarm(uid = null, action = null).toDto(icalEntryId = 1L, triggerAt = null)

        assertNull(dto.uid)
        assertNull(dto.triggerAt)
        assertEquals(Alarm(uid = null, action = null), dto.toDomain())
    }
}
