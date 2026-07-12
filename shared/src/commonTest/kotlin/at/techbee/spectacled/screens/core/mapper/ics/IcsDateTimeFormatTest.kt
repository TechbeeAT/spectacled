package at.techbee.spectacled.screens.core.mapper.ics

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class IcsDateTimeFormatTest {

    private val utcInstant = Instant.parse("2026-07-11T10:15:30Z")
    private val vienna = TimeZone.of("Europe/Vienna")

    // --- formatIcsDateTime ---

    @Test
    fun format_dateOnly() {
        val dt = IcsDateTime(LocalDate(2026, 7, 11).atStartOfDayIn(TimeZone.UTC), isDateOnly = true, timeZone = null)
        assertEquals("20260711" to "VALUE=DATE", formatIcsDateTime(dt))
    }

    @Test
    fun format_utcDateTimeGetsZSuffix() {
        val dt = IcsDateTime(utcInstant, isDateOnly = false, timeZone = TimeZone.UTC)
        assertEquals("20260711T101530Z" to null, formatIcsDateTime(dt))
    }

    @Test
    fun format_zonedDateTimeGetsTzidParamAndLocalWallTime() {
        // 10:15:30 UTC is 12:15:30 in Vienna during summer time (UTC+2)
        val dt = IcsDateTime(utcInstant, isDateOnly = false, timeZone = vienna)
        assertEquals("20260711T121530" to "TZID=Europe/Vienna", formatIcsDateTime(dt))
    }

    @Test
    fun format_floatingDateTimeHasNoSuffixAndNoParam() {
        val dt = IcsDateTime(utcInstant, isDateOnly = false, timeZone = null)
        assertEquals("20260711T101530" to null, formatIcsDateTime(dt))
    }

    @Test
    fun format_nullReturnsNull() {
        assertNull(formatIcsDateTime(null))
    }

    // --- parseIcsDateTime ---

    @Test
    fun parse_dateOnly() {
        val parsed = parseIcsDateTime("20260711")
        assertNotNull(parsed)
        assertEquals(LocalDate(2026, 7, 11).atStartOfDayIn(TimeZone.UTC), parsed.instant)
        assertEquals(true, parsed.isDateOnly)
        assertNull(parsed.timeZone)
    }

    @Test
    fun parse_utcDateTime() {
        val parsed = parseIcsDateTime("20260711T101530Z")
        assertNotNull(parsed)
        assertEquals(utcInstant, parsed.instant)
        assertEquals(false, parsed.isDateOnly)
        assertEquals(TimeZone.UTC, parsed.timeZone)
    }

    @Test
    fun parse_zonedDateTimeAppliesTzid() {
        val parsed = parseIcsDateTime("20260711T121530", tzid = "Europe/Vienna")
        assertNotNull(parsed)
        assertEquals(utcInstant, parsed.instant)
        assertEquals(vienna, parsed.timeZone)
    }

    @Test
    fun parse_floatingDateTimeIsTreatedAsUtc() {
        val parsed = parseIcsDateTime("20260711T101530")
        assertNotNull(parsed)
        assertEquals(utcInstant, parsed.instant)
        assertNull(parsed.timeZone)
    }

    @Test
    fun parse_unknownTzidFallsBackToUtc() {
        val parsed = parseIcsDateTime("20260711T101530", tzid = "Not/AZone")
        assertNotNull(parsed)
        assertEquals(utcInstant, parsed.instant)
        assertEquals(TimeZone.UTC, parsed.timeZone)
    }

    @Test
    fun parse_malformedValuesDegradeToNullInsteadOfThrowing() {
        assertNull(parseIcsDateTime(null))
        assertNull(parseIcsDateTime("garbage!"))          // 8 chars, but not a date
        assertNull(parseIcsDateTime("20261399"))          // month 13, day 99
        assertNull(parseIcsDateTime("20260711T256000Z"))  // hour 25
        assertNull(parseIcsDateTime("20260711T1015"))     // too short for any branch
    }

    // --- round trips through both functions ---

    @Test
    fun formatThenParse_roundTripsAllFourShapes() {
        val shapes = listOf(
            IcsDateTime(LocalDate(2026, 7, 11).atStartOfDayIn(TimeZone.UTC), isDateOnly = true, timeZone = null),
            IcsDateTime(utcInstant, isDateOnly = false, timeZone = TimeZone.UTC),
            IcsDateTime(utcInstant, isDateOnly = false, timeZone = vienna),
            IcsDateTime(utcInstant, isDateOnly = false, timeZone = null)
        )
        shapes.forEach { original ->
            val (value, param) = assertNotNull(formatIcsDateTime(original))
            val tzid = param?.takeIf { it.startsWith("TZID=") }?.removePrefix("TZID=")
            assertEquals(original, parseIcsDateTime(value, tzid), "Round trip failed for $original")
        }
    }
}
