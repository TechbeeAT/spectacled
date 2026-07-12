package at.techbee.spectacled.screens.core.domain

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class CalendarSyncStatusTest {

    @Test
    fun serializeDeserializeRoundTrip() {
        val status = CalendarSyncStatus(
            type = CalendarSyncStatusType.FAILED,
            message = "Sync failed",
            details = "HTTP 500 Internal Server Error",
            icsDateTime = IcsDateTime(Instant.parse("2026-07-11T10:15:30Z"), isDateOnly = false, timeZone = null)
        )
        assertEquals(status, CalendarSyncStatus.deserialize(status.serialize()))
    }

    @Test
    fun deserializeToleratesUnknownKeysAndMissingOptionalFields() {
        // Forward compatibility: an old app version must be able to read a status JSON
        // written by a newer one (unknown keys), and defaults must fill missing fields.
        val parsed = CalendarSyncStatus.deserialize("""{"type":"SYNCED","someFutureField":123}""")
        assertEquals(CalendarSyncStatusType.SYNCED, parsed.type)
        assertNull(parsed.message)
        assertNull(parsed.details)
    }

    @Test
    fun errorTypes_exhaustive() {
        val expectedErrors = setOf(
            CalendarSyncStatusType.FAILED,
            CalendarSyncStatusType.NOT_AUTHORIZED,
            CalendarSyncStatusType.NOT_FOUND
        )
        CalendarSyncStatusType.entries.forEach { type ->
            assertEquals(type in expectedErrors, type.isErrorType(), "isErrorType() for $type")
        }
    }
}
