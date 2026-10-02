package at.techbee.spectacled.screens.core.mapper.dto

import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
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

    @Test
    fun legacyLocationInExtraPropertiesIsLiftedIntoField() {
        // Rows written before LOCATION was a known property carry it as a raw extra property.
        val other = RawIcsProperty(name = "X-CUSTOM", unfoldedLine = "X-CUSTOM:keep me")
        val legacy = IcalEntry(
            uid = "legacy-uid",
            extraProperties = listOf(
                RawIcsProperty(name = "LOCATION", unfoldedLine = "LOCATION;LANGUAGE=de:Hauptstraße 1\\, Wien"),
                other
            ),
            calendarComponent = CalendarComponent.VJOURNAL
        )

        val mapped = legacy.toDto().toDomain()

        assertEquals("Hauptstraße 1, Wien", mapped.location)
        assertEquals(listOf(other), mapped.extraProperties)
    }

    @Test
    fun dedicatedLocationColumnWinsOverLegacyExtraProperty() {
        val entry = IcalEntry(
            uid = "both-uid",
            location = "New place",
            extraProperties = listOf(RawIcsProperty(name = "LOCATION", unfoldedLine = "LOCATION:Old place")),
            calendarComponent = CalendarComponent.VJOURNAL
        )

        assertEquals("New place", entry.toDto().toDomain().location)
    }
}
