package at.techbee.spectacled.screens.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class PriorityTest {

    @Test
    fun fromIcsValue_followsRfc5545Grouping() {
        val expected = mapOf(
            1L to Priority.HIGH,
            2L to Priority.HIGH,
            3L to Priority.HIGH,
            4L to Priority.HIGH,
            5L to Priority.MEDIUM,
            6L to Priority.LOW,
            7L to Priority.LOW,
            8L to Priority.LOW,
            9L to Priority.LOW
        )
        expected.forEach { (icsValue, priority) ->
            assertEquals(priority, Priority.fromIcsValue(icsValue), "fromIcsValue($icsValue)")
        }
    }

    @Test
    fun fromIcsValue_undefinedAndOutOfRange_isNotSpecified() {
        listOf(null, 0L, -1L, 10L, Long.MAX_VALUE).forEach { icsValue ->
            assertEquals(Priority.NOT_SPECIFIED, Priority.fromIcsValue(icsValue), "fromIcsValue($icsValue)")
        }
    }

    @Test
    fun icsValue_roundTripsToSameLevel() {
        Priority.entries.forEach { priority ->
            assertEquals(priority, Priority.fromIcsValue(priority.icsValue), "round trip for $priority")
        }
    }

    @Test
    fun icsValue_perLevel() {
        assertEquals(null, Priority.NOT_SPECIFIED.icsValue)
        assertEquals(9L, Priority.LOW.icsValue)
        assertEquals(5L, Priority.MEDIUM.icsValue)
        assertEquals(1L, Priority.HIGH.icsValue)
    }

    @Test
    fun sliderSteps_orderedFromLowestToHighest() {
        assertEquals(
            listOf(Priority.NOT_SPECIFIED, Priority.LOW, Priority.MEDIUM, Priority.HIGH),
            (0..3).map { Priority.fromSliderStep(it) }
        )
    }

    @Test
    fun fromSliderStep_outOfRange_isNotSpecified() {
        listOf(-1, 4, Int.MAX_VALUE).forEach { step ->
            assertEquals(Priority.NOT_SPECIFIED, Priority.fromSliderStep(step), "fromSliderStep($step)")
        }
    }
}
