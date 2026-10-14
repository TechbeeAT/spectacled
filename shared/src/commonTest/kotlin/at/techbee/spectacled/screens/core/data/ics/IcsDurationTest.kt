package at.techbee.spectacled.screens.core.data.ics

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class IcsDurationTest {

    @Test
    fun parse_validValues() {
        assertEquals(IcsDuration(isNegative = true, minutes = 15), IcsDuration.parse("-PT15M"))
        assertEquals(IcsDuration(weeks = 1), IcsDuration.parse("P1W"))
        assertEquals(IcsDuration(days = 1), IcsDuration.parse("+P1D"))
        assertEquals(IcsDuration(isNegative = true, days = 1, hours = 2), IcsDuration.parse("-P1DT2H"))
        assertEquals(IcsDuration(days = 15, hours = 5, seconds = 20), IcsDuration.parse("P15DT5H0M20S"))
        assertEquals(IcsDuration(), IcsDuration.parse("PT0S"))
        assertEquals(IcsDuration(isNegative = true, minutes = 5), IcsDuration.parse("-pt5m"))
    }

    @Test
    fun parse_invalidValues() {
        listOf("", "P", "PT", "P1DT", "-PT", "15M", "PT15", "P1W2D", "PT-15M", "P1.5D", "garbage")
            .forEach { assertNull(IcsDuration.parse(it), "'$it' should not parse") }
    }

    @Test
    fun format_roundTripsCanonicalValues() {
        listOf("-PT15M", "P1W", "P1D", "-P1DT2H", "PT0S", "PT1H0M30S", "-P2DT1H30M", "PT45S")
            .forEach { assertEquals(it, IcsDuration.parse(it)?.format()) }
    }

    @Test
    fun format_weeksMixedWithOtherComponentsAreWrittenAsDays() {
        assertEquals("P9DT1H", IcsDuration(weeks = 1, days = 2, hours = 1).format())
    }

    @Test
    fun addTo_exactAndNominalComponents() {
        val viennaZone = TimeZone.of("Europe/Vienna")
        // 2026-03-29 is the DST switch in Vienna (02:00 -> 03:00), that day has 23 hours.
        val afterSwitch = Instant.parse("2026-03-29T07:00:00Z")      // 09:00 CEST

        // A nominal day keeps the wall clock time: 09:00 CET the day before.
        assertEquals(Instant.parse("2026-03-28T08:00:00Z"), IcsDuration.parse("-P1D")!!.addTo(afterSwitch, viennaZone))
        // 24 exact hours don't.
        assertEquals(Instant.parse("2026-03-28T07:00:00Z"), IcsDuration.parse("-PT24H")!!.addTo(afterSwitch, viennaZone))

        assertEquals(Instant.parse("2026-03-29T06:45:00Z"), IcsDuration.parse("-PT15M")!!.addTo(afterSwitch, viennaZone))
        assertEquals(Instant.parse("2026-04-05T07:00:00Z"), IcsDuration.parse("P1W")!!.addTo(afterSwitch, viennaZone))
    }
}
