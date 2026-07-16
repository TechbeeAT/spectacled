package at.techbee.spectacled.screens.core.data.ics

import kotlin.js.JsModule

/**
 * The `@js-joda/timezone` npm module (declared in shared/build.gradle.kts, webMain). Referencing it
 * makes the bundler emit its import, whose registration side effect installs the IANA time-zone
 * database into the js-joda backend kotlinx-datetime uses on the web targets. Without it,
 * TimeZone.of("Europe/Vienna") throws IllegalTimeZoneException on js/wasmJs. JVM/Android/iOS get
 * their zones from the platform, so this whole file - and any call to [loadTimeZoneDatabase] - is
 * web-only; there are deliberately no no-op counterparts on the other targets.
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule

/**
 * Loads the IANA time-zone database so named zones (TZID=...) resolve on the web targets. Call once
 * at web startup (see SecureStorageReadyGate). The import runs at module load, so this only needs to
 * be reachable, not invoked before every lookup; calling it more than once is harmless.
 */
fun loadTimeZoneDatabase() {
    // The reference keeps the import (and its registration side effect) alive through tree-shaking.
    // On wasmJs, materializing the side-effect-only module as a value throws - harmless, and after
    // the registration has already run at import - so we swallow it.
    runCatching { checkNotNull(JsJodaTimeZoneModule) }
}
