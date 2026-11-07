package at.techbee.spectacled.screens.core

import kotlin.experimental.ExperimentalNativeApi

/** True when the shared framework was built in Xcode's Debug configuration. */
@OptIn(ExperimentalNativeApi::class)
actual fun platformIsDebugBuild(): Boolean = kotlin.native.Platform.isDebugBinary  // fully qualified, this package has its own Platform
