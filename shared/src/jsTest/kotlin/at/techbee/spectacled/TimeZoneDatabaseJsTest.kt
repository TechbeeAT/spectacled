package at.techbee.spectacled

import kotlinx.datetime.TimeZone
import kotlin.js.JsModule
import kotlin.js.JsNonModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Loads the IANA time-zone database into the Kotlin/JS browser test bundle.
 *
 * kotlinx-datetime resolves named zones on JS through js-joda, which only has the full database
 * when the `@js-joda/timezone` npm module is actually imported. Declaring the npm dependency
 * (see shared/build.gradle.kts, webMain) is not enough on its own - the module registers the
 * database as an import side effect, which only runs if something references it. The external
 * declaration below, referenced from the test, is what forces webpack to emit that import. The
 * require is hoisted to module load, so the database is registered before any commonTest timezone
 * test class is constructed. Without it, TimeZone.of("Europe/Vienna") throws
 * IllegalTimeZoneException on the js target.
 */
@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTimeZoneModule

class TimeZoneDatabaseJsTest {
    @Test
    fun namedTimeZonesResolveOnJs() {
        // Referencing the module keeps the @js-joda/timezone import (and its registration side
        // effect) alive through tree-shaking.
        assertNotNull(JsJodaTimeZoneModule)
        assertEquals("Europe/Vienna", TimeZone.of("Europe/Vienna").id)
    }
}
