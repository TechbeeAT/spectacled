package at.techbee.spectacled.screens.core.data.ics

import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
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

    // --- Regressions for the GMT-8 timeshift: "now" / "today" must use the device timezone ---

    private val losAngeles = TimeZone.of("America/Los_Angeles")

    // Nov 12, 17:00 in Los Angeles (UTC-8) is already Nov 13, 01:00 UTC.
    private val evenInLosAngeles = Instant.parse("2026-11-13T01:00:00Z")

    @Test
    fun nowFloating_keepsTheLocalWallClockOfTheDevice() {
        val now = IcsDateTime.nowFloating(losAngeles, evenInLosAngeles)

        assertFalse(now.isDateOnly)
        assertNull(now.timeZone)
        assertEquals(LocalDateTime(2026, 11, 12, 17, 0), now.toLocalDateTime())
        assertEquals("20261112T170000", formatIcsDateTime(now)!!.first)
    }

    @Test
    fun today_usesTheDateOfTheDevice() {
        val today = IcsDateTime.today(losAngeles, evenInLosAngeles)

        assertTrue(today.isDateOnly)
        assertEquals(LocalDate(2026, 11, 12), today.toLocalDateTime().date)
        assertEquals(LocalDateTime(2026, 11, 12, 0, 0), IcsDateTime.todayAtStartOfDay(losAngeles, evenInLosAngeles).toLocalDateTime())
    }

    @Test
    fun nowUtc_isSerializedAsUtc() {
        val (value, param) = formatIcsDateTime(IcsDateTime.nowUtc())!!
        assertTrue(value.endsWith("Z"))
        assertNull(param)
    }

    @Test
    fun nowInZone_isTheCurrentMomentInThatZone() {
        val now = IcsDateTime.nowInZone(losAngeles)
        assertEquals(losAngeles, now.timeZone)
        assertEquals("TZID=America/Los_Angeles", formatIcsDateTime(now)!!.second)
    }

    @Test
    fun toDatePickerMillis_floatingKeepsItsWallClockDay() {
        // Floating 03:00 on Nov 12 must stay Nov 12, it is not read in the device timezone.
        val floating = IcsDateTime(LocalDateTime(2026, 11, 12, 3, 0).toInstant(TimeZone.UTC), isDateOnly = false)
        assertEquals(LocalDate(2026, 11, 12).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(), floating.toDatePickerMillis())
    }

    @Test
    fun toDatePickerMillis_zonedUsesItsOwnZone() {
        val zoned = IcsDateTime(evenInLosAngeles, isDateOnly = false, timeZone = losAngeles)
        assertEquals(LocalDate(2026, 11, 12).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(), zoned.toDatePickerMillis())
    }

    @Test
    fun updateWithDatePickerMillis_keepsTimeAndZone() {
        val selected = LocalDate(2026, 11, 20).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

        val zoned = IcsDateTime(evenInLosAngeles, isDateOnly = false, timeZone = losAngeles).updateWithDatePickerMillis(selected)
        assertEquals(losAngeles, zoned.timeZone)
        assertEquals(LocalDateTime(2026, 11, 20, 17, 0), zoned.toLocalDateTime())

        val floating = IcsDateTime.nowFloating(losAngeles, evenInLosAngeles).updateWithDatePickerMillis(selected)
        assertNull(floating.timeZone)
        assertEquals(LocalDateTime(2026, 11, 20, 17, 0), floating.toLocalDateTime())

        val dateOnly = IcsDateTime.today(losAngeles, evenInLosAngeles).updateWithDatePickerMillis(selected)
        assertTrue(dateOnly.isDateOnly)
        assertEquals(LocalDate(2026, 11, 20), dateOnly.toLocalDateTime().date)
    }

    @Test
    fun datePickerRoundTrip_isStable() {
        for (value in listOf(
            IcsDateTime.nowFloating(losAngeles, evenInLosAngeles),
            IcsDateTime(evenInLosAngeles, isDateOnly = false, timeZone = losAngeles),
            IcsDateTime.today(losAngeles, evenInLosAngeles),
        )) {
            assertEquals(value, value.updateWithDatePickerMillis(value.toDatePickerMillis()))
        }
    }

    @Test
    fun nowInZone_keepsANamedZone() {
        val now = IcsDateTime.nowInZone(losAngeles, evenInLosAngeles)
        assertEquals(evenInLosAngeles, now.instant)
        assertEquals(losAngeles, now.timeZone)
    }

    @Test
    fun nowInZone_keepsUtc() {
        val now = IcsDateTime.nowInZone(TimeZone.UTC, evenInLosAngeles)
        assertEquals(evenInLosAngeles, now.instant)
        assertEquals(TimeZone.UTC, now.timeZone)
    }

    @Test
    fun nowInZone_fixedOffsetFallsBackToFloatingWallClock() {
        // A device configured with a raw offset ("GMT-08:00") has no TZID other clients can resolve.
        val fixedOffset = FixedOffsetTimeZone(UtcOffset(hours = -8))
        val now = IcsDateTime.nowInZone(fixedOffset, evenInLosAngeles)

        assertNull(now.timeZone)
        assertFalse(now.isDateOnly)
        assertEquals(LocalDateTime(2026, 11, 12, 17, 0), now.toLocalDateTime())
        assertNull(formatIcsDateTime(now)!!.second)
    }

    @Test
    fun isUsableAsTzid_onlyForNamedZonesAndUtc() {
        assertTrue(losAngeles.isUsableAsTzid())
        assertTrue(vienna.isUsableAsTzid())
        assertTrue(TimeZone.UTC.isUsableAsTzid())
        assertFalse(FixedOffsetTimeZone(UtcOffset(hours = -8)).isUsableAsTzid())
        assertFalse(TimeZone.of("GMT-08:00").isUsableAsTzid())
    }

    @Test
    fun inDeviceZone_showsAUtcTimestampInTheDeviceZone() {
        val utc = IcsDateTime(evenInLosAngeles, isDateOnly = false, timeZone = TimeZone.UTC)
        val local = utc.inDeviceZone(losAngeles)

        assertEquals(evenInLosAngeles, local.instant)
        assertEquals(losAngeles, local.timeZone)
        assertEquals(LocalDateTime(2026, 11, 12, 17, 0), local.toLocalDateTime())
    }

    @Test
    fun inDeviceZone_takesAFloatingTimestampAsUtc() {
        val floating = IcsDateTime(evenInLosAngeles, isDateOnly = false, timeZone = null)
        assertEquals(LocalDateTime(2026, 11, 12, 17, 0), floating.inDeviceZone(losAngeles).toLocalDateTime())
    }
}
