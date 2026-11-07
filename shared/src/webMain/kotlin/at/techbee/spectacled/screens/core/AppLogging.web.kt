package at.techbee.spectacled.screens.core

/** The web build has no release signal yet; its logs only go to the browser console. */
actual fun platformIsDebugBuild(): Boolean = true
