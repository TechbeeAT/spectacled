package at.techbee.spectacled.screens.core.data.ics

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards that named IANA zones resolve on every target. On js/wasmJs the call to
 * [ensureTimeZoneDatabaseLoaded] is what pulls the @js-joda/timezone import into the test bundle
 * (its registration side effect runs at bundle load, before any other timezone test class is
 * constructed); on JVM/Android/iOS it is a no-op and the platform database is used. Without the
 * web setup this assertion - and the rest of the ICS date-time suite - fails with
 * IllegalTimeZoneException on the browser targets.
 */
class TimeZoneDatabaseTest {
    @Test
    fun namedTimeZonesResolve() {
        ensureTimeZoneDatabaseLoaded()
        assertEquals("Europe/Vienna", TimeZone.of("Europe/Vienna").id)
    }
}
