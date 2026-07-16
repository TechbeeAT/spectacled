package at.techbee.spectacled.screens.core.data.ics

// Android resolves IANA time zones through the platform (ICU / core-library desugaring),
// so no extra setup is needed.
actual fun ensureTimeZoneDatabaseLoaded() {}
