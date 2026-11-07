package at.techbee.spectacled.screens.core

/**
 * The Android apps initialize [AppLogging] in their Application.onCreate from the app's debuggable
 * flag, before this could be used. Should anything get here first, it logs like a release build.
 */
actual fun platformIsDebugBuild(): Boolean = false
