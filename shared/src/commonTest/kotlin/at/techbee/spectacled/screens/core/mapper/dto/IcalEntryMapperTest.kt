package at.techbee.spectacled.screens.core.mapper.dto

import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.IcalEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IcalEntryMapperTest {

    @Test
    fun locationSurvivesDtoRoundTrip() {
        val entry = IcalEntry(uid = "loc-uid", location = "Stadtpark", calendarComponent = CalendarComponent.VTODO)

        assertEquals("Stadtpark", entry.toDto().toDomain().location)
    }

    @Test
    fun emptyLocationIsStoredAsNull() {
        val entry = IcalEntry(uid = "loc-uid", location = "", calendarComponent = CalendarComponent.VTODO)

        assertNull(entry.toDto().location)
    }
}
