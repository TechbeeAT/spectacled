package at.techbee.spectacled.screens.core.data.ics

/**
 * Ensures that named IANA time zones (e.g. "Europe/Vienna") can be resolved via
 * [kotlinx.datetime.TimeZone.of] on the current platform.
 *
 * It is a no-op on JVM, Android and iOS, where the platform ships a time-zone database. On the
 * web targets (js/wasmJs) kotlinx-datetime resolves zones through js-joda, which only has the full
 * database once the `@js-joda/timezone` module is imported; the web actuals reference that module
 * so the bundler emits the import, whose registration side effect runs at module load.
 *
 * Calling this from a code path the app always reaches (see [TimeZoneSerializer.deserialize]) is
 * enough to pull that import into the app bundle - the registration happens at load, before any
 * zone is resolved, so callers do not need to invoke this before every lookup.
 */
expect fun ensureTimeZoneDatabaseLoaded()
