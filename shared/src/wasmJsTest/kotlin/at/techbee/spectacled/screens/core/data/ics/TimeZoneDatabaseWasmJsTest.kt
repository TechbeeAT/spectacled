package at.techbee.spectacled.screens.core.data.ics

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pulls the @js-joda/timezone import into the Kotlin/wasmJs test bundle - its registration side
 * effect runs at bundle load, before any timezone test class is constructed - and asserts named
 * zones resolve. In production SecureStorageReadyGate triggers the same load; the test needs its
 * own because :shared:allTests does not exercise the web app startup.
 */
class TimeZoneDatabaseWasmJsTest {
    @Test
    fun namedTimeZonesResolveOnWasmJs() {
        loadTimeZoneDatabase()
        assertEquals("Europe/Vienna", TimeZone.of("Europe/Vienna").id)
    }
}
