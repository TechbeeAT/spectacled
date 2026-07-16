package at.techbee.spectacled.screens.core.data.ics

import kotlin.js.JsModule
import kotlin.js.JsNonModule

/**
 * The `@js-joda/timezone` npm module. Referencing it forces the bundler to emit the import, whose
 * registration side effect installs the IANA time-zone database into the js-joda backend
 * kotlinx-datetime uses on Kotlin/JS. Without it, TimeZone.of("Europe/Vienna") throws
 * IllegalTimeZoneException.
 */
@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTimeZoneModule

actual fun ensureTimeZoneDatabaseLoaded() {
    // The reference is what keeps the import (and therefore the registration side effect) alive
    // through tree-shaking; the actual load happens once, when this module is first evaluated.
    checkNotNull(JsJodaTimeZoneModule)
}
