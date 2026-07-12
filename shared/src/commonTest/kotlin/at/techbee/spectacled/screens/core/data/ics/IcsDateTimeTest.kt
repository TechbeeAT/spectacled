package at.techbee.spectacled.screens.core.data.ics

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

class IcsDateTimeTest {

    private val vienna = TimeZone.of("Europe/Vienna")

    @Test
    fun effectiveZone_dateOnlyAlwaysWinsAsUtc() {
        val dt = IcsDateTime(Instant.parse("2026-07-11T22:30:00Z"), isDateOnly = true, timeZone = vienna)
        assertEquals(TimeZone.UTC, dt.effectiveZone())
    }

    @Test
    fun effectiveZone_usesExplicitZoneForDateTimes() {
        val dt = IcsDateTime(Instant.parse("2026-07-11T22:30:00Z"), isDateOnly = false, timeZone = vienna)
        assertEquals(vienna, dt.effectiveZone())
    }

    @Test
    fun effectiveZone_floatingDefaultsToUtc() {
        val dt = IcsDateTime(Instant.parse("2026-07-11T22:30:00Z"), isDateOnly = false, timeZone = null)
        assertEquals(TimeZone.UTC, dt.effectiveZone())
    }

    @Test
    fun asDateOnly_usesTheLocalDateOfTheEffectiveZone() {
        // 22:30 UTC on Jul 11 is already Jul 12, 00:30 in Vienna (UTC+2 in summer)
        val zoned = IcsDateTime(Instant.parse("2026-07-11T22:30:00Z"), isDateOnly = false, timeZone = vienna)
        val dateOnly = zoned.asDateOnly()

        assertTrue(dateOnly.isDateOnly)
        assertNull(dateOnly.timeZone)
        assertEquals(LocalDate(2026, 7, 12).atStartOfDayIn(TimeZone.UTC), dateOnly.instant)
    }

    @Test
    fun asDateTime_usesTheLocalDateOfTheEffectiveZone() {
        val zoned = IcsDateTime(Instant.parse("2026-07-12T00:00:00Z"), isDateOnly = true)
        val dateTime = zoned.asDateTime()

        assertFalse(dateTime.isDateOnly)
        assertNull(dateTime.timeZone)
        assertEquals(LocalDateTime(2026, 7, 12, 0, 0).toInstant(TimeZone.UTC), dateTime.instant)
    }

    @Test
    fun withZone_preservesTheLocalWallTime() {
        // 10:00 UTC re-anchored to Vienna keeps the 10:00 wall time, so the instant
        // moves back by Vienna's summer offset (UTC+2) to 08:00 UTC.
        val utc = IcsDateTime(Instant.parse("2026-07-11T10:00:00Z"), isDateOnly = false, timeZone = TimeZone.UTC)
        val rezoned = utc.withZone(vienna)

        assertEquals(Instant.parse("2026-07-11T08:00:00Z"), rezoned.instant)
        assertEquals(vienna, rezoned.timeZone)
    }
}
