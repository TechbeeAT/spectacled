package at.techbee.spectacled.screens.core

/**
 * The jpackage launcher of the packaged desktop app (.dmg/.msi/.deb) sets `jpackage.app-path`;
 * `./gradlew run` and hot reload don't, so those count as debug builds.
 */
actual fun platformIsDebugBuild(): Boolean = System.getProperty("jpackage.app-path") == null
