package at.techbee.spectacled.screens.core.data.ics

import kotlin.js.JsModule

/**
 * The `@js-joda/timezone` npm module. Referencing it forces the bundler to emit the import, whose
 * registration side effect installs the IANA time-zone database into the js-joda backend
 * kotlinx-datetime uses on Kotlin/wasmJs. Without it, TimeZone.of("Europe/Vienna") throws
 * IllegalTimeZoneException. (The wasmJs declaration omits @JsNonModule, unlike the js one.)
 */
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule

actual fun ensureTimeZoneDatabaseLoaded() {
    // The reference keeps the import (and its registration side effect, which runs at module load)
    // alive through tree-shaking. On wasmJs the module is side-effect-only with no default export,
    // so materializing it as a value throws - harmless, and unrelated to the registration that has
    // already happened, so we swallow it.
    runCatching { checkNotNull(JsJodaTimeZoneModule) }
}
